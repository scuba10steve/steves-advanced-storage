# Storage Cables — Design

## Overview
Colored cable blocks that extend the multiblock structure across distances. Purely cosmetic — no power draw, no block entity.

## Block Design
- **`BlockStorageCable extends BlockBlankBox`** — strength 1.0f, no block entity, no ticker
- 16 color variants: `{color}_storage_cable` (all 16 dye colors)
- Auto-discovered by the base multiblock BFS — no `scanMultiblock()` changes needed

## Visual
- Custom thin model (4x16x4 pixel central pipe-like element)
- Side texture based on `metal_pipes_copper.png`, recolored per variant
- One blockstate JSON per color, no blockstate properties

## Recipes
- **Direct:** 3 blank boxes + 6 matching colored glass → 3 cables (shaped)
- **Recolor:** white cable + dye → 1 colored cable (shapeless)

## Registration
Single `BlockStorageCable` class, instantiated 16 times in `ModBlocks`. Each gets a `BlockItem` in `ModItems`. Listed in creative tab. No `ModBlockEntities` entry.
