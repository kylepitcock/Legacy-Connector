#!/usr/bin/env python3
"""Offline check of the data and resource side of Forge 1.20.1 jars against the 1.21.1 / NeoForge formats.

    python -I tools/compat/data1201.py <jar-or-dir> [<jar-or-dir> ...] [--json out.json] [--verbose N]

Run it on the original corpus (what is wrong) and on the translated jars (what the loader's DataConverter1201 left). Every
finding is a file that 1.21.1 would not load as the mod meant it (it is not a validator of the whole format: it knows the
breaking changes between Forge 1.20.1 and NeoForge 1.21.1). Output: per category, files and jars affected; per jar, files
per category. Exit code 0 always (it is a measurement).
"""
import json
import re
import sys
import zipfile
import collections
import os

PLURAL_DIRS = {
    'recipes': 'recipe', 'loot_tables': 'loot_table', 'advancements': 'advancement', 'structures': 'structure',
    'functions': 'function', 'item_modifiers': 'item_modifier', 'predicates': 'predicate',
}
PLURAL_TAGS = {'blocks': 'block', 'items': 'item', 'fluids': 'fluid', 'entity_types': 'entity_type', 'game_events': 'game_event',
               'functions': 'function'}
RENAMED_IDS = {'minecraft:grass': 'minecraft:short_grass', 'minecraft:sweeping': 'minecraft:sweeping_edge'}
OLD_LOOT_FUNCTIONS = {'minecraft:set_nbt', 'minecraft:copy_nbt', 'minecraft:looting_enchant', 'set_nbt', 'copy_nbt', 'looting_enchant'}
OLD_LOOT_CONDITIONS = {'minecraft:random_chance_with_looting', 'random_chance_with_looting'}
ITEM_PRED_OLD_KEYS = ('tag', 'nbt', 'enchantments', 'stored_enchantments', 'potion', 'durability')
ITEM_PRED_HOLDERS = ('item', 'tool', 'weapon', 'mainhand', 'offhand', 'head', 'chest', 'legs', 'feet', 'held_item')


def walk(node, fn, path=()):
    fn(node, path)
    if isinstance(node, dict):
        for k, v in node.items():
            walk(v, fn, path + (k,))
    elif isinstance(node, list):
        for i, v in enumerate(node):
            walk(v, fn, path + (i,))


def is_old_item_pred(o):
    return isinstance(o, dict) and any(k in o for k in ITEM_PRED_OLD_KEYS) and not any(k in o for k in ('type', 'function', 'condition', 'id'))


