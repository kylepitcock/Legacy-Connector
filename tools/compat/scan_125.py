#!/usr/bin/env python3
"""Audit a folder of Minecraft 1.2.5 mod archives (ModLoader `mod_*` zips, early Forge 3.x, ModLoaderMP, client and server builds).

    python -I tools/compat/scan_125.py <archive-dir> [--json out.json] [--md out.md] [--snapshot docs/data/legacy-1.2.5-corpus.json]

Read-only: archives are opened as zips and class files are parsed, nothing is extracted or executed. The 1.2.5 game is identified by its obfuscated
(notch) names, the table is `loader/src/main/resources/legacyconnector/detect/notch-1.2.5.txt.gz` (client and server, written by tools/gen_notch_125.py);
the 1.4.7 table (`legacy/1.4.7/notch.txt`) is the comparison that tells a 1.4.7 jar from a 1.2.5 one. A jar is split into units by install root
(a zip holding `SMP/` and `SSP/` folders is two units; nested zips are separate units). Per unit:

  entrypoints      `mod_*` classes (BaseMod subclasses by name)
  side             client / server (which obfuscation the references fit), or `craftbukkit` for a build whose game names are net/minecraft/server (MCP-named)
  apis             ModLoader (calls), ModLoaderMP (BaseModMp / ModLoaderMp / Packet230ModLoader), Forge 3.x (net/minecraft/src/forge or forge/ references),
                   Packet250 (registerPacketChannel / sendPacket), other known libraries
  bundled          copies of ModLoader / BaseMod / ModLoaderMP / Forge classes inside the unit
  replaced         classes of the unit that carry the name of a 1.2.5 game class (a jar mod patch); the share of the unit's classes
  status           mod | jar-mod (mostly replaced vanilla classes) | api-distribution | library | craftbukkit-build
"""
import argparse, collections, gzip, io, json, os, re, struct, sys, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
RES = os.path.join(ROOT, 'loader', 'src', 'main', 'resources', 'legacyconnector')

ML_CLASSES = {'ModLoader', 'BaseMod', 'MLProp', 'ModTextureStatic', 'ModTextureAnimation', 'EntityRendererProxy'}
MP_CLASSES = {'BaseModMp', 'ModLoaderMp', 'Packet230ModLoader', 'ISpawnable', 'MLMPProp', 'EntityTrackerEntry'}
FORGE_PREFIXES = ('net/minecraft/src/forge/', 'forge/')
OTHER_API = {'GuiAPI': ('de/matthiasmann/', 'GuiModScreen', 'ModSettings', 'ModSettingScreen'), 'ModOptionsAPI': ('modoptionsapi/',),
             'ShockAhPI': ('SAPI', 'DimensionBase'), 'PlayerAPI': ('PlayerAPI', 'PlayerBase'), 'AudioMod': ('paulscode/', 'ibxm/', 'AudioMod'),
             'Bukkit/CraftBukkit': ('org/bukkit/', 'org/craftbukkit/'), 'BuildCraft': ('buildcraft/',), 'IC2': ('ic2/',),
             'NEI': ('codechicken/', 'NEI', 'mod_NotEnoughItems'), 'RedPower': ('com/eloraam/',), 'Forestry': ('forestry/',)}
ML_CALL_LOWER = re.compile(r'^[a-z]')


