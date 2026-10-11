"""Generate 16x16 glass-frame tank textures, one per tier. Run from the repo root."""
from pathlib import Path
from PIL import Image

OUT = Path("neoforge/src/main/resources/assets/s3_advanced/textures/block")
TIERS = {
    "basic": ((216, 216, 216), (150, 150, 150)),
    "advanced": ((250, 214, 74), (176, 138, 20)),
    "super": ((74, 237, 217), (30, 150, 140)),
    "hyper": ((110, 100, 110), (60, 54, 60)),
}
GLASS = (230, 245, 255, 255)

for name, (edge, shade) in TIERS.items():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            px[x, y] = edge + (255,)
        for (x, y) in ((i, 1), (i, 14), (1, i), (14, i)):
            if 1 <= i <= 14:
                px[x, y] = shade + (255,)
    for (x, y) in ((4, 3), (3, 4), (5, 3), (3, 5), (11, 12), (12, 11)):
        px[x, y] = GLASS
    img.save(OUT / f"{name}_tank.png")
    print("wrote", OUT / f"{name}_tank.png")
