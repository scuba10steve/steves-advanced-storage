from PIL import Image
import os

colors = ['white','orange','magenta','light_blue','yellow','lime','pink','gray',
          'light_gray','cyan','purple','blue','brown','green','red','black']

tints = {
    'white': (255,255,255), 'orange': (216,127,51), 'magenta': (178,76,216),
    'light_blue': (102,153,216), 'yellow': (229,229,51), 'lime': (127,204,25),
    'pink': (242,127,165), 'gray': (76,76,76), 'light_gray': (153,153,153),
    'cyan': (51,127,127), 'purple': (127,63,178), 'blue': (51,76,178),
    'brown': (102,76,51), 'green': (76,127,51), 'red': (153,51,51),
    'black': (25,25,25)
}

src = Image.open('../unused-minecraft-textures/blocks/metal_pipes_copper.png').convert('RGBA')
out_dir = 'neoforge/src/main/resources/assets/s3_advanced/textures/block'
os.makedirs(out_dir, exist_ok=True)

for color in colors:
    tint = tints[color]
    img = src.copy()
    pixels = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = pixels[x, y]
            if a > 0:
                pixels[x, y] = (
                    min(255, int(r * tint[0] / 255)),
                    min(255, int(g * tint[1] / 255)),
                    min(255, int(b * tint[2] / 255)),
                    a
                )
    out_path = os.path.join(out_dir, f'storage_cable_{color}.png')
    img.save(out_path)
    print(f'Created {out_path}')
