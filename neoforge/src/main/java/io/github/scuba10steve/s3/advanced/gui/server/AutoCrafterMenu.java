package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.blockentity.AutoCrafterBlockEntity;
import io.github.scuba10steve.s3.advanced.crafting.PatternKey;
import io.github.scuba10steve.s3.advanced.crafting.PerPatternConfig;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AutoCrafterMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    /**
     * On client: populated from FriendlyByteBuf at open. Used by AutoCrafterScreen.
     * On server: null (screen interactions go through packets, not this map).
     */
    private final Map<PatternKey, PerPatternConfig> assignments;

    // Client constructor
    public AutoCrafterMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), null, readAssignments(buf));
    }

    // Server constructor
    public AutoCrafterMenu(int containerId, Inventory playerInventory, AutoCrafterBlockEntity be) {
        this(containerId, playerInventory, be.getBlockPos(), be,
            new LinkedHashMap<>(be.getAssignments()));
    }

    private AutoCrafterMenu(int containerId, Inventory playerInventory, BlockPos pos,
                             AutoCrafterBlockEntity be, Map<PatternKey, PerPatternConfig> assignments) {
        super(ModMenuTypes.AUTO_CRAFTER.get(), containerId);
        this.pos = pos;
        this.assignments = Collections.unmodifiableMap(assignments);

        // Player inventory — 3 rows
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    /** Returns the assignment map (client-side copy for rendering). */
    public Map<PatternKey, PerPatternConfig> getAssignments() {
        return assignments;
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    private static Map<PatternKey, PerPatternConfig> readAssignments(FriendlyByteBuf buf) {
        int count = buf.readInt();
        Map<PatternKey, PerPatternConfig> map = new LinkedHashMap<>(count);
        for (int i = 0; i < count; i++) {
            PatternKey key = new PatternKey(buf.readBlockPos(), buf.readInt());
            map.put(key, PerPatternConfig.decode(buf));
        }
        return map;
    }
}
