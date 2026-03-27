package io.github.scuba10steve.s3.advanced.crafting;

import net.minecraft.core.BlockPos;

/**
 * Identifies a specific recipe pattern slot: the BlockPos of the Recipe Memory Box
 * that owns it, and its index within that box.
 */
public record PatternKey(BlockPos pos, int index) {}
