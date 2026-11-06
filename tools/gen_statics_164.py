#!/usr/bin/env python3
"""Generates the vanilla static accessors of the 1.6.4 Block/Item facades.

1.6.4 mods read vanilla blocks and items as `Block.dirt`, `Item.ingotIron`; the 1.7.10 shim has them as `Blocks.dirt` / `Items.iron_ingot`.
For every static field of Block/Item in loader/.../legacy/1.6.4/fields.txt that src/main/resources/legacyconnector/v1_6_4/{blocks,items}.tsv
maps to a 1.7.10 registry name, this writes `mcc$get$<field>()` (the accessor the loader redirects the field read to) into the marked section of
src/main/java/legacyconnector/v1_6_4/net/minecraft/{block/Block,item/Item}.java. The accessor's return type is the field's 1.6.4 type after
the class renames (forward/1.6.4/class-renames.tsv); a field whose type the shim lacks is skipped (the loader then synthesizes it as before).

Run from the repository root: python tools/gen_statics_164.py
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FIELDS = os.path.join(ROOT, 'loader/src/main/resources/legacyconnector/legacy/1.6.4/fields.txt')
RENAMES = os.path.join(ROOT, 'loader/src/main/resources/legacyconnector/forward/1.6.4/class-renames.tsv')
DATA = os.path.join(ROOT, 'src/main/resources/legacyconnector/v1_6_4')
SHIM = os.path.join(ROOT, 'src/main/java/legacyconnector/v1_7_10')
FACADE = os.path.join(ROOT, 'src/main/java/legacyconnector/v1_6_4/net/minecraft')
BEGIN = '    // BEGIN GENERATED'
END = '    // END GENERATED'


def tsv(name):
    rows = []
    for line in open(os.path.join(DATA, name), encoding='utf-8'):
        if line.startswith('#') or not line.strip():
            continue
        rows.append(line.rstrip('\n').split('\t'))
    return rows


def main():
    renames = {}
    for line in open(RENAMES, encoding='utf-8'):
        if line.startswith('#') or '\t' not in line:
            continue
        a, b = line.rstrip('\n').split('\t')[:2]
        renames[a] = b
    fields = {}
    for line in open(FIELDS, encoding='utf-8'):
        p = line.rstrip('\n').split(' ')
        if len(p) >= 5:
            fields.setdefault(p[1], {})[p[2]] = p[3]

    def shim_type(desc):
        m = re.fullmatch(r'L([^;]+);', desc)
        if not m:
            return None
        name = renames.get(m.group(1), m.group(1))
        if not os.path.exists(os.path.join(SHIM, name + '.java')):
            return None
        return 'legacyconnector.v1_7_10.' + name.replace('/', '.')

    jobs = [
        ('block/Block.java', 'net/minecraft/block/Block', 'blocks.tsv', 'legacyconnector.v1_7_10.net.minecraft.init.Blocks', 'Ids.as', 'Ids.vanillaBlock'),
        ('item/Item.java', 'net/minecraft/item/Item', 'items.tsv', 'legacyconnector.v1_7_10.net.minecraft.init.Items', 'Ids.as', 'Ids.vanillaItem'),
    ]
    for rel, cls, data, holder, cast, byname in jobs:
        holder_src = open(os.path.join(SHIM, holder.replace('legacyconnector.v1_7_10.', '').replace('.', '/') + '.java'), encoding='utf-8').read()
        names = {}
        for r in tsv(data):
            if r[2] != '-':
                names[r[1]] = r[2]
        out = []
        skipped = []
        for fname, desc in sorted(fields[cls].items()):
            if fname not in names:
                continue
            t = shim_type(desc)
            if t is None:
                skipped.append(fname)
                continue
            holder_field = names[fname]
            out.append('    /** {}.{} = {}.{} */'.format(cls.split('/')[-1], fname, holder.split('.')[-1], holder_field))
            out.append('    public static {} mcc$get${}() {{'.format(t, fname))
            # a few vanilla objects have no static in the shim's Blocks/Items (they are looked up by name)
            if (' ' + holder_field + ' = ') in holder_src:
                value = '{}.{}'.format(holder, holder_field)
            else:
                value = '{}("{}")'.format(byname, holder_field)
            out.append('        return {}({}.class, {});'.format(cast, t, value))
            out.append('    }')
            out.append('')
        path = os.path.join(FACADE, rel)
        text = open(path, encoding='utf-8').read()
        b = text.index(BEGIN)
        e = text.index(END)
        head = text[:text.index('\n', b) + 1]
        text = head + '\n'.join(out) + ('\n' if out else '') + text[e:]
        open(path, 'w', encoding='utf-8', newline='\n').write(text)
        print('{}: {} accessors, skipped (type not in shim): {}'.format(rel, sum(1 for l in out if 'public static' in l), skipped))


if __name__ == '__main__':
    sys.exit(main())
