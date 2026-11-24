#!/usr/bin/env python3
"""Host symbol diff: which Minecraft/NeoForge symbols does the compiled runtime reference, and
what happened to them between two host versions (jar sets given as classpath files).

usage: hostdiff.py --classes DIR [--classes DIR ...] --old CPFILE --new CPFILE --out TSV [--summary JSON]
CPFILE: one jar path per line.
"""
import argparse, json, os, struct, sys, zipfile, collections, re

PREFIXES = ('net/minecraft/', 'net/neoforged/', 'com/mojang/', 'cpw/mods/')

def parse(data, with_refs=True, with_annots=False):
    pos = 0
    magic, minor, major, cpn = struct.unpack_from('>IHHH', data, 0)
    pos = 10
    cp = [None] * cpn
    i = 1
    while i < cpn:
        t = data[pos]
        if t == 1:
            ln = struct.unpack_from('>H', data, pos + 1)[0]
            cp[i] = (1, data[pos + 3:pos + 3 + ln].decode('utf-8', 'replace'))
            pos += 3 + ln
        elif t in (3, 4):
            pos += 5
        elif t in (5, 6):
            pos += 9; i += 1
        elif t in (7, 8, 16, 19, 20):
            cp[i] = (t, struct.unpack_from('>H', data, pos + 1)[0]); pos += 3
        elif t in (9, 10, 11, 12, 17, 18):
            cp[i] = (t, struct.unpack_from('>HH', data, pos + 1)); pos += 5
        elif t == 15:
            cp[i] = (t, struct.unpack_from('>BH', data, pos + 1)); pos += 4
        else:
            raise ValueError('cp tag %d' % t)
        i += 1
    def utf(ix): return cp[ix][1]
    def cname(ix): return utf(cp[ix][1])
    acc, this, sup, nif = struct.unpack_from('>HHHH', data, pos); pos += 8
    ifs = []
    for _ in range(nif):
        ifs.append(cname(struct.unpack_from('>H', data, pos)[0])); pos += 2
    info = {'name': cname(this), 'super': cname(sup) if sup else None, 'ifs': ifs, 'acc': acc,
            'fields': {}, 'methods': {}, 'annots': [], 'mannots': {}}
    def skip_attrs(p, collect=None):
        n = struct.unpack_from('>H', data, p)[0]; p += 2
        for _ in range(n):
            ni, ln = struct.unpack_from('>HI', data, p)
            if collect is not None and with_annots and utf(ni) in ('RuntimeVisibleAnnotations', 'RuntimeInvisibleAnnotations'):
                collect.extend(parse_annots(p + 6))
            p += 6 + ln
        return p
    def elem(p):
        tag = chr(data[p]); p += 1
        if tag in 'BCDFIJSZs':
            v = cp[struct.unpack_from('>H', data, p)[0]]; return (v[1] if tag == 's' else '#'), p + 2
        if tag == 'e':
            a, b = struct.unpack_from('>HH', data, p); return utf(a) + '.' + utf(b), p + 4
        if tag == 'c':
            return ('class:' + utf(struct.unpack_from('>H', data, p)[0])), p + 2
        if tag == '@':
            a, p2 = annot(p); return a, p2
        if tag == '[':
            n = struct.unpack_from('>H', data, p)[0]; p += 2; out = []
            for _ in range(n):
                v, p = elem(p); out.append(v)
            return out, p
        raise ValueError(tag)
    def annot(p):
        ti, np_ = struct.unpack_from('>HH', data, p); p += 4
        d = {}
        for _ in range(np_):
            ni = struct.unpack_from('>H', data, p)[0]; p += 2
            v, p = elem(p); d[utf(ni)] = v
        return {'type': utf(ti), 'v': d}, p
    def parse_annots(p):
        n = struct.unpack_from('>H', data, p)[0]; p += 2; out = []
        for _ in range(n):
            a, p = annot(p); out.append(a)
        return out
    for kind in ('fields', 'methods'):
        n = struct.unpack_from('>H', data, pos)[0]; pos += 2
        for _ in range(n):
            a, ni, di = struct.unpack_from('>HHH', data, pos); pos += 6
            key = (utf(ni), utf(di))
            info[kind][key] = a
            col = [] if kind == 'methods' or kind == 'fields' else None
            pos = skip_attrs(pos, col)
            if col: info['mannots'][(kind[0],) + key] = col
    skip_attrs(pos, info['annots'])
    if with_refs:
        refs = set(); crefs = set(); descs = set()
        for e in cp:
            if not e: continue
            if e[0] in (9, 10, 11):
                ci, nt = e[1]
                owner = cname(ci)
                n, d = cp[nt][1]
                refs.add(({9: 'F', 10: 'M', 11: 'M'}[e[0]], owner, utf(n), utf(d)))
            elif e[0] == 7:
                crefs.add(utf(e[1]))
        info['refs'] = refs; info['crefs'] = crefs
    return info