def parse(data):
    """(this, super, interfaces, refs, members, strings, declared) of a class file; members = (owner, name, desc) of method/field refs;
    declared = [(name, desc)] of its own methods."""
    if data[:4] != b'\xca\xfe\xba\xbe':
        return None
    n = struct.unpack_from('>H', data, 8)[0]
    pos, pool, i = 10, [None] * n, 1
    while i < n:
        t = data[pos]; pos += 1
        if t == 1:
            ln = struct.unpack_from('>H', data, pos)[0]
            pool[i] = ('u', data[pos + 2:pos + 2 + ln].decode('utf-8', 'replace')); pos += 2 + ln
        elif t in (3, 4): pos += 4
        elif t in (5, 6): pos += 8; i += 1
        elif t in (7, 8): pool[i] = ('c' if t == 7 else 's', struct.unpack_from('>H', data, pos)[0]); pos += 2
        elif t in (16, 19, 20): pos += 2
        elif t in (9, 10, 11): pool[i] = ('m', struct.unpack_from('>HH', data, pos)); pos += 4
        elif t == 12: pool[i] = ('nt', struct.unpack_from('>HH', data, pos)); pos += 4
        elif t == 15: pos += 3
        elif t in (17, 18): pos += 4
        else: return None
        i += 1
    u = lambda k: pool[k][1]
    cname = lambda k: u(pool[k][1])
    refs, members, strings = set(), [], set()
    for e in pool:
        if not e: continue
        if e[0] == 'c': refs.add(u(e[1]))
        elif e[0] == 's': strings.add(u(e[1]))
        elif e[0] == 'm':
            ci, nti = e[1]
            nm, ds = pool[nti][1]
            members.append((cname(ci), u(nm), u(ds)))
    flags, this, sup, ic = struct.unpack_from('>HHHH', data, pos)
    pos += 8 + 2 * ic
    ifaces = [cname(struct.unpack_from('>H', data, pos - 2 * ic + 2 * k)[0]) for k in range(ic)]
    declared = []
    for kind in ('f', 'm'):
        cnt = struct.unpack_from('>H', data, pos)[0]; pos += 2
        for _ in range(cnt):
            _, nm, ds, ac = struct.unpack_from('>HHHH', data, pos); pos += 8
            if kind == 'm': declared.append((u(nm), u(ds)))
            for _ in range(ac):
                size = struct.unpack_from('>I', data, pos + 2)[0]; pos += 6 + size
    for d in [m[2] for m in members] + [d for _, d in declared]:
        refs.update(re.findall(r'L([^;]+);', d))
    return cname(this), (cname(sup) if sup else ''), ifaces, refs, members, strings, declared


class Tables:
    def __init__(self):
        self.classes = {'c': set(), 's': set()}
        self.members = {'c': set(), 's': set()}
        for line in gzip.open(os.path.join(RES, 'detect', 'notch-1.2.5.txt.gz'), 'rt', encoding='utf-8'):
            p = line.rstrip('\n').split('\t')
            if p[1] == 'C': self.classes[p[0]].add(p[2])
            elif len(p) >= 5: self.members[p[0]].add('%s.%s %s' % (p[2], p[3], p[4]))
        self.m147 = set()
        for line in open(os.path.join(RES, 'legacy', '1.4.7', 'notch.txt'), encoding='utf-8'):
            p = line.split()
            if p[0] == 'M': self.m147.add('%s.%s %s' % (p[1], p[2], p[3]))
        self.vanilla_names = self.classes['c'] | self.classes['s']


def measure(tables, own, all_members):
    """(share on 1.2.5 client, server, 1.4.7, total) of the obfuscated-named member references (owner has no package, name at most 3 chars)."""
    hits = [0, 0, 0]; total = 0
    for owner, name, desc in all_members:
        if '/' in owner or owner in own or len(name) > 3 or name.startswith('<'): continue
        total += 1
        k = '%s.%s %s' % (owner, name, desc)
        if k in tables.members['c']: hits[0] += 1
        if k in tables.members['s']: hits[1] += 1
        if k in tables.m147: hits[2] += 1
    return hits, total


def collect(blob, label, units, errors, depth=0):
    if depth > 4:
        errors.append((label, 'nested archive depth limit')); return
    groups = collections.defaultdict(list)
    docs = []
    try:
        z = zipfile.ZipFile(io.BytesIO(blob))
    except Exception as e:
        errors.append((label, 'not a zip: %s' % e)); return
    with z:
        for info in z.infolist():
            fn = info.filename
            if info.is_dir() or fn.startswith('__MACOSX/'): continue
            low = fn.lower()
            if low.endswith(('.zip', '.jar')) and info.file_size < 64 << 20:
                collect(z.read(info), label + '!' + fn, units, errors, depth + 1)
            elif low.endswith('.class'):
                try:
                    p = parse(z.read(info))
                    if not p: raise ValueError('bad class')
                    suffix = p[0] + '.class'
                    root = fn[:-len(suffix)] if fn.endswith(suffix) else '?' + fn
                    groups[root].append(p)
                except Exception as e:
                    errors.append((label + '!' + fn, str(e)))
            elif low.endswith(('.txt', '.md', '.html')) and info.file_size < 200000:
                text = z.read(info).decode('utf-8', 'replace')
                docs.extend(l.strip()[:160] for l in text.splitlines() if re.search(r'minecraft\s*1\.\d|modloader|forge|requires', l, re.I))
    for root, classes in groups.items():
        units.append({'archive': label, 'root': root, 'classes': classes, 'docs': docs[:5]})