def check_json(rel, doc, add):
    """rel: path inside the jar (already in whatever folder naming the jar uses)."""
    m = re.match(r'data/([^/]+)/(.*)', rel)
    kind = None
    if m:
        ns, rest = m.group(1), m.group(2)
        kind = rest.split('/')[0]
    # forge: ids that became neoforge: (every JSON of the pack)
    def forge_ids(o, path):
        if isinstance(o, dict):
            for k in ('type', 'loader', 'condition'):
                v = o.get(k)
                if isinstance(v, str) and v.startswith('forge:'):
                    add('forge_ids', rel)
            for k in o:
                if k in ('forge_data', 'forge:conditions', 'forge_marker'):
                    add('forge_ids', rel)
        elif isinstance(o, str):
            if o.startswith('#forge:') or o.startswith('forge:') and len(path) and path[-1] in ('tag',) :
                add('forge_tags', rel)
    if rel.startswith('data/'):
        walk(doc, forge_ids)
    # renamed ids
    if m:
        def renamed(o, path):
            if isinstance(o, str) and o in RENAMED_IDS:
                add('renamed_ids', rel)
            elif isinstance(o, dict):
                for v in o.values():
                    if isinstance(v, str) and v in RENAMED_IDS:
                        add('renamed_ids', rel)
        walk(doc, renamed)
    if not m:
        # assets: model loader
        return
    if kind in ('recipes', 'recipe') or (kind and kind.startswith('recipe')):
        if not isinstance(doc, dict):
            return
        res = doc.get('result')
        t = doc.get('type', '')
        vanilla = isinstance(t, str) and (':' not in t or t.startswith('minecraft:'))
        if not vanilla and t not in ('forge:conditional', 'neoforge:conditional'):
            # a mod's own recipe type reads its own format (its serializer, not the game, decides)
            if 'conditions' in doc and 'neoforge:conditions' not in doc:
                add('recipe_conditions', rel)
            return
        if isinstance(res, str) or (isinstance(res, dict) and 'item' in res and 'id' not in res):
            add('recipe_result', rel)
        if 'conditions' in doc and 'neoforge:conditions' not in doc:
            add('recipe_conditions', rel)
        if t in ('forge:conditional', 'neoforge:conditional'):
            add('recipe_conditional', rel)

        def nbt(o, path):
            if isinstance(o, dict):
                if 'nbt' in o and ('item' in o or 'id' in o or 'type' in o):
                    add('recipe_nbt', rel)
                if o.get('type') in ('forge:nbt', 'forge:partial_nbt', 'forge:strict_nbt'):
                    add('recipe_nbt', rel)
        walk(doc, nbt)
    if kind in ('loot_tables', 'loot_table', 'loot_modifiers') or (kind == 'loot_modifiers'):
        def loot(o, path):
            if not isinstance(o, dict):
                return
            f = o.get('function')
            if f in OLD_LOOT_FUNCTIONS:
                add('loot_function', rel)
            if f in ('minecraft:set_attributes', 'set_attributes'):
                for mod in o.get('modifiers', []):
                    if 'id' not in mod:
                        add('loot_function', rel)
            if f in ('minecraft:set_potion', 'set_potion', 'minecraft:enchant_with_levels') and 'treasure' in o:
                add('loot_function', rel)
            c = o.get('condition')
            if c in OLD_LOOT_CONDITIONS:
                add('loot_condition', rel)
            if o.get('type') in ('minecraft:loot_table', 'loot_table') and 'name' in o and 'value' not in o:
                add('loot_function', rel)
            if c in ('minecraft:match_tool', 'match_tool') and is_old_item_pred(o.get('predicate')):
                add('item_predicate', rel)
            if is_old_item_pred(o) and path and path[-1] in ITEM_PRED_HOLDERS + ('items',):
                add('item_predicate', rel)
        walk(doc, loot)
    if kind in ('advancements', 'advancement'):
        if not isinstance(doc, dict):
            return
        icon = (doc.get('display') or {}).get('icon') if isinstance(doc.get('display'), dict) else None
        if isinstance(icon, dict) and ('item' in icon or 'nbt' in icon):
            add('adv_icon', rel)
        if 'conditions' in doc and 'neoforge:conditions' not in doc:
            add('recipe_conditions', rel)

        def pred(o, path):
            if is_old_item_pred(o) and path and path[-1] in ITEM_PRED_HOLDERS + ('items', 'predicate'):
                add('item_predicate', rel)
            if isinstance(o, dict) and isinstance(o.get('items'), list) and any(isinstance(x, dict) and is_old_item_pred(x) for x in o['items']):
                add('item_predicate', rel)
        walk(doc, pred)
    if kind == 'dimension_type' or rel.split('/')[2:3] == ['dimension_type']:
        pass
    if kind == 'dimension_type':
        if isinstance(doc, dict) and 'monster_spawn_light_level' not in doc:
            add('dimension_type', rel)
    if rest.startswith('worldgen/') and 'dimension_type' in rest and isinstance(doc, dict):
        pass
    if kind == 'forge':
        add('forge_dir', rel)


def check_path(rel, add):
    m = re.match(r'data/([^/]+)/([^/]+)/', rel)
    if m:
        d = m.group(2)
        if d in PLURAL_DIRS:
            add('plural_dir', rel)
        elif d == 'tags':
            sub = rel.split('/')[3] if rel.count('/') >= 4 else ''
            if sub in PLURAL_TAGS:
                add('plural_dir', rel)
        elif d == 'forge':
            add('forge_dir', rel)
        if m.group(1) == 'forge' and d == 'tags':
            add('forge_tags', rel)
        if m.group(1) == 'forge' and d == 'loot_modifiers':
            add('forge_dir', rel)
    if rel.startswith('assets/'):
        pass


