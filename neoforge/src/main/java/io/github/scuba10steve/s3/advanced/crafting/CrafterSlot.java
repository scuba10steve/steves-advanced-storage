package io.github.scuba10steve.s3.advanced.crafting;

import net.minecraft.core.BlockPos;

/**
 * Identifies a specific slot on an Auto-Crafter: the crafter's world position
 * and the slot index (0–3) corresponding to its paired RMB's pattern slot.
 * Replaces PatternKey as the coordinator's job deduplication key.
 */
public record CrafterSlot(BlockPos crafterPos, int slotIndex) {}