def load_jar(path, idx, prefixes=PREFIXES, with_annots=False):
    try:
        z = zipfile.ZipFile(path)
    except Exception as ex:
        print('skip', path, ex, file=sys.stderr); return
    for n in z.namelist():
        if n.endswith('.class') and n.startswith(prefixes) and n != 'module-info.class':
            name = n[:-6]
            if name in idx: continue
            try:
                idx[name] = parse(z.read(n), with_refs=False, with_annots=with_annots)
            except Exception as ex:
                print('bad', n, ex, file=sys.stderr)

def load_cp(cpfile, extra=None):
    idx = {}
    for line in open(cpfile, encoding='utf-8-sig'):
        line = line.strip()
        if line and os.path.exists(line) and line.endswith('.jar'):
            load_jar(line, idx)
    return idx

def desc_types(d):
    return re.findall(r'L([^;]+);', d)

def lineage(idx, name, seen=None):
    """name followed by all known ancestors (class chain first, then interfaces)."""
    out = []; stack = [name]; seen = set()
    while stack:
        n = stack.pop(0)
        if n in seen: continue
        seen.add(n)
        c = idx.get(n)
        if c is None: continue
        out.append(n)
        if c['super']: stack.append(c['super'])
        stack.extend(c['ifs'])
    return out

def resolve(idx, owner, name, desc, kind):
    owner = owner.lstrip('[')
    if owner.startswith('L') and owner.endswith(';'): owner = owner[1:-1]
    tbl = 'fields' if kind == 'F' else 'methods'
    for n in lineage(idx, owner):
        if (name, desc) in idx[n][tbl]: return n
    return None

