package io.github.scuba10steve.s3.advanced.crafting;

import io.github.scuba10steve.s3.storage.StorageInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CraftingCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CraftingCoordinator.class);

    /**
     * Snapshot of one Recipe Memory Box: its world position and pattern list.
     * Passed into tick() to avoid a circular crafting ↔ blockentity dependency.
     */
    public record BoxData(BlockPos pos, List<RecipePattern> patterns) {
        public RecipePattern get(int index) {
            if (index < 0 || index >= patterns.size()) {
                return null;
            }
            return patterns.get(index);
        }
    }

    /**
     * Snapshot of one Auto-Crafter: its assignment map.
     * Passed into tick() to avoid a circular crafting ↔ blockentity dependency.
     */
    public record CrafterData(Map<PatternKey, PerPatternConfig> assignments) {}

    private final CraftingEngine craftingEngine;
    private final Queue<CraftingJob> queue = new LinkedList<>();
    private final Set<PatternKey> pendingAutoBuffer = new HashSet<>();

    public CraftingCoordinator(CraftingEngine craftingEngine) {
        this.craftingEngine = craftingEngine;
    }

    /**
     * Enqueues a crafting job.
     * AUTO_BUFFER jobs are deduplicated by patternKey — if one is already outstanding,
     * the new request is silently dropped.
     * GUI_REQUEST jobs are never deduplicated.
     */
    public void enqueue(PatternKey patternKey, int quantity, CraftingSource source) {
        if (source == CraftingSource.AUTO_BUFFER) {
            if (pendingAutoBuffer.contains(patternKey)) {
                return;
            }
            pendingAutoBuffer.add(patternKey);
        }
        queue.add(new CraftingJob(patternKey, quantity, source));
    }

    /** Returns the current number of pending jobs. */
    public int getQueueSize() {
        return queue.size();
    }

    /**
     * Called each server tick. Checks auto-buffer thresholds and dispatches queued jobs.
     *
     * @param inventory  The multiblock's shared StorageInventory.
     * @param boxes      Snapshot of all Recipe Memory Boxes (patterns by box position).
     * @param crafters   Snapshot of all Auto-Crafters (assignments per crafter).
     */
    public boolean tick(StorageInventory inventory, List<BoxData> boxes, List<CrafterData> crafters) {
        boolean crafted = false;
        // 1. Auto-buffer check: enqueue jobs for patterns below their minimum buffer.
        for (CrafterData crafter : crafters) {
            for (Map.Entry<PatternKey, PerPatternConfig> entry : crafter.assignments().entrySet()) {
                PatternKey key = entry.getKey();
                PerPatternConfig config = entry.getValue();
                if (!config.autoEnabled()) {
                    continue;
                }
                if (pendingAutoBuffer.contains(key)) {
                    continue;
                }

                RecipePattern pattern = resolvePattern(key, boxes);
                if (pattern == null) {
                    continue;
                }

                int current = countItem(inventory, pattern.getOutput());
                if (current < config.minimumBuffer()) {
                    enqueue(key, 1, CraftingSource.AUTO_BUFFER);
                }
            }
        }

        // 2. Dispatch: drain the queue, resolve pattern, find a crafter, execute.
        while (!queue.isEmpty()) {
            CraftingJob job = queue.poll();
            LOGGER.debug("[Coordinator] Dispatching job patternKey={} qty={} source={}", job.patternKey(), job.quantity(), job.source());
            if (job.source() == CraftingSource.AUTO_BUFFER) {
                pendingAutoBuffer.remove(job.patternKey());
            }

            RecipePattern pattern = resolvePattern(job.patternKey(), boxes);
            if (pattern == null) {
                LOGGER.debug("[Coordinator] DROPPED: resolvePattern returned null for {} (boxes={})", job.patternKey(), boxes.stream().map(BoxData::pos).toList());
                continue; // pattern box removed — drop job
            }
            boolean hasCrafter = crafters.stream()
                .anyMatch(c -> c.assignments().containsKey(job.patternKey()));
            if (!hasCrafter) {
                LOGGER.debug("[Coordinator] DROPPED: no crafter assigned for {} (crafter keys={})",
                    job.patternKey(), crafters.stream().flatMap(c -> c.assignments().keySet().stream()).toList());
                continue; // no crafter assigned — drop job
            }
            List<ItemStack> ingredients = Arrays.asList(pattern.getGrid());
            for (int i = 0; i < job.quantity(); i++) {
                boolean success = craftingEngine.execute(ingredients, pattern.getOutput(), inventory);
                LOGGER.debug("[Coordinator] execute iteration {}/{}: {}", i + 1, job.quantity(), success ? "OK" : "FAILED (missing ingredients)");
                if (success) {
                    crafted = true;
                } else {
                    break; // missing ingredients — stop this job without retry
                }
            }
        }
        return crafted;
    }

    /**
     * Counts the number of items in storage matching the given template
     * (same item type and components).
     * StorageInventory has no countItem() method; we iterate getStoredItems()
     * and sum counts for matching entries.
     */
    private int countItem(StorageInventory inventory, ItemStack template) {
        if (template.isEmpty()) {
            return 0;
        }
        long total = 0;
        for (var stored : inventory.getStoredItems()) {
            if (ItemStack.isSameItemSameComponents(stored.getItemStack(), template)) {
                total += stored.getCount();
            }
        }
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    private RecipePattern resolvePattern(PatternKey key, List<BoxData> boxes) {
        for (BoxData box : boxes) {
            if (box.pos().equals(key.pos())) {
                RecipePattern p = box.get(key.index());
                return p != null && !p.isEmpty() ? p : null;
            }
        }
        return null;
    }
}
