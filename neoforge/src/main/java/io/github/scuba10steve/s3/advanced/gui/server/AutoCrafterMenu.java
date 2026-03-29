package io.github.scuba10steve.s3.advanced.gui.server;

import io.github.scuba10steve.s3.advanced.blockentity.AutoCrafterBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.RecipeMemoryBoxBlockEntity;
import io.github.scuba10steve.s3.advanced.crafting.PatternKey;
import io.github.scuba10steve.s3.advanced.crafting.PerPatternConfig;
import io.github.scuba10steve.s3.advanced.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AutoCrafterMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final Map<PatternKey, PerPatternConfig> assignments;
    /** Output item for each assigned pattern; client-side display only. */
    private final Map<PatternKey, ItemStack> outputItems;

    // Client constructor
    public AutoCrafterMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(ModMenuTypes.AUTO_CRAFTER.get(), containerId);
        this.pos = buf.readBlockPos();

        int count = buf.readInt();
        Map<PatternKey, PerPatternConfig> assignments = new LinkedHashMap<>(count);
        Map<PatternKey, ItemStack> outputItems = new LinkedHashMap<>(count);
        for (int i = 0; i < count; i++) {
            PatternKey key = new PatternKey(buf.readBlockPos(), buf.readInt());
            assignments.put(key, PerPatternConfig.decode(buf));
            outputItems.put(key, ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        this.assignments = Collections.unmodifiableMap(assignments);
        this.outputItems = Collections.unmodifiableMap(outputItems);
    }

    // Server constructor
    public AutoCrafterMenu(int containerId, Inventory playerInventory, AutoCrafterBlockEntity be) {
        super(ModMenuTypes.AUTO_CRAFTER.get(), containerId);
        this.pos = be.getBlockPos();
        this.assignments = Collections.unmodifiableMap(new LinkedHashMap<>(be.getAssignments()));
        this.outputItems = resolveOutputItems(be);
    }


    private static Map<PatternKey, ItemStack> resolveOutputItems(AutoCrafterBlockEntity be) {
        Level level = be.getLevel();
        if (level == null) return Map.of();
        Map<PatternKey, ItemStack> result = new LinkedHashMap<>();
        for (PatternKey key : be.getAssignments().keySet()) {
            ItemStack output = ItemStack.EMPTY;
            if (level.getBlockEntity(key.pos()) instanceof RecipeMemoryBoxBlockEntity rmbBe) {
                output = rmbBe.getPattern(key.index()).getOutput().copy();
            }
            result.put(key, output);
        }
        return result;
    }

    public Map<PatternKey, PerPatternConfig> getAssignments() {
        return assignments;
    }

    public Map<PatternKey, ItemStack> getOutputItems() {
        return outputItems;
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
}
