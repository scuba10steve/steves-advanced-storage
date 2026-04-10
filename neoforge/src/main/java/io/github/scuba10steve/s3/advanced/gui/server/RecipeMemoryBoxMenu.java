package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.blockentity.RecipeMemoryBoxBlockEntity;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RecipeMemoryBoxMenu extends AbstractContainerMenu {

    public static final int PATTERN_SLOT_COUNT = RecipeMemoryBoxBlockEntity.MAX_PATTERNS;
    private static final int PATTERN_SLOTS_START = 0;

    private final BlockPos pos;
    private final RecipeMemoryBoxBlockEntity blockEntity;

    // Client constructor
    public RecipeMemoryBoxMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), null, new SimpleContainer(PATTERN_SLOT_COUNT));
    }

    // Server constructor
    public RecipeMemoryBoxMenu(int containerId, Inventory playerInventory, RecipeMemoryBoxBlockEntity be) {
        this(containerId, playerInventory, be.getBlockPos(), be, buildDisplayContainer(be));
    }

    private RecipeMemoryBoxMenu(int containerId, Inventory playerInventory, BlockPos pos,
                                RecipeMemoryBoxBlockEntity be, SimpleContainer displayContainer) {
        super(ModMenuTypes.RECIPE_MEMORY_BOX.get(), containerId);
        this.pos = pos;
        this.blockEntity = be;

        for (int i = 0; i < PATTERN_SLOT_COUNT; i++) {
            int row = i / 2, col = i % 2;
            addSlot(new Slot(displayContainer, i, 26 + col * 18, 17 + row * 18) {
                @Override public boolean mayPickup(Player player) { return false; }
                @Override public boolean mayPlace(ItemStack stack) { return false; }
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

    public BlockPos getRmbPos() { return pos; }

    private static SimpleContainer buildDisplayContainer(RecipeMemoryBoxBlockEntity be) {
        SimpleContainer container = new SimpleContainer(PATTERN_SLOT_COUNT);
        for (int i = 0; i < PATTERN_SLOT_COUNT; i++) {
            container.setItem(i, be.getPattern(i).getOutput().copy());
        }
        return container;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= PATTERN_SLOTS_START && slotId < PATTERN_SLOTS_START + PATTERN_SLOT_COUNT
                && blockEntity != null && player instanceof ServerPlayer serverPlayer) {
            int patternIndex = slotId - PATTERN_SLOTS_START;
            serverPlayer.openMenu(
                RecipePatternMenu.createMenuProvider(blockEntity, patternIndex),
                buf -> {
                    buf.writeBlockPos(blockEntity.getBlockPos());
                    buf.writeInt(patternIndex);
                });
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
