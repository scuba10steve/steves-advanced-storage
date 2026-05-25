import json, os

colors = ['white','orange','magenta','light_blue','yellow','lime','pink','gray',
          'light_gray','cyan','purple','blue','brown','green','red','black']

def to_cap(name):
    return ' '.join(w.capitalize() for w in name.split('_'))

def update_lang(filepath, prefix=''):
    with open(filepath) as f:
        data = json.load(f)
    for color in colors:
        key = f'block.s3_advanced.{color}_storage_cable'
        if key not in data:
            val = f'{prefix}{to_cap(color)} Storage Cable'
            data[key] = val
    with open(filepath, 'w') as f:
        json.dump(data, f, indent=2)

base = 'neoforge/src/main/resources/assets/s3_advanced/lang'
update_lang(os.path.join(base, 'en_us.json'))
update_lang(os.path.join(base, 'es_es.json'), prefix='[TRANSLATE] ')
print('Lang files updated')
