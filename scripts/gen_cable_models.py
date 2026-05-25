import json, os

colors = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray',
          'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']

base = 'neoforge/src/main/resources/assets/s3_advanced'
os.makedirs(f'{base}/blockstates', exist_ok=True)
os.makedirs(f'{base}/models/block', exist_ok=True)
os.makedirs(f'{base}/models/item', exist_ok=True)


def make_model(elements, tex):
    return {
        "render_type": "minecraft:cutout",
        "parent": "minecraft:block/block",
        "ambientocclusion": False,
        "elements": elements,
        "textures": {"0": tex, "particle": tex}
    }


def face(uv):
    return {"uv": uv, "texture": "#0"}


# Core: 4×4×4 cube at the center of the block
CORE_ELEMENT = {
    "from": [6, 6, 6], "to": [10, 10, 10],
    "faces": {d: face([0, 0, 4, 4]) for d in ("north", "south", "east", "west", "up", "down")}
}

# Horizontal arm pointing NORTH: [6,6,0]–[10,10,6] (4×4 cross-section, 6 deep)
# EW side faces span 6 (z-depth) × 4 (y-height) texture pixels
# UD faces span 4 (x-width) × 6 (z-depth) texture pixels
ARM_SIDE_ELEMENT = {
    "from": [6, 6, 0], "to": [10, 10, 6],
    "faces": {
        "north": face([0, 0, 4, 4]),
        "south": face([0, 0, 4, 4]),
        "east":  face([0, 0, 6, 4]),
        "west":  face([0, 0, 6, 4]),
        "up":    face([0, 0, 4, 6]),
        "down":  face([0, 0, 4, 6]),
    }
}

# Vertical arm pointing UP: [6,10,6]–[10,16,10] (4×4 cross-section, 6 tall)
# NS/EW side faces span 4 (x or z width) × 6 (y-height) texture pixels
ARM_VERT_ELEMENT = {
    "from": [6, 10, 6], "to": [10, 16, 10],
    "faces": {
        "north": face([0, 0, 4, 6]),
        "south": face([0, 0, 4, 6]),
        "east":  face([0, 0, 4, 6]),
        "west":  face([0, 0, 4, 6]),
        "up":    face([0, 0, 4, 4]),
        "down":  face([0, 0, 4, 4]),
    }
}

# Pillar used for inventory item display only (unchanged shape)
PILLAR_ELEMENT = {
    "from": [6, 0, 6], "to": [10, 16, 10],
    "faces": {
        "north": {"uv": [0, 0, 4, 16], "texture": "#0"},
        "east":  {"uv": [0, 0, 4, 16], "texture": "#0"},
        "south": {"uv": [0, 0, 4, 16], "texture": "#0"},
        "west":  {"uv": [0, 0, 4, 16], "texture": "#0"},
        "up":    {"uv": [0, 0, 4,  4], "texture": "#0"},
        "down":  {"uv": [0, 0, 4,  4], "texture": "#0"},
    }
}


for color in colors:
    tex = f"s3_advanced:block/storage_cable_{color}"

    # Blockstate: multipart — core always visible, arms conditional per direction.
    # Horizontal arm uses y-rotation (NORTH=0, EAST=90, SOUTH=180, WEST=270).
    # Vertical arm uses x=180 to flip UP arm into DOWN arm.
    blockstate = {
        "multipart": [
            {"apply": {"model": f"s3_advanced:block/{color}_storage_cable_core"}},
            {"when": {"north": "true"}, "apply": {"model": f"s3_advanced:block/{color}_storage_cable_arm_side"}},
            {"when": {"east":  "true"}, "apply": {"model": f"s3_advanced:block/{color}_storage_cable_arm_side", "y": 90}},
            {"when": {"south": "true"}, "apply": {"model": f"s3_advanced:block/{color}_storage_cable_arm_side", "y": 180}},
            {"when": {"west":  "true"}, "apply": {"model": f"s3_advanced:block/{color}_storage_cable_arm_side", "y": 270}},
            {"when": {"up":    "true"}, "apply": {"model": f"s3_advanced:block/{color}_storage_cable_arm_vertical"}},
            {"when": {"down":  "true"}, "apply": {"model": f"s3_advanced:block/{color}_storage_cable_arm_vertical", "x": 180}},
        ]
    }
    with open(os.path.join(base, 'blockstates', f'{color}_storage_cable.json'), 'w') as f:
        json.dump(blockstate, f, indent=2)

    # Block models
    for name, element in [
        (f'{color}_storage_cable',             PILLAR_ELEMENT),
        (f'{color}_storage_cable_core',        CORE_ELEMENT),
        (f'{color}_storage_cable_arm_side',    ARM_SIDE_ELEMENT),
        (f'{color}_storage_cable_arm_vertical', ARM_VERT_ELEMENT),
    ]:
        with open(os.path.join(base, 'models/block', f'{name}.json'), 'w') as f:
            json.dump(make_model([element], tex), f, indent=2)

    # Item model references the pillar model for a recognisable inventory icon
    with open(os.path.join(base, 'models/item', f'{color}_storage_cable.json'), 'w') as f:
        json.dump({"parent": f"s3_advanced:block/{color}_storage_cable"}, f, indent=2)

print(f'Generated {len(colors) * 4} block models, {len(colors)} item models, {len(colors)} blockstates')
