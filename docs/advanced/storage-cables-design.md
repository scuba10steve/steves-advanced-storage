# Storage Cables — Design

## Overview
Colored cable blocks that extend the multiblock structure across distances. Purely cosmetic — no power draw, no block entity.

## Block Design
- **`BlockStorageCable extends StorageMultiblock`** — strength 1.0f, no block entity, no ticker
- 16 color variants: `{color}_storage_cable` (all 16 dye colors)
- Auto-discovered by the base multiblock BFS — no `scanMultiblock()` changes needed

## Visual
- Multipart blockstate per color — always renders a `_core` model; conditionally renders a `_arm_side` (rotated 0/90/180/270°) or `_arm_vertical` (x:0/x:180) per direction
- 6 directional `BooleanProperty` block state properties: `NORTH`, `SOUTH`, `EAST`, `WEST`, `UP`, `DOWN` (from `BlockStateProperties`)
- Connection state computed on placement (`getStateForPlacement`) and updated via `updateShape` on neighbor changes
- `.noOcclusion()` required on block properties; `render_type: cutout` required in model JSONs — omitting either causes invisible geometry or black transparency artifacts
- Side texture based on `metal_pipes_copper.png`, recolored per variant

## Recipes
- **Direct:** 3 blank boxes + 6 matching colored glass → 3 cables (shaped)
- **Recolor:** white cable + dye → 1 colored cable (shapeless)

## Registration
Single `BlockStorageCable` class, instantiated 16 times in `ModBlocks`. Each gets a `BlockItem` in `ModItems`. Listed in creative tab. No `ModBlockEntities` entry.
