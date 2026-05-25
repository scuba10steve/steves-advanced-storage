import json, os

colors = ['white','orange','magenta','light_blue','yellow','lime','pink','gray',
          'light_gray','cyan','purple','blue','brown','green','red','black']

base = 'neoforge/src/main/resources/data/s3_advanced/recipe'
os.makedirs(base, exist_ok=True)

for color in colors:
    recipe = {
        "type": "minecraft:crafting_shaped",
        "pattern": ["GGG", "BBB", "GGG"],
        "key": {
            "G": {"item": f"minecraft:{color}_stained_glass"},
            "B": {"item": "s3:blank_box"}
        },
        "result": {
            "id": f"s3_advanced:{color}_storage_cable",
            "count": 3
        }
    }
    path = os.path.join(base, f'{color}_storage_cable_from_glass.json')
    with open(path, 'w') as f:
        json.dump(recipe, f, indent=2)

for color in colors:
    if color == 'white':
        continue
    recipe = {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [
            {"item": "s3_advanced:white_storage_cable"},
            {"item": f"minecraft:{color}_dye"}
        ],
        "result": {
            "id": f"s3_advanced:{color}_storage_cable",
            "count": 1
        }
    }
    path = os.path.join(base, f'{color}_storage_cable_recolor.json')
    with open(path, 'w') as f:
        json.dump(recipe, f, indent=2)

print(f'Generated {len(colors)} colored-glass recipes + {len(colors)-1} recolor recipes')
