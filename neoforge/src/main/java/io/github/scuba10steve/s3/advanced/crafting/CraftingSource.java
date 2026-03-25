package io.github.scuba10steve.s3.advanced.crafting;

public enum CraftingSource {
    /** Triggered by a player request from the storage GUI. Not deduplicated. */
    GUI_REQUEST,
    /** Triggered automatically to maintain minimum stock. Deduplicated by PatternKey. */
    AUTO_STOCK
}
