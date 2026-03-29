package io.github.scuba10steve.s3.advanced.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/**
 * Stores a single crafting recipe pattern: a 3x3 ingredient grid, a pinned recipe ID
 * (resolves Polymorph-style ambiguity without a direct Polymorph dependency), and the
 * resolved output item. Identified externally by a PatternKey (BlockPos + index).
 */
public class RecipePattern {

    public static final int GRID_SIZE = 9;

    /** Ingredient templates; empty slots hold ItemStack.EMPTY. Never null entries. */
    private final ItemStack[] grid;
    /** Pinned Minecraft recipe ID. Null until the player sets the pattern. */
    private ResourceLocation pinnedRecipeId;
    /** Resolved output item. Empty if pattern is unresolved. */
    private ItemStack output;

    public RecipePattern() {
        this.grid = new ItemStack[GRID_SIZE];
        Arrays.fill(grid, ItemStack.EMPTY);
        this.output = ItemStack.EMPTY;
    }

    public ItemStack getIngredient(int slot) {
        if (slot < 0 || slot >= GRID_SIZE) {
            throw new IllegalArgumentException("Slot " + slot + " out of range [0, " + GRID_SIZE + ")");
        }
        return grid[slot];
    }

    /** Stores a defensive copy. */
    public void setIngredient(int slot, ItemStack stack) {
        if (slot < 0 || slot >= GRID_SIZE) {
            throw new IllegalArgumentException("Slot " + slot + " out of range [0, " + GRID_SIZE + ")");
        }
        grid[slot] = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    public ItemStack[] getGrid() {
        return Arrays.copyOf(grid, GRID_SIZE);
    }

    public ResourceLocation getPinnedRecipeId() {
        return pinnedRecipeId;
    }

    public void setPinnedRecipeId(ResourceLocation id) {
        this.pinnedRecipeId = id;
    }

    public ItemStack getOutput() {
        return output;
    }

    public void setOutput(ItemStack stack) {
        this.output = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    /** Returns true if no ingredient slots are filled. */
    public boolean isEmpty() {
        for (ItemStack s : grid) {
            if (!s.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ListTag gridTag = new ListTag();
        for (int i = 0; i < GRID_SIZE; i++) {
            CompoundTag slotTag = new CompoundTag();
            slotTag.putByte("Slot", (byte) i);
            if (!grid[i].isEmpty()) {
                slotTag.put("Item", grid[i].save(registries));
            }
            gridTag.add(slotTag);
        }
        tag.put("Grid", gridTag);
        if (pinnedRecipeId != null) {
            tag.putString("PinnedRecipe", pinnedRecipeId.toString());
        }
        if (!output.isEmpty()) {
            tag.put("Output", output.save(registries));
        }
        return tag;
    }

    public static RecipePattern load(CompoundTag tag, HolderLookup.Provider registries) {
        RecipePattern pattern = new RecipePattern();
        ListTag gridTag = tag.getList("Grid", 10); // 10 = CompoundTag type
        for (int i = 0; i < gridTag.size(); i++) {
            CompoundTag slotTag = gridTag.getCompound(i);
            int slot = slotTag.getByte("Slot") & 0xFF;
            if (slot < GRID_SIZE && slotTag.contains("Item")) {
                pattern.grid[slot] = ItemStack.parseOptional(registries, slotTag.getCompound("Item"));
            }
        }
        if (tag.contains("PinnedRecipe")) {
            try {
                pattern.pinnedRecipeId = ResourceLocation.parse(tag.getString("PinnedRecipe"));
            } catch (net.minecraft.ResourceLocationException e) {
                // Corrupted or outdated NBT; leave pinnedRecipeId null so pattern degrades gracefully
            }
        }
        if (tag.contains("Output")) {
            pattern.output = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        }
        return pattern;
    }
}
