package io.github.scuba10steve.s3.advanced.crafting;

import io.github.scuba10steve.s3.storage.StorageInventory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Owns the auto-crafting job queue. Deduplicates AUTO_STOCK jobs by PatternKey.
 * Recipe dispatch logic is added in subsequent issues (#10/#11).
 */
public class CraftingCoordinator {

    private final CraftingEngine craftingEngine;
    private final Queue<CraftingJob> queue = new LinkedList<>();
    /** Tracks which pattern keys already have a pending AUTO_STOCK job. */
    private final Set<PatternKey> pendingAutoStock = new HashSet<>();

    public CraftingCoordinator(CraftingEngine craftingEngine) {
        this.craftingEngine = craftingEngine;
    }

    /**
     * Enqueues a crafting job.
     * AUTO_STOCK jobs are deduplicated by patternKey — if one is already outstanding,
     * the new request is silently dropped.
     * GUI_REQUEST jobs are never deduplicated.
     */
    public void enqueue(PatternKey patternKey, int quantity, CraftingSource source) {
        if (source == CraftingSource.AUTO_STOCK) {
            if (pendingAutoStock.contains(patternKey)) {
                return;
            }
            pendingAutoStock.add(patternKey);
        }
        queue.add(new CraftingJob(patternKey, quantity, source));
    }

    /** Returns the current number of pending jobs. */
    public int getQueueSize() {
        return queue.size();
    }

    /**
     * Called each server tick by AdvancedStorageCoreBlockEntity.
     * Drains the queue. Dispatch logic (resolving patterns from RecipeMemoryBoxBlockEntity,
     * executing via AutoCrafterBlockEntity or MachineInterfaceBlockEntity) will be added
     * in issues #10, #11, and #12.
     *
     * @param inventory The multiblock's shared StorageInventory.
     */
    public void tick(StorageInventory inventory) {
        while (!queue.isEmpty()) {
            CraftingJob job = queue.poll();
            if (job.source() == CraftingSource.AUTO_STOCK) {
                pendingAutoStock.remove(job.patternKey());
            }
            // TODO (#10/#11): resolve pattern from RecipeMemoryBoxBlockEntity and dispatch
        }
    }
}