def analyze(tables, unit):
    classes = unit['classes']
    own = {c[0] for c in classes}
    all_members = [m for c in classes for m in c[4]]
    refs = set().union(*(c[3] for c in classes)) - own
    bukkit = any(n.startswith('net/minecraft/server/') and '/mod_' in '/' + n for n in own)
    hits, total = measure(tables, own, all_members)
    # api evidence
    ml_calls = collections.Counter(m[1] for m in all_members if m[0] == 'ModLoader')
    ml_lower = sum(v for k, v in ml_calls.items() if ML_CALL_LOWER.match(k))
    ml_upper = sum(v for k, v in ml_calls.items() if not ML_CALL_LOWER.match(k))
    mp = sorted(r for r in refs | own if r.split('/')[-1] in MP_CLASSES)
    forge_refs = sorted(r for r in refs if r.startswith(FORGE_PREFIXES))
    forge_own = sorted(n for n in own if n.startswith(FORGE_PREFIXES))
    entry = sorted(n for n in own if n.split('/')[-1].startswith('mod_') and '$' not in n)
    api = []
    if ml_calls or 'BaseMod' in refs or entry: api.append('ModLoader')
    if any(r.split('/')[-1] in ('BaseModMp', 'ModLoaderMp', 'Packet230ModLoader') for r in refs | own): api.append('ModLoaderMP')
    if forge_refs or forge_own: api.append('Forge3')
    if any(k in ml_calls for k in ('registerPacketChannel', 'sendPacket', 'serverChat')): api.append('Packet250')
    for name, keys in OTHER_API.items():
        if any(r.startswith(k) or r.split('/')[-1] == k for r in refs | own for k in keys): api.append(name)
    bundled = sorted(n for n in own if n.split('/')[-1] in ML_CLASSES | MP_CLASSES and n.count('/') == 0 or n.startswith(FORGE_PREFIXES))
    ver = None
    for c in classes:
        if c[0] == 'ModLoader':
            ver = next((s for s in c[5] if s.startswith('ModLoader ')), None)
    # replaced vanilla: a class with the name of a 1.2.5 game class (default-package obfuscated names or net/minecraft/...)
    # side
    if bukkit:
        side = 'craftbukkit'
    elif total < 5:
        side = 'unknown'
    else:
        best = max(hits[0], hits[1])
        side = 'server' if hits[1] > hits[0] else 'client'
        if best / total < 0.4: side = 'unknown'
    if side == 'unknown' and 'net/minecraft/client/Minecraft' in refs: side = 'client'
    if side == 'unknown' and 'net/minecraft/server/MinecraftServer' in refs: side = 'server'
    # replaced vanilla: a class with the name of a 1.2.5 game class of the unit's side (default-package obfuscated names or net/minecraft/...)
    names = tables.vanilla_names if side in ('unknown', 'craftbukkit') else tables.classes['c' if side == 'client' else 's']
    replaced = sorted(n for n in own if n in names and n.split('/')[-1] not in ML_CLASSES)
    non_api = [n for n in own if n not in bundled]
    share = len(replaced) / len(non_api) if non_api else 0.0
    share125 = max(hits[0], hits[1]) / total if total else None
    share147 = hits[2] / total if total else None
    # status
    own_api_only = bool(own) and all(n.split('/')[-1] in ML_CLASSES | MP_CLASSES or n.startswith(FORGE_PREFIXES) or n in tables.vanilla_names for n in own) and not entry
    if bukkit: status = 'craftbukkit-build'
    elif own_api_only and (bundled or replaced): status = 'api-distribution'
    elif replaced and share >= 0.25: status = 'jar-mod'          # mostly replaced game classes: cannot be translated as a standalone jar
    elif replaced and entry: status = 'mod-with-patches'          # a mod_* mod that also ships a few edited game classes
    elif replaced: status = 'game-patch'                          # no mod class, only edited game classes (a coremod-style jar mod)
    elif entry: status = 'mod'
    else: status = 'library'
    mc125 = share125 is not None and share125 >= 0.6 and (share147 is None or share147 + 0.15 < share125)
    return {'archive': unit['archive'], 'root': unit['root'], 'status': status, 'side': side, 'classes': len(own), 'entrypoints': entry,
            'apis': api, 'ml_calls': dict(sorted(ml_calls.items())), 'ml_style': 'lower' if ml_lower and not ml_upper else 'upper' if ml_upper and not ml_lower else 'mixed' if ml_upper else 'none',
            'bundled': bundled, 'bundled_modloader': ver, 'forge_refs': forge_refs[:8], 'replaced': replaced, 'replaced_share': round(share, 2),
            'obf_refs': total, 'share125': None if share125 is None else round(share125, 2), 'share147': None if share147 is None else round(share147, 2),
            'is125': bool(mc125), 'docs': unit['docs']}


