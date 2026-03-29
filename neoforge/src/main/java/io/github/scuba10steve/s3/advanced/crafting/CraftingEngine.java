package io.github.scuba10steve.s3.advanced.crafting;

import io.github.scuba10steve.s3.storage.StorageInventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure recipe execution delegate. Consumes ingredients from a StorageInventory and
 * inserts the output. On failure, all partial extractions are rolled back.
 * Has no knowledge of patterns, queues, or the coordinator.
 */
public class CraftingEngine {

    /**
     * Attempts to craft by consuming ingredients and inserting the output.
     *
     * @param ingredients Ingredient stacks. Each entry is extracted exactly once.
     *                    Empty stacks are skipped.
     * @param output      The stack to insert into storage on success.
     * @param inventory   The multiblock's shared StorageInventory.
     * @return true if all ingredients were available and the craft succeeded;
     *         false if any ingredient was unavailable (storage unchanged).
     */
    public boolean execute(List<ItemStack> ingredients, ItemStack output, StorageInventory inventory) {
        List<ItemStack> extracted = new ArrayList<>();

        for (ItemStack ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                continue;
            }

            ItemStack got = inventory.extractItem(ingredient, ingredient.getCount());
            if (got.getCount() < ingredient.getCount()) {
                // Insufficient — rollback
                if (!got.isEmpty()) {
                    inventory.insertItem(got);
                }
                for (ItemStack e : extracted) {
                    inventory.insertItem(e);
                }
                return false;
            }
            extracted.add(got);
        }

        inventory.insertItem(output.copy());
        return true;
    }
}
