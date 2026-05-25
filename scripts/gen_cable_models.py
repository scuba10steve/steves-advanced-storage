import json, os

colors = ['white','orange','magenta','light_blue','yellow','lime','pink','gray',
          'light_gray','cyan','purple','blue','brown','green','red','black']

base = 'neoforge/src/main/resources/assets/s3_advanced'
os.makedirs(f'{base}/blockstates', exist_ok=True)
os.makedirs(f'{base}/models/block', exist_ok=True)
os.makedirs(f'{base}/models/item', exist_ok=True)

# Blockstate JSONs
for color in colors:
    path = os.path.join(base, 'blockstates', f'{color}_storage_cable.json')
    with open(path, 'w') as f:
        json.dump({"variants": {"": {"model": f"s3_advanced:block/{color}_storage_cable"}}}, f, indent=2)

# Block model JSONs — thin central pipe element (4x16x4 pixels)
for color in colors:
    path = os.path.join(base, 'models/block', f'{color}_storage_cable.json')
    model = {
        "parent": "minecraft:block/block",
        "ambientocclusion": False,
        "elements": [
            {
                "from": [6, 0, 6],
                "to": [10, 16, 10],
                "faces": {
                    "north": {"uv": [0, 0, 4, 16], "texture": "#0"},
                    "east": {"uv": [0, 0, 4, 16], "texture": "#0"},
                    "south": {"uv": [0, 0, 4, 16], "texture": "#0"},
                    "west": {"uv": [0, 0, 4, 16], "texture": "#0"},
                    "up": {"uv": [0, 0, 4, 4], "texture": "#0"},
                    "down": {"uv": [0, 0, 4, 4], "texture": "#0"}
                }
            }
        ],
        "textures": {
            "0": f"s3_advanced:block/storage_cable_{color}",
            "particle": f"s3_advanced:block/storage_cable_{color}"
        }
    }
    with open(path, 'w') as f:
        json.dump(model, f, indent=2)

# Item model JSONs
for color in colors:
    path = os.path.join(base, 'models/item', f'{color}_storage_cable.json')
    with open(path, 'w') as f:
        json.dump({"parent": f"s3_advanced:block/{color}_storage_cable"}, f, indent=2)

print('Generated 48 model/blockstate files')