def run(folder):
    tables = Tables()
    units, errors = [], []
    files = sorted(f for f in os.listdir(folder) if os.path.isfile(os.path.join(folder, f)))
    for f in files:
        if f.lower().endswith(('.zip', '.jar')):
            with open(os.path.join(folder, f), 'rb') as fh: collect(fh.read(), f, units, errors)
    rows = [analyze(tables, u) for u in units]
    return files, rows, errors


def pairs(rows):
    by = collections.defaultdict(list)
    for r in rows:
        for e in r['entrypoints']:
            by[e.split('/')[-1]].append(r)
    return {k: v for k, v in by.items() if len({x['side'] for x in v}) > 1 or len(v) > 1}


def summary(files, rows, errors):
    st = collections.Counter(r['status'] for r in rows)
    side = collections.Counter(r['side'] for r in rows if r['status'] not in ('library',))
    api = collections.Counter(a for r in rows for a in r['apis'])
    mods = [r for r in rows if r['entrypoints']]
    return {'archives': len(files), 'units': len(rows), 'units_with_mod_entrypoint': len(mods), 'status': dict(st), 'side': dict(side),
            'apis': dict(api), 'ml_style': dict(collections.Counter(r['ml_style'] for r in rows if r['ml_style'] != 'none')),
            'detected_as_1.2.5': sum(1 for r in rows if r['is125']),
            'with_bundled_modloader_or_forge': sum(1 for r in rows if r['bundled']),
            'with_replaced_vanilla_classes': sum(1 for r in rows if r['replaced']),
            'modloader_versions': dict(collections.Counter(r['bundled_modloader'] for r in rows if r['bundled_modloader'])),
            'read_errors': len(errors)}


def markdown(files, rows, errors):
    s = summary(files, rows, errors)
    o = ['# Legacy 1.2.5 corpus', '', 'Generated by `tools/compat/scan_125.py` from the MCArchive 1.2.5 category (not committed; the data snapshot is `docs/data/legacy-1.2.5-corpus.json`).', '']
    o += ['| | Count |', '|---|---|']
    o += ['| archive files | %d |' % s['archives'], '| install units (a zip with SMP/SSP folders or nested zips is several) | %d |' % s['units'],
          '| units with a `mod_*` entry class | %d |' % s['units_with_mod_entrypoint']]
    for k, v in sorted(s['status'].items()): o.append('| status `%s` | %d |' % (k, v))
    o += ['', 'Side (non-library units): ' + ', '.join('%s %d' % kv for kv in sorted(s['side'].items())) + '.',
          'API evidence: ' + ', '.join('%s %d' % kv for kv in sorted(s['apis'].items(), key=lambda kv: -kv[1])) + '.',
          'ModLoader call spelling: ' + ', '.join('%s %d' % kv for kv in sorted(s['ml_style'].items())) + '.', '']
    o += ['| unit | status | side | classes | entry | APIs | replaced vanilla | bundled | 1.2.5 / 1.4.7 fit |', '|---|---|---|---|---|---|---|---|---|']
    for r in rows:
        name = r['archive'] + ('' if not r['root'] else ' `' + r['root'] + '`')
        o.append('| %s | %s | %s | %d | %s | %s | %d (%d%%) | %s | %s / %s |' % (
            name.replace('|', '/'), r['status'], r['side'], r['classes'], ', '.join(e.split('/')[-1] for e in r['entrypoints'][:2]) or '-',
            ', '.join(r['apis']) or '-', len(r['replaced']), round(r['replaced_share'] * 100), len(r['bundled']) or '-',
            '-' if r['share125'] is None else '%d%%' % round(r['share125'] * 100), '-' if r['share147'] is None else '%d%%' % round(r['share147'] * 100)))
    return '\n'.join(o) + '\n'


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('folder')
    ap.add_argument('--json'); ap.add_argument('--md'); ap.add_argument('--snapshot')
    a = ap.parse_args()
    files, rows, errors = run(a.folder)
    if a.json:
        json.dump({'summary': summary(files, rows, errors), 'units': rows, 'errors': errors}, open(a.json, 'w', encoding='utf-8'), indent=1)
    if a.md:
        open(a.md, 'w', encoding='utf-8', newline='\n').write(markdown(files, rows, errors))
    if a.snapshot:
        snap = {'summary': summary(files, rows, errors),
                'units': [{k: r[k] for k in ('archive', 'root', 'status', 'side', 'classes', 'entrypoints', 'apis', 'ml_style', 'replaced_share', 'is125')} for r in rows]}
        json.dump(snap, open(a.snapshot, 'w', encoding='utf-8'), indent=0, separators=(',', ':'))
    print(json.dumps(summary(files, rows, errors), indent=1))


if __name__ == '__main__':
    main()
