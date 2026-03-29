package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.block.BlockAdvancedStorageCore;
import io.github.scuba10steve.s3.advanced.blockentity.AdvancedStorageCoreBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.AutoCrafterBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.MachineInterfaceBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.RecipeMemoryBoxBlockEntity;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import io.github.scuba10steve.s3.block.StorageMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.*;
import java.util.stream.Stream;

public class RecipeMemoryBoxMenu extends AbstractContainerMenu {

    public static final int PATTERN_SLOT_COUNT = RecipeMemoryBoxBlockEntity.MAX_PATTERNS;
    private static final int PATTERN_SLOTS_START = 0;

    private final BlockPos pos;
    private final RecipeMemoryBoxBlockEntity blockEntity;
    /** Positions of Auto-Crafters in the multiblock; populated on server, read on client. */
    private final List<BlockPos> crafterPositions;

    // Client constructor
    public RecipeMemoryBoxMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), null,
             new SimpleContainer(PATTERN_SLOT_COUNT), readCrafterPositions(buf));
    }

    // Server constructor
    public RecipeMemoryBoxMenu(int containerId, Inventory playerInventory, RecipeMemoryBoxBlockEntity be) {
        this(containerId, playerInventory, be.getBlockPos(), be,
             buildDisplayContainer(be), computeCrafterPositions(be));
    }

    private static List<BlockPos> computeCrafterPositions(RecipeMemoryBoxBlockEntity be) {
        Level level = be.getLevel();
        if (level == null) return List.of();
        AdvancedStorageCoreBlockEntity core = findCore(level, be.getBlockPos());
        if (core == null) return List.of();
        return Stream.concat(
            core.getAutoCrafters().stream().map(AutoCrafterBlockEntity::getBlockPos),
            core.getMachineInterfaces().stream().map(MachineInterfaceBlockEntity::getBlockPos)
        ).toList();
    }

    private RecipeMemoryBoxMenu(int containerId, Inventory playerInventory, BlockPos pos,
                                 RecipeMemoryBoxBlockEntity be, SimpleContainer displayContainer,
                                 List<BlockPos> crafterPositions) {
        super(ModMenuTypes.RECIPE_MEMORY_BOX.get(), containerId);
        this.pos = pos;
        this.blockEntity = be;
        this.crafterPositions = List.copyOf(crafterPositions);

        for (int i = 0; i < PATTERN_SLOT_COUNT; i++) {
            int row = i / 2;
            int col = i % 2;
            addSlot(new Slot(displayContainer, i, 26 + col * 18, 17 + row * 18) {
                @Override
                public boolean mayPickup(Player player) { return false; }
                @Override
                public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    /** Returns the list of Auto-Crafter positions in this multiblock (client-side). */
    public List<BlockPos> getCrafterPositions() {
        return crafterPositions;
    }

    public BlockPos getRmbPos() {
        return pos;
    }

    private static SimpleContainer buildDisplayContainer(RecipeMemoryBoxBlockEntity be) {
        SimpleContainer container = new SimpleContainer(PATTERN_SLOT_COUNT);
        for (int i = 0; i < PATTERN_SLOT_COUNT; i++) {
            container.setItem(i, be.getPattern(i).getOutput().copy());
        }
        return container;
    }

    private static List<BlockPos> readCrafterPositions(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<BlockPos> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(buf.readBlockPos());
        }
        return result;
    }

    /**
     * Writes the full extra data expected by the client constructor:
     * BlockPos + crafter count + crafter positions.
     * Call this whenever opening a RecipeMemoryBoxMenu from any server-side path.
     */
    public static void writeMenuExtraData(FriendlyByteBuf buf, RecipeMemoryBoxBlockEntity be, Level level) {
        buf.writeBlockPos(be.getBlockPos());
        AdvancedStorageCoreBlockEntity core = findCore(level, be.getBlockPos());
        List<BlockPos> crafterPositions = core != null
            ? Stream.concat(
                core.getAutoCrafters().stream().map(AutoCrafterBlockEntity::getBlockPos),
                core.getMachineInterfaces().stream().map(MachineInterfaceBlockEntity::getBlockPos)
              ).toList()
            : List.of();
        buf.writeInt(crafterPositions.size());
        for (BlockPos cp : crafterPositions) {
            buf.writeBlockPos(cp);
        }
    }

    private static AdvancedStorageCoreBlockEntity findCore(Level level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            Block block = level.getBlockState(pos).getBlock();
            if (block instanceof BlockAdvancedStorageCore
                    && level.getBlockEntity(pos) instanceof AdvancedStorageCoreBlockEntity core) {
                return core;
            }
            if (block instanceof StorageMultiblock || block instanceof BlockAdvancedStorageCore) {
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = pos.relative(dir);
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= PATTERN_SLOTS_START && slotId < PATTERN_SLOTS_START + PATTERN_SLOT_COUNT
                && blockEntity != null && player instanceof ServerPlayer serverPlayer) {
            int patternIndex = slotId - PATTERN_SLOTS_START;
            serverPlayer.openMenu(
                RecipePatternMenu.createMenuProvider(blockEntity, patternIndex, crafterPositions),
                buf -> {
                    buf.writeBlockPos(blockEntity.getBlockPos());
                    buf.writeInt(patternIndex);
                    buf.writeInt(crafterPositions.size());
                    for (BlockPos cp : crafterPositions) {
                        buf.writeBlockPos(cp);
                    }
                });
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
