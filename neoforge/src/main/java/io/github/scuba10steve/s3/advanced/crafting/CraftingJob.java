package io.github.scuba10steve.s3.advanced.crafting;

/**
 * A single entry in the CraftingCoordinator's queue.
 * quantity is always 1 for AUTO_BUFFER jobs (incremental replenishment).
 */
public record CraftingJob(CrafterSlot crafterSlot, int quantity, CraftingSource source) {}