def subsystem(owner):
    o = owner
    rules = [
        ('net/minecraft/client/renderer/entity', 'rendering: entity renderers'),
        ('net/minecraft/client/renderer/blockentity', 'rendering: block entity renderers'),
        ('net/minecraft/client/renderer/block', 'rendering: block rendering/models'),
        ('net/minecraft/client/renderer', 'rendering: renderer core (RenderType, shaders, buffers)'),
        ('net/minecraft/client/model', 'rendering: models/baking'),
        ('net/minecraft/client/resources/model', 'rendering: models/baking'),
        ('net/minecraft/client/particle', 'rendering: particles'),
        ('net/minecraft/client/gui', 'GUI: screens/graphics'),
        ('net/minecraft/client/Minecraft', 'client core'),
        ('net/minecraft/client', 'client core'),
        ('com/mojang/blaze3d', 'rendering: blaze3d (GL/vertex)'),
        ('com/mojang/math', 'math'),
        ('com/mojang/serialization', 'codecs/serialization'),
        ('com/mojang/brigadier', 'commands'),
        ('net/minecraft/commands', 'commands'),
        ('net/minecraft/core/component', 'data components'),
        ('net/minecraft/world/item/component', 'data components'),
        ('net/minecraft/core/registries', 'registries'),
        ('net/minecraft/core/Registry', 'registries'),
        ('net/minecraft/core/Holder', 'registries'),
        ('net/minecraft/core/Reference', 'registries'),
        ('net/minecraft/resources', 'registries/resources'),
        ('net/minecraft/tags', 'tags'),
        ('net/minecraft/network', 'networking'),
        ('net/neoforged/neoforge/network', 'networking'),
        ('net/neoforged/neoforge/capabilities', 'capabilities'),
        ('net/neoforged/neoforge/items', 'capabilities'),
        ('net/neoforged/neoforge/fluids', 'capabilities/fluids'),
        ('net/neoforged/neoforge/energy', 'capabilities'),
        ('net/neoforged/neoforge/event', 'neoforge events'),
        ('net/neoforged/bus', 'neoforge events'),
        ('net/neoforged/neoforge/registries', 'registries'),
        ('net/neoforged/neoforge/client', 'neoforge client extensions'),
        ('net/neoforged/neoforge/common', 'neoforge common'),
        ('net/neoforged/neoforge/server', 'neoforge common'),
        ('net/neoforged/fml', 'FML / mod loading'),
        ('net/neoforged/neoforgespi', 'FML / mod loading'),
        ('cpw/mods', 'FML / mod loading'),
        ('net/neoforged/neoforge', 'neoforge common'),
        ('net/neoforged', 'neoforge common'),
        ('net/minecraft/world/level/levelgen', 'worldgen'),
        ('net/minecraft/world/level/biome', 'worldgen'),
        ('net/minecraft/world/level/chunk', 'worldgen/chunks'),
        ('net/minecraft/world/level/block/entity', 'block entities'),
        ('net/minecraft/world/level/block', 'blocks'),
        ('net/minecraft/world/level/material', 'fluids/materials'),
        ('net/minecraft/world/level/storage', 'level storage/loot'),
        ('net/minecraft/world/level', 'world/level'),
        ('net/minecraft/world/item/crafting', 'recipes'),
        ('net/minecraft/world/item', 'items'),
        ('net/minecraft/world/entity/ai', 'entities: AI'),
        ('net/minecraft/world/entity/player', 'entities: players'),
        ('net/minecraft/world/entity', 'entities'),
        ('net/minecraft/world/inventory', 'menus/containers'),
        ('net/minecraft/world/Container', 'menus/containers'),
        ('net/minecraft/world/damagesource', 'damage/combat'),
        ('net/minecraft/world/effect', 'damage/combat'),
        ('net/minecraft/world/phys', 'math/physics'),
        ('net/minecraft/world', 'world misc'),
        ('net/minecraft/server', 'server'),
        ('net/minecraft/nbt', 'NBT'),
        ('net/minecraft/core', 'core (BlockPos, Direction...)'),
        ('net/minecraft/sounds', 'sounds'),
        ('net/minecraft/util', 'util'),
        ('net/minecraft/data', 'datagen'),
        ('net/minecraft/advancements', 'advancements'),
        ('net/minecraft/stats', 'stats'),
        ('net/minecraft/', 'minecraft other'),
        ('com/mojang', 'mojang libs other'),
    ]
    for p, s in rules:
        if o.startswith(p): return s
    return 'other'

