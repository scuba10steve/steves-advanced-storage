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


# The cable texture is a cross/plus shape (16x16 RGBA):
#   Opaque:      center column x=5-10 (all y) and center band y=5-10 (all x)
#   Transparent: four corners (x<5 or x>10 AND y<5 or y>10)
#
# All UVs below sample the opaque cross region:
#   4x4 face  -> UV [6, 6, 10, 10]   (center of cross, fully opaque)
#   4x6 face  -> UV [6, 5, 10, 11]   (4 wide x 6 tall, center column)
#   6x4 face  -> UV [5, 6, 11, 10]   (6 wide x 4 tall, center band)

# Core: 4x4x4 cube at the center of the block
CORE_ELEMENT = {
    "from": [6, 6, 6], "to": [10, 10, 10],
    "faces": {d: face([6, 6, 10, 10]) for d in ("north", "south", "east", "west", "up", "down")}
}

# Horizontal arm pointing NORTH: [6,6,0] to [10,10,6] (4x4 cross-section, 6 deep)
# EW side faces: 6 (z-depth) x 4 (y-height) -> UV [5, 6, 11, 10]
# UD faces: 4 (x-width) x 6 (z-depth) -> UV [6, 5, 10, 11]
# NS end-cap faces: 4x4 -> UV [6, 6, 10, 10]
ARM_SIDE_ELEMENT = {
    "from": [6, 6, 0], "to": [10, 10, 6],
    "faces": {
        "north": face([6, 6, 10, 10]),
        "south": face([6, 6, 10, 10]),
        "east":  face([5, 6, 11, 10]),
        "west":  face([5, 6, 11, 10]),
        "up":    face([6, 5, 10, 11]),
        "down":  face([6, 5, 10, 11]),
    }
}

# Vertical arm pointing UP: [6,10,6] to [10,16,10] (4x4 cross-section, 6 tall)
# NS/EW side faces: 4 (x or z width) x 6 (y-height) -> UV [6, 5, 10, 11]
# Up end-cap and down inner faces: 4x4 -> UV [6, 6, 10, 10]
ARM_VERT_ELEMENT = {
    "from": [6, 10, 6], "to": [10, 16, 10],
    "faces": {
        "north": face([6, 5, 10, 11]),
        "south": face([6, 5, 10, 11]),
        "east":  face([6, 5, 10, 11]),
        "west":  face([6, 5, 10, 11]),
        "up":    face([6, 6, 10, 10]),
        "down":  face([6, 6, 10, 10]),
    }
}

# Pillar used for inventory item display only (unchanged shape)
PILLAR_ELEMENT = {
    "from": [6, 0, 6], "to": [10, 16, 10],
    "faces": {
        "north": {"uv": [6, 0, 10, 16], "texture": "#0"},
        "east":  {"uv": [6, 0, 10, 16], "texture": "#0"},
        "south": {"uv": [6, 0, 10, 16], "texture": "#0"},
        "west":  {"uv": [6, 0, 10, 16], "texture": "#0"},
        "up":    {"uv": [6, 6, 10, 10], "texture": "#0"},
        "down":  {"uv": [6, 6, 10, 10], "texture": "#0"},
    }
}


for color in colors:
    tex = f"s3_advanced:block/storage_cable_{color}"

    # Blockstate: multipart -- core always visible, arms conditional per direction.
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
        (f'{color}_storage_cable',              PILLAR_ELEMENT),
        (f'{color}_storage_cable_core',         CORE_ELEMENT),
        (f'{color}_storage_cable_arm_side',     ARM_SIDE_ELEMENT),
        (f'{color}_storage_cable_arm_vertical', ARM_VERT_ELEMENT),
    ]:
        with open(os.path.join(base, 'models/block', f'{name}.json'), 'w') as f:
            json.dump(make_model([element], tex), f, indent=2)

    # Item model references the pillar model for a recognisable inventory icon
    with open(os.path.join(base, 'models/item', f'{color}_storage_cable.json'), 'w') as f:
        json.dump({"parent": f"s3_advanced:block/{color}_storage_cable"}, f, indent=2)

print(f'Generated {len(colors) * 4} block models, {len(colors)} item models, {len(colors)} blockstates')