def check_models(rel, doc, add):
    if not isinstance(doc, dict):
        return
    if re.match(r'assets/[^/]+/models/', rel):
        l = doc.get('loader')
        if isinstance(l, str) and l.startswith('forge:'):
            add('model_loader', rel)
        if 'forge_data' in doc:
            add('model_loader', rel)


def check_jar(path, add, verbose):
    out = {}
    try:
        z = zipfile.ZipFile(path)
    except Exception as e:
        return None
    names = z.namelist()
    has_toml = 'META-INF/mods.toml' in names and 'META-INF/neoforge.mods.toml' not in names
    if has_toml:
        add('mods_toml', 'META-INF/mods.toml')
    if 'pack.mcmeta' in names:
        try:
            pm = json.loads(z.read('pack.mcmeta').decode('utf-8', 'replace'))
            pf = (pm.get('pack') or {}).get('pack_format')
            has_assets = any(n.startswith('assets/') for n in names)
            has_data = any(n.startswith('data/') for n in names)
            if isinstance(pf, int) and ((has_assets and not has_data and pf != 34) or (has_data and not has_assets and pf != 48)
                                        or (has_assets and has_data and pf not in (34, 48))):
                add('pack_format', 'pack.mcmeta')
        except Exception:
            add('pack_format', 'pack.mcmeta')
    for n in names:
        if n.endswith('/'):
            continue
        check_path(n, add)
        if n.endswith('.json'):
            if not (n.startswith('data/') or (n.startswith('assets/') and '/models/' in n)):
                continue
            try:
                raw = z.read(n).decode('utf-8-sig', 'replace')
                doc = json.loads(raw)
            except Exception:
                try:
                    doc = json.loads(re.sub(r',\s*([}\]])', r'\1', raw))
                except Exception:
                    add('unparsable', n)
                    continue
            check_json(n, doc, add)
            check_models(n, doc, add)
    return True


def main(argv):
    paths = []
    json_out = None
    verbose = 0
    i = 0
    while i < len(argv):
        a = argv[i]
        if a == '--json':
            json_out = argv[i + 1]
            i += 2
            continue
        if a == '--verbose':
            verbose = int(argv[i + 1])
            i += 2
            continue
        if os.path.isdir(a):
            for dp, _, fs in os.walk(a):
                for f in fs:
                    if f.endswith('.jar'):
                        paths.append(os.path.join(dp, f))
        else:
            paths.append(a)
        i += 1
    total = collections.Counter()
    jars_hit = collections.defaultdict(set)
    per_jar = {}
    examples = collections.defaultdict(list)
    n_jars = 0
    for p in sorted(paths):
        counts = collections.Counter()
        seen = set()

        def add(cat, rel):
            if (cat, rel) in seen:
                return
            seen.add((cat, rel))
            counts[cat] += 1
            if len(examples[cat]) < max(verbose, 3):
                examples[cat].append(os.path.basename(p) + ':' + rel)
        ok = check_jar(p, add, verbose)
        if ok is None:
            continue
        n_jars += 1
        per_jar[os.path.basename(p)] = dict(counts)
        for k, v in counts.items():
            total[k] += v
            jars_hit[k].add(os.path.basename(p))
    print('jars: %d' % n_jars)
    print('%-20s %8s %6s' % ('category', 'files', 'jars'))
    for k, v in sorted(total.items(), key=lambda kv: -kv[1]):
        print('%-20s %8d %6d' % (k, v, len(jars_hit[k])))
    if not total:
        print('(no breaking files)')
    if verbose:
        for k in total:
            print('--', k)
            for e in examples[k]:
                print('   ', e)
    if json_out:
        with open(json_out, 'w') as f:
            json.dump({'jars': n_jars, 'total': dict(total), 'jarsHit': {k: len(v) for k, v in jars_hit.items()}, 'perJar': per_jar}, f, indent=1)


if __name__ == '__main__':
    main(sys.argv[1:])