def lc_group(name):
    # which of our code areas a class belongs to
    n = name
    for p, g in (('legacyconnector/v1_7_10/', 'shim 1.7.10'), ('legacyconnector/v1_12_2/', 'shim 1.12.2'),
                 ('legacyconnector/v1_6_4/', 'shim 1.6.4'), ('legacyconnector/vb1_7_3/', 'shim b1.7.3'),
                 ('legacyconnector/v1_20_1/', 'shim 1.20.1'), ('legacyconnector/', 'shim other'),
                 ('org/kylepitcock/legacyconnector/mixin', 'mixins'),
                 ('org/kylepitcock/legacyconnector/v1_7_10', 'bridges 1.7.10'),
                 ('org/kylepitcock/legacyconnector/v1_12_2', 'bridges 1.12.2'),
                 ('org/kylepitcock/legacyconnector/v1_6_4', 'bridges 1.6.4'),
                 ('org/kylepitcock/legacyconnector/vb1_7_3', 'bridges b1.7.3'),
                 ('org/kylepitcock/legacyconnector/v1_20_1', 'bridges 1.20.1'),
                 ('org/kylepitcock/legacyconnector/core', 'core'),
                 ('org/kylepitcock/legacyconnector/api', 'addon api'),
                 ('org/kylepitcock/legacyconnector/compat', 'compat'),
                 ('org/kylepitcock/legacyconnector/loader', 'loader')):
        if n.startswith(p): return g
    return 'runtime other'

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--classes', action='append', required=True)
    ap.add_argument('--old', required=True); ap.add_argument('--new', required=True)
    ap.add_argument('--out', required=True); ap.add_argument('--summary')
    ap.add_argument('--label', default='new')
    a = ap.parse_args()
    old = load_cp(a.old); new = load_cp(a.new)
    print('old classes', len(old), 'new classes', len(new), file=sys.stderr)
    # simple-name index of new for rename guesses
    simple = collections.defaultdict(list)
    for n in new: simple[n.rsplit('/', 1)[-1]].append(n)
    ours = {}
    for d in a.classes:
        for root, _, files in os.walk(d):
            for f in files:
                if f.endswith('.class'):
                    p = os.path.join(root, f)
                    try:
                        c = parse(open(p, 'rb').read(), with_refs=True, with_annots=True)
                    except Exception as ex:
                        print('bad', p, ex, file=sys.stderr); continue
                    ours[c['name']] = c
    print('our classes', len(ours), file=sys.stderr)
    # aggregate refs
    agg = {}  # (kind, owner, name, desc) -> set(users)
    cagg = collections.defaultdict(set)
    for cn, c in ours.items():
        for r in c['refs']:
            if r[1].lstrip('[').lstrip('L').startswith(PREFIXES):
                agg.setdefault(r, set()).add(cn)
        for r in c['crefs']:
            rr = r.lstrip('[')
            if rr.startswith('L') and rr.endswith(';'): rr = rr[1:-1]
            if rr.startswith(PREFIXES): cagg[rr].add(cn)
        # types in declared member descriptors
        for tbl in ('fields', 'methods'):
            for (n, d) in c[tbl]:
                for t in desc_types(d):
                    if t.startswith(PREFIXES): cagg[t].add(cn)
        for s in ([c['super']] + c['ifs']):
            if s and s.startswith(PREFIXES): cagg[s].add(cn)
    rows = []
    stat = collections.Counter()
    def classify_class(name):
        if name in new: return 'unchanged', ''
        cand = simple.get(name.rsplit('/', 1)[-1], [])
        if name not in old: return 'unresolved_in_old', ''
        return ('moved' if cand else 'removed'), ','.join(cand[:3])
    for name, users in sorted(cagg.items()):
        if name not in old and name not in new:
            continue  # not a host class (e.g. generated synth) - ignore
        st, det = classify_class(name)
        rows.append(('class', name, '', '', st, det, len(users), sorted(users), subsystem(name)))
    for (kind, owner, name, desc), users in sorted(agg.items()):
        o = owner.lstrip('[')
        if o.startswith('L') and o.endswith(';'): o = o[1:-1]
        if o not in old:
            continue  # unresolved even in old (e.g. synthesized class)
        so = resolve(old, owner, name, desc, kind)
        if so is None:
            stat['unresolved_in_old'] += 1
            continue
        sub = subsystem(o)
        tbl = 'fields' if kind == 'F' else 'methods'
        det = ''
        if o not in new:
            cand = simple.get(o.rsplit('/', 1)[-1], [])
            if cand:
                # look at candidate for the member
                ok = [c for c in cand if resolve(new, c, name, desc, kind)]
                st = 'class_moved' + ('_member_ok' if ok else '_member_changed')
                det = ','.join(cand[:2])
            else:
                st = 'class_removed'
        else:
            sn = resolve(new, o, name, desc, kind)
            if sn:
                # member present; check descriptor types still exist
                missing = [t for t in desc_types(desc) if t.startswith(PREFIXES) and t not in new]
                if missing:
                    st = 'type_removed'; det = ','.join(missing[:2])
                else:
                    st = 'unchanged'
                    if sn != so and not (so in new and so == sn): det = 'owner %s' % sn if sn != so else ''
            else:
                same = []
                for n in lineage(new, o):
                    for (mn, md) in new[n][tbl]:
                        if mn == name: same.append(md)
                if same:
                    st = 'signature_changed'; det = ' | '.join(sorted(set(same))[:3])
                else:
                    # rename guess: same desc, member name absent in old for owner lineage
                    oldnames = set();
                    for n in lineage(old, o):
                        oldnames.update(m for (m, d) in old[n][tbl])
                    cands = []
                    for n in lineage(new, o):
                        for (mn, md) in new[n][tbl]:
                            if md == desc and mn not in oldnames: cands.append(mn)
                    st = 'renamed?' if len(set(cands)) == 1 else 'removed'
                    det = cands[0] if st == 'renamed?' else ''
        rows.append((('field' if kind == 'F' else 'method'), o, name, desc, st, det, len(users), sorted(users), sub))
    # overrides: declared methods of our classes that override host methods
    for cn, c in sorted(ours.items()):
        chain = [x for x in lineage(old, c['super'])] if c['super'] else []
        for i in c['ifs']: chain += lineage(old, i)
        if not chain: continue
        chain_new = []
        if c['super']: chain_new += lineage(new, c['super'])
        for i in c['ifs']: chain_new += lineage(new, i)
        cands = [h for h in chain if h.startswith(PREFIXES)]
        if not cands: continue
        for (mn, md), acc in c['methods'].items():
            if mn in ('<init>', '<clinit>') or acc & 0x0A:  # private/static
                continue
            hit = [h for h in chain if (mn, md) in old[h]['methods'] and h.startswith(PREFIXES)]
            if not hit: continue
            if any((mn, md) in new[h]['methods'] for h in chain_new if h in new):
                st = 'unchanged'; det = ''
            else:
                same = sorted({d for h in chain_new for (n2, d) in new.get(h, {'methods': {}})['methods'] if n2 == mn})
                st = 'override_sig_changed' if same else 'override_gone'; det = ' | '.join(same[:3])
            if hit[0] not in new and st != 'unchanged':
                st = 'override_host_class_gone'
            rows.append(('override', hit[0], mn, md, st, det, 1, [cn], subsystem(hit[0])))
        # host class super changed/moved
        if c['super'] and c['super'] in old and c['super'] not in new:
            rows.append(('super', c['super'], '', '', 'class_removed', '', 1, [cn], subsystem(c['super'])))
    # new abstract methods not implemented
    for cn, c in sorted(ours.items()):
        if c['acc'] & 0x0600: continue  # abstract/interface
        sups = ([c['super']] if c['super'] else []) + c['ifs']
        if not any(s in new and s.startswith(PREFIXES) for s in sups) and not any(
                any(x.startswith(PREFIXES) for x in lineage(new, s)) for s in sups if s in new): continue
        # build lookup of concrete methods across the chain (ours + new host)
        both = lambda n: ours.get(n) or new.get(n)
        concrete = set(); abstract = {}
        seen = set(); stack = [cn]
        while stack:
            n = stack.pop(0)
            if n in seen: continue
            seen.add(n)
            x = both(n)
            if x is None: continue
            for (mn, md), acc in x['methods'].items():
                if acc & 0x0400:
                    abstract.setdefault((mn, md), n)
                elif not acc & 0x0008:
                    concrete.add((mn, md))
            if x['super']: stack.append(x['super'])
            stack.extend(x['ifs'])
        missing = [k for k in abstract if k not in concrete and abstract[k] in new and abstract[k].startswith(PREFIXES)]
        # only those not already abstract-unimplemented in old
        for k in missing:
            ow = abstract[k]
            was_abs_old = ow in old and k in old[ow]['methods']
            if was_abs_old: continue
            rows.append(('new_abstract', ow, k[0], k[1], 'new_abstract_unimplemented', '', 1, [cn], subsystem(ow)))
    # mixins
    for cn, c in sorted(ours.items()):
        mx = [x for x in c['annots'] if x['type'] == 'Lorg/spongepowered/asm/mixin/Mixin;']
        if not mx: continue
        tg = []
        for x in mx:
            v = x['v'].get('value', []); v = v if isinstance(v, list) else [v]
            tg += [t[7:-1] if t.startswith('class:L') else t for t in v]
            t2 = x['v'].get('targets', []); tg += (t2 if isinstance(t2, list) else [t2])
        tg = [t.replace('.', '/') for t in tg]
        for t in tg:
            st = 'unchanged' if t in new else ('class_moved' if simple.get(t.rsplit('/', 1)[-1]) else 'class_removed')
            if t not in old: continue
            rows.append(('mixin_target', t, '', '', st, ','.join(simple.get(t.rsplit('/', 1)[-1], [])[:2]) if st != 'unchanged' else '', 1, [cn], 'mixin: ' + subsystem(t)))
        def selectors(a):
            out = []
            m = a['v'].get('method')
            if m is not None: out += m if isinstance(m, list) else [m]
            return out
        for key, anns in c['mannots'].items():
            kind, mn, md = key
            for an in anns:
                ty = an['type']
                if not ty.startswith('Lorg/spongepowered/asm/mixin/') and not ty.startswith('Lcom/llamalad7/'): continue
                short = ty.rsplit('/', 1)[-1].rstrip(';')
                sels = []
                if short in ('Inject', 'Redirect', 'ModifyArg', 'ModifyArgs', 'ModifyVariable', 'ModifyConstant', 'WrapOperation', 'ModifyExpressionValue', 'WrapWithCondition', 'WrapMethod'):
                    sels = selectors(an)
                elif short in ('Shadow',):
                    sels = [mn]
                elif short in ('Accessor', 'Invoker'):
                    val = an['v'].get('value')
                    if not val:
                        base = re.sub(r'^(get|set|is|call|invoke)', '', mn)
                        val = base[:1].lower() + base[1:]
                    sels = [val]
                elif short == 'Overwrite':
                    sels = [mn]
                # @At target
                for k2, v2 in an['v'].items():
                    vs = v2 if isinstance(v2, list) else [v2]
                    for item in vs:
                        if isinstance(item, dict) and item.get('type', '').endswith('/At;'):
                            tgt = item['v'].get('target')
                            if tgt: sels.append('@' + tgt)
                for sel in sels:
                    if not isinstance(sel, str): continue
                    for t in tg or []:
                        if t not in old: continue
                        res = check_selector(old, new, t, sel, short, kind, md)
                        if res: rows.append(('mixin_' + short, t, sel, '', res[0], res[1], 1, [cn], 'mixin: ' + subsystem(t)))
    with open(a.out, 'w', encoding='utf-8') as f:
        f.write('kind\towner\tname\tdesc\tstatus\tdetail\tusers\texample_users\tsubsystem\n')
        for r in rows:
            f.write('\t'.join([r[0], r[1], r[2], r[3], r[4], r[5], str(r[6]), ';'.join(r[7][:3]), r[8]]) + '\n')
    per = collections.Counter()
    for r in rows:
        if r[4] != 'unchanged':
            for u in r[7]: per[u] += 1
    with open(a.out + '.classes.tsv', 'w', encoding='utf-8') as f:
        f.write('class\tgroup\tbroken_refs\n')
        for u, n in per.most_common(): f.write('%s\t%s\t%d\n' % (u, lc_group(u), n))
    c = collections.Counter((r[0], r[4]) for r in rows)
    print(json.dumps({str(k): v for k, v in sorted(c.items())}, indent=1))
    print('unresolved_in_old refs skipped:', stat['unresolved_in_old'], file=sys.stderr)

