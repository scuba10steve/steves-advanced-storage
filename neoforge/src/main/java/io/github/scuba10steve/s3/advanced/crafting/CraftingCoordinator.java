package io.github.scuba10steve.s3.advanced.crafting;

import io.github.scuba10steve.s3.advanced.blockentity.AutoCrafterBlockEntity;
import io.github.scuba10steve.s3.advanced.blockentity.RecipeMemoryBoxBlockEntity;
import io.github.scuba10steve.s3.storage.StorageInventory;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CraftingCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CraftingCoordinator.class);

    private final CraftingEngine craftingEngine;
    private final Queue<CraftingJob> queue = new LinkedList<>();
    private final Set<CrafterSlot> pendingAutoBuffer = new HashSet<>();

    public CraftingCoordinator(CraftingEngine craftingEngine) {
        this.craftingEngine = craftingEngine;
    }

    /**
     * Enqueues a crafting job.
     * AUTO_BUFFER jobs are deduplicated by CrafterSlot; duplicates are silently dropped.
     * GUI_REQUEST jobs are never deduplicated.
     */
    public void enqueue(CrafterSlot slot, int quantity, CraftingSource source) {
        if (source == CraftingSource.AUTO_BUFFER) {
            if (pendingAutoBuffer.contains(slot)) return;
            pendingAutoBuffer.add(slot);
        }
        queue.add(new CraftingJob(slot, quantity, source));
    }

    public int getQueueSize() { return queue.size(); }

    /**
     * Called each server tick.
     * Checks auto-buffer thresholds for all paired (RMB, AutoCrafter) entries,
     * then dispatches queued jobs.
     *
     * @param inventory    The multiblock's shared StorageInventory.
     * @param rmbToCrafter Resolved pairs from AdvancedStorageCoreBlockEntity.scanMultiblock().
     */
    public boolean tick(StorageInventory inventory,
                        Map<RecipeMemoryBoxBlockEntity, AutoCrafterBlockEntity> rmbToCrafter) {
        boolean crafted = false;

        // 1. Auto-buffer check
        for (Map.Entry<RecipeMemoryBoxBlockEntity, AutoCrafterBlockEntity> entry : rmbToCrafter.entrySet()) {
            RecipeMemoryBoxBlockEntity rmb = entry.getKey();
            AutoCrafterBlockEntity crafter = entry.getValue();
            for (int i = 0; i < AutoCrafterBlockEntity.SLOT_COUNT; i++) {
                CrafterSlot slot = new CrafterSlot(crafter.getBlockPos(), i);
                PerPatternConfig config = crafter.getConfigs()[i];
                if (!config.autoEnabled() || pendingAutoBuffer.contains(slot)) continue;
                RecipePattern pattern = rmb.getPattern(i);
                if (pattern == null || pattern.isEmpty()) continue;
                int current = countItem(inventory, pattern.getOutput());
                if (current < config.minimumBuffer()) {
                    enqueue(slot, 1, CraftingSource.AUTO_BUFFER);
                }
            }
        }

        // 2. Dispatch queued jobs
        while (!queue.isEmpty()) {
            CraftingJob job = queue.poll();
            if (job.source() == CraftingSource.AUTO_BUFFER) {
                pendingAutoBuffer.remove(job.crafterSlot());
            }

            // Find the RMB paired to this crafter
            RecipeMemoryBoxBlockEntity rmb = null;
            for (Map.Entry<RecipeMemoryBoxBlockEntity, AutoCrafterBlockEntity> entry : rmbToCrafter.entrySet()) {
                if (entry.getValue().getBlockPos().equals(job.crafterSlot().crafterPos())) {
                    rmb = entry.getKey();
                    break;
                }
            }
            if (rmb == null) {
                LOGGER.debug("[Coordinator] DROPPED: no paired RMB for crafter {}", job.crafterSlot().crafterPos());
                continue;
            }

            RecipePattern pattern = rmb.getPattern(job.crafterSlot().slotIndex());
            if (pattern == null || pattern.isEmpty()) {
                LOGGER.debug("[Coordinator] DROPPED: empty pattern at slot {}", job.crafterSlot().slotIndex());
                continue;
            }

            List<ItemStack> ingredients = Arrays.asList(pattern.getGrid());
            for (int i = 0; i < job.quantity(); i++) {
                boolean success = craftingEngine.execute(ingredients, pattern.getOutput(), inventory);
                LOGGER.debug("[Coordinator] execute iteration {}/{}: {}", i + 1, job.quantity(), success ? "OK" : "FAILED");
                if (success) crafted = true;
                else break;
            }
        }
        return crafted;
    }

    private int countItem(StorageInventory inventory, ItemStack template) {
        if (template.isEmpty()) return 0;
        long total = 0;
        for (var stored : inventory.getStoredItems()) {
            if (ItemStack.isSameItemSameComponents(stored.getItemStack(), template)) {
                total += stored.getCount();
            }
        }
        return (int) Math.min(total, Integer.MAX_VALUE);
    }
}