def check_selector(old, new, target, sel, short, kind, md):
    # returns (status, detail) or None if cannot tell
    at = sel.startswith('@')
    if at: sel = sel[1:]
    owner = target
    m = re.match(r'^(?:L([^;]+);)?([^(;:]+)?(\(.*\).*)?$', sel)
    fld = re.match(r'^(?:L([^;]+);)?([^(:;]+):(.+)$', sel)
    if fld:
        ow = fld.group(1) or target; nm = fld.group(2); ds = fld.group(3)
        if ow not in old: return None
        if ow not in new: return ('class_removed', ow)
        if resolve(new, ow, nm, ds, 'F'): return ('unchanged', '')
        return ('removed', nm)
    if not m: return None
    ow = m.group(1) or target; nm = m.group(2); ds = m.group(3)
    if nm is None or nm == '': return None
    if nm.startswith('*') or nm == '<init>' and ds is None: pass
    if ow not in old: return None
    if ow not in new: return ('class_removed', ow)
    if ds:
        if resolve(new, ow, nm, ds, 'M'): return ('unchanged', '')
        if short in ('Shadow', 'Accessor', 'Invoker') and False: pass
        same = sorted({d for n in lineage(new, ow) for (mn, d) in new[n]['methods'] if mn == nm})
        return ('signature_changed' if same else 'removed', ' | '.join(same[:3]))
    # name only: any method or field with this name
    for n in lineage(new, ow):
        if any(mn == nm for (mn, d) in new[n]['methods']) or any(fn == nm for (fn, d) in new[n]['fields']):
            return ('unchanged', '')
    return ('removed', '')

if __name__ == '__main__':
    main()
