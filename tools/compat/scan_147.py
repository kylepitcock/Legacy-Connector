#!/usr/bin/env python3
"""Scan a folder of Minecraft 1.4.7 (or 1.5.2) Forge/ModLoader mod jars and measure which Minecraft/Forge APIs they use.

    python tools/compat/scan_147.py <jar-dir> [--mc 1.4.7|1.5.2] [--json out.json] [--md out.md] [--top 40]

The 1.4.7 counterpart of scan_164.py. Differences that shape it: 1.4.x mods are shipped with the game's OBFUSCATED (notch) class, field and method names
(FML deobfuscates at load time), vanilla classes are the flat `net.minecraft.src.<Name>` of MCP 6.x in the few unobfuscated jars, and textures are
sprite-sheet indices rather than icons. So every reference is first resolved to the MCP class (notch -> `legacy/<mc>/notch.txt`), then bucketed:

  present      the class (after the flat -> 1.5.2 package move and the 1.6.4 forward renames) exists in the hand-written shim
               (`src/main/java/legacyconnector/v1_7_10` or the id facade `v1_6_4`)
  renamed      a forward rename or the flat -> packaged move leads to a shim class
  synth        not in the shim, but the 1.7.10 hierarchy knows the class, so the loader can synthesize it
  removed      an API 1.7.10 dropped (tick/network/player tracker families, texture FX, ModLoader...); needs a facade
  modloader    the ModLoader emulation FML keeps in net.minecraft.src (BaseMod, ModLoader, MLProp...)
  unknown      a vanilla/Forge/FML class none of the above knows (a 1.4.7-only class)
  obfuscated?  default-package short name not in the notch table (a library's own class or another version's obfuscation)

Needs `tools/fetch_devtools.sh` data (build/devtools/mcp/forge147-src.zip) for the SRG -> MCP member names.
"""
import argparse, collections, csv, io, json, os, re, struct, sys, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, HERE)
import scan_164 as s164  # noqa: E402  (shared archive walker and the 1.6.4 rename/removed lists)

RES = os.path.join(ROOT, 'loader', 'src', 'main', 'resources', 'legacyconnector')
ML_FLAT = {'BaseMod', 'ModLoader', 'MLProp', 'ModTextureStatic', 'ModTextureAnimation', 'TradeEntry', 'EntityRendererProxy', 'FMLRenderAccessLibrary'}
OBF = re.compile(r'^[a-zA-Z_$][a-zA-Z0-9_$]{0,5}$')

NUMERIC_FIELDS = {'blocksList': 'Block.blocksList', 'itemsList': 'Item.itemsList', 'blockID': 'blockID', 'itemID': 'itemID',
                  'shiftedIndex': 'Item.shiftedIndex', 'biomeList': 'BiomeGenBase.biomeList', 'enchantmentsList': 'Enchantment.enchantmentsList',
                  'potionTypes': 'Potion.potionTypes', 'biomeID': 'biomeID', 'effectId': 'effectId'}
NUMERIC_METHODS = {'getBlockId': 'World.getBlockId', 'setBlockAndMetadataWithNotify': 'World.setBlock(int ids)', 'setBlockWithNotify': 'World.setBlock(int ids)',
                   'setBlock': 'World.setBlock(int ids)', 'setBlockMetadataWithNotify': 'World.setBlockMetadata(int ids)',
                   'getBlockMaterial': 'World.getBlockMaterial(x,y,z)', 'isBlockOpaqueCube': 'World.isBlockOpaqueCube(x,y,z)'}
CTOR_OWNERS = {'Block', 'Item', 'ItemStack', 'Enchantment', 'Potion', 'BiomeGenBase'}
# sprite-sheet texturing (1.4.x): Forge's per-block/item texture file, the index fields and the per-side lookup mods override
TEXTURE_MEMBERS = {'setTextureFile': 'setTextureFile', 'getTextureFile': 'getTextureFile', 'blockIndexInTexture': 'blockIndexInTexture (field)',
                   'iconIndex': 'iconIndex (field)', 'setIconIndex': 'Item.setIconIndex', 'getBlockTextureFromSideAndMetadata': 'getBlockTextureFromSideAndMetadata',
                   'getBlockTextureFromSide': 'getBlockTextureFromSide', 'getBlockTexture': 'getBlockTexture(world,x,y,z,side)',
                   'getIconFromDamage': 'getIconFromDamage', 'getIconIndex': 'getIconIndex', 'preloadTexture': 'MinecraftForgeClient.preloadTexture',
                   'bindTexture': 'RenderEngine.bindTexture', 'getTexture': 'RenderEngine.getTexture', 'setBlockBounds': None,
                   'registerTextureFX': 'registerTextureFX', 'addTextureFX': 'addTextureFX', 'addOverride': 'ModLoader.addOverride',
                   'getBlockTextureFromSideAndMetadata_': None}
STRING_STYLES = [('/terrain.png', 'terrain.png sheet'), ('/gui/items.png', 'gui/items.png sheet'), ('/mods/', '/mods/<id>/ path'),
                 ('/gui/', '/gui/ path'), ('/mob/', '/mob/ path'), ('/item/', '/item/ path'), ('/armor/', '/armor/ path'),
                 ('/misc/', '/misc/ path'), ('/terrain/', '/terrain/ path'), ('/textures/', '/textures/ path'), ('/gfx/', '/gfx/ path'),
                 ('/%', None), ('.png', '.png string')]
LIFECYCLE = ['Lcpw/mods/fml/common/Mod$PreInit;', 'Lcpw/mods/fml/common/Mod$Init;', 'Lcpw/mods/fml/common/Mod$PostInit;',
             'Lcpw/mods/fml/common/Mod$ServerStarting;', 'Lcpw/mods/fml/common/Mod$ServerStopping;']
NET_CLASSES = ['cpw/mods/fml/common/network/NetworkMod', 'cpw/mods/fml/common/network/IPacketHandler', 'net/minecraft/src/Packet250CustomPayload',
               'cpw/mods/fml/common/network/NetworkRegistry', 'cpw/mods/fml/common/network/PacketDispatcher', 'cpw/mods/fml/common/network/IConnectionHandler',
               'cpw/mods/fml/common/network/IGuiHandler', 'cpw/mods/fml/common/registry/TickRegistry', 'cpw/mods/fml/common/ITickHandler',
               'cpw/mods/fml/common/network/Player', 'cpw/mods/fml/common/IPlayerTracker']


def parse(data):
    """(classes, members, utf8, strings, this, super) of a class file. members: (kind, owner, name, desc)."""
    if data[:4] != b'\xca\xfe\xba\xbe':
        return None
    n = struct.unpack('>H', data[8:10])[0]
    pos, pool, i = 10, [None] * n, 1
    while i < n:
        t = data[pos]; pos += 1
        if t == 1:
            ln = struct.unpack('>H', data[pos:pos + 2])[0]
            pool[i] = ('u', data[pos + 2:pos + 2 + ln].decode('utf-8', 'replace')); pos += 2 + ln
        elif t in (3, 4): pos += 4
        elif t in (5, 6): pos += 8; i += 1
        elif t == 7: pool[i] = ('c', struct.unpack('>H', data[pos:pos + 2])[0]); pos += 2
        elif t == 8: pool[i] = ('s', struct.unpack('>H', data[pos:pos + 2])[0]); pos += 2
        elif t in (16, 19, 20): pos += 2
        elif t in (9, 10, 11): pool[i] = ('m', t, struct.unpack('>HH', data[pos:pos + 4])); pos += 4
        elif t == 12: pool[i] = ('nt', struct.unpack('>HH', data[pos:pos + 4])); pos += 4
        elif t == 15: pos += 3
        elif t in (17, 18): pos += 4
        else: return None
        i += 1
    u = lambda k: pool[k][1]
    classes, members, utf8, strings = set(), [], set(), set()
    for e in pool:
        if not e: continue
        if e[0] == 'u': utf8.add(e[1])
        elif e[0] == 'c': classes.add(u(e[1]))
        elif e[0] == 's': strings.add(u(e[1]))
        elif e[0] == 'm':
            ci, nti = e[2]
            nm, ds = pool[nti][1]
            members.append((e[1], u(pool[ci][1]), u(nm), u(ds)))
    this = struct.unpack('>H', data[pos + 2:pos + 4])[0]
    sup = struct.unpack('>H', data[pos + 4:pos + 6])[0]
    return classes, members, utf8, strings, u(pool[this][1]), (u(pool[sup][1]) if sup else None)


def desc_classes(desc):
    return re.findall(r'L([^;]+);', desc)


class Data:
    def __init__(self, mc):
        self.mc = mc
        self.notch_class, self.notch_field, self.notch_method = {}, {}, {}
        for line in open(os.path.join(RES, 'legacy', mc, 'notch.txt'), encoding='utf-8'):
            p = line.split()
            if p[0] == 'C': self.notch_class[p[1]] = p[2]
            elif p[0] == 'F': self.notch_field[(p[1], p[2])] = p[3]
            elif p[0] == 'M': self.notch_method[(p[1], p[2], p[3])] = p[4]
        self.hier = {}
        for line in open(os.path.join(RES, 'legacy', mc, 'hierarchy.txt'), encoding='utf-8'):
            p = line.split()
            self.hier[p[0]] = p[1]
        self.h17 = {line.split()[0] for line in open(os.path.join(RES, 'legacy', '1.7.10', 'hierarchy.txt'), encoding='utf-8')}
        self.flat = {}
        if mc == '1.4.7':
            self.flat = self.table(os.path.join(RES, 'forward', '1.4.7', 'flat-names.tsv'))
        self.fwd = {}
        for v in (['1.6.4', '1.5.2'] if mc == '1.4.7' else ['1.6.4']):
            self.fwd.update(self.table(os.path.join(RES, 'forward', v, 'class-renames.tsv')))
        self.shim = set()
        for sub in ('v1_7_10', 'v1_6_4'):
            base = os.path.join(ROOT, 'src', 'main', 'java', 'legacyconnector', sub)
            for dp, _, fs in os.walk(base):
                for f in fs:
                    if f.endswith('.java'):
                        self.shim.add(os.path.relpath(os.path.join(dp, f), base).replace(os.sep, '/')[:-5])
        # SRG -> MCP member names
        self.mcp = {}
        z = os.path.join(ROOT, 'build', 'devtools', 'mcp', 'forge%s-src.zip' % mc.replace('.', ''))
        if os.path.exists(z):
            zf = zipfile.ZipFile(z)
            for name in ('forge/fml/conf/methods.csv', 'forge/fml/conf/fields.csv'):
                for row in csv.reader(io.TextIOWrapper(zf.open(name), 'utf-8')):
                    if row and row[0].startswith(('func_', 'field_')):
                        self.mcp[row[0]] = row[1]
        else:
            print('warning: %s missing (run tools/fetch_devtools.sh); member names stay SRG' % z, file=sys.stderr)

    @staticmethod
    def table(path):
        out = {}
        if os.path.exists(path):
            for line in open(path, encoding='utf-8'):
                if line.strip() and not line.startswith('#') and '\t' in line:
                    a, b = line.rstrip('\n').split('\t')[:2]
                    out[a] = b
        return out

    def mcp_class(self, c):
        """The MCP class of a class reference (notch name resolved), or None for a non-vanilla one."""
        if '/' not in c:
            return self.notch_class.get(c)
        return c

    def member_name(self, owner, name, desc, field):
        """MCP name of a member reference (owner as written in the jar)."""
        key = (owner, name) if field else (owner, name, desc)
        srg = (self.notch_field if field else self.notch_method).get(key, name)
        return self.mcp.get(srg, srg)

    def final(self, mcp_class):
        """The 1.7.10 shim name of an MCP class (flat -> packaged -> forward renames)."""
        base, _, rest = mcp_class.partition('$')
        packaged = self.flat.get(base, base)
        packaged = self.fwd.get(packaged, packaged)
        return packaged + ('$' + rest if rest else '')

    def bucket(self, c):
        """(bucket, hint) of a class reference as written in a jar, or None when it is not Minecraft/Forge."""
        if '/' not in c:
            if c in ML_FLAT:  # the ModLoader emulation sits in the default package of a reobfuscated jar
                return 'modloader', None
            m = self.mcp_class(c)
            if m is None:
                return ('obfuscated?', None) if OBF.match(c) else None
            c = m
        simple = c.rsplit('/', 1)[-1].split('$')[0]
        if c.startswith('net/minecraft/src/') and simple in ML_FLAT:
            return 'modloader', None
        if not c.startswith(('net/minecraft/', 'net/minecraftforge/', 'cpw/mods/fml/')):
            return None
        known = c in self.hier
        hint = s164.RENAMED.get(c) or s164.RENAMED.get(c.split('$')[0])
        if hint and hint.startswith('removed'):
            return 'removed', hint
        fin = self.final(c)
        if fin.split('$')[0] in self.shim:
            # "present": the class exists in the shim once the flat name moved into its 1.5.2 package; "renamed": a forward rename was needed as well
            moved = self.flat.get(c.split('$')[0], c.split('$')[0])
            return ('renamed' if self.fwd.get(moved) else 'present'), (fin if fin != c else None)
        if fin in self.h17 or fin.split('$')[0] in self.h17:
            return 'synth', fin
        if not known and not hint:
            return 'unknown', None
        return 'removed', hint or 'not in 1.7.10'


def scan(jardir, data):
    jars = sorted(f for f in os.listdir(jardir) if f.lower().endswith(('.jar', '.zip', '.litemod')))
    per = {}
    for j in jars:
        try:
            z = zipfile.ZipFile(os.path.join(jardir, j))
        except Exception as e:
            print('skip', j, e, file=sys.stderr); continue
        files = list(s164.entries(z))
        own = {n[:-6] for n, _ in files}
        refs, numeric, ann, texture, strings_seen, net = (collections.Counter() for _ in range(6))
        flags = collections.Counter()
        nclass = 0
        bare, notch_hit = 0, 0
        for n, raw in files:
            nclass += 1
            try:
                r = parse(raw)
            except Exception:
                r = None
            if not r:
                continue
            classes, members, utf8, strings, this, sup = r
            seen = set()
            for c in classes:
                c = c.lstrip('[')
                if c.startswith('L') and c.endswith(';'): c = c[1:-1]
                seen.add(c)
            for k, o, nm, ds in members:
                seen.update(desc_classes(ds))
            for s in utf8:
                if s.startswith('(') or s.startswith('L'): seen.update(desc_classes(s))
            for c in seen:
                if c in own: continue
                refs[c] += 1
            for a in LIFECYCLE + ['Lcpw/mods/fml/common/Mod;', 'Lcpw/mods/fml/common/network/NetworkMod;', 'Lnet/minecraftforge/event/ForgeSubscribe;',
                                  'Lcpw/mods/fml/common/Mod$EventHandler;']:
                if a in utf8: ann[a] += 1
            # ModLoader style: extends BaseMod, or a class named mod_*
            if sup in ('net/minecraft/src/BaseMod', 'BaseMod'): flags['extends BaseMod'] += 1
            if this.rsplit('/', 1)[-1].startswith('mod_'): flags['mod_* class'] += 1
            if 'net/minecraft/src/ModLoader' in classes or 'ModLoader' in classes: flags['uses ModLoader'] += 1
            if any(c.startswith('net/minecraft/src/') and c.rsplit('/', 1)[-1] not in ML_FLAT for c in classes): flags['flat vanilla refs (class files)'] += 1
            if any('/' not in c and c in data.notch_class and c not in own for c in classes): flags['notch-named vanilla refs (class files)'] += 1
            if 'cpw/mods/fml/common/Mod' in classes: flags['@Mod'] += 1
            for s in strings:
                for needle, label in STRING_STYLES:
                    if label and s.startswith(needle) or (label and needle == '.png' and s.endswith('.png')):
                        strings_seen[label] += 1
                        break
            for u in utf8:
                if u in TEXTURE_MEMBERS and TEXTURE_MEMBERS[u] and u in ('setTextureFile', 'getTextureFile', 'preloadTexture', 'addOverride', 'registerTextureFX', 'addTextureFX'):
                    texture[TEXTURE_MEMBERS[u]] += 1
            for kind, o, nm, ds in members:
                if o in own: continue
                mcp_o = data.mcp_class(o) or o
                simple_o = mcp_o.rsplit('/', 1)[-1]
                is_mc = mcp_o.startswith('net/minecraft/') or ('/' not in o and o in data.notch_class)
                if not is_mc and not o.startswith(('net/minecraftforge/', 'cpw/mods/fml/')):
                    continue
                mn = data.member_name(o, nm, ds, kind == 9)
                if kind == 9 and mn in NUMERIC_FIELDS: numeric[NUMERIC_FIELDS[mn]] += 1
                elif kind in (10, 11) and mn in NUMERIC_METHODS and simple_o in ('World', 'IBlockAccess', 'Chunk'): numeric[NUMERIC_METHODS[mn]] += 1
                elif nm == '<init>' and ds.startswith('(I') and simple_o in CTOR_OWNERS: numeric['new %s(int ...)' % simple_o] += 1
                elif nm == '<init>' and ds.startswith('(III') and simple_o == 'ItemStack': numeric['new ItemStack(int,int,int)'] += 1
                elif kind == 9 and mn in ('blockIndexInTexture', 'iconIndex'): texture[TEXTURE_MEMBERS[mn]] += 1
                elif kind in (10, 11) and mn in TEXTURE_MEMBERS and TEXTURE_MEMBERS[mn]: texture[TEXTURE_MEMBERS[mn]] += 1
                if kind in (10, 11) and mn == 'getBlockTexture' : pass
        for c in refs:
            m = data.mcp_class(c) or c
            if m in NET_CLASSES:
                net[m] += 1
        per[j] = dict(classes=nclass, refs=refs, numeric=numeric, ann=ann, texture=texture, strings=strings_seen, flags=flags, net=net)
    return per


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('jardir')
    ap.add_argument('--mc', default='1.4.7', choices=['1.4.7', '1.5.2'])
    ap.add_argument('--json')
    ap.add_argument('--md')
    ap.add_argument('--top', type=int, default=40)
    a = ap.parse_args()
    data = Data(a.mc)
    per = scan(a.jardir, data)
    bk = {}
    users = collections.defaultdict(set)
    uses = collections.Counter()
    hints = {}
    for j, p in per.items():
        for c, n in p['refs'].items():
            b = data.bucket(c)
            if not b: continue
            name = data.mcp_class(c) or c if '/' not in c else c
            bk[name] = b[0]; hints[name] = b[1]
            users[name].add(j); uses[name] += n
    emit(a, data, per, bk, users, uses, hints)


def jar_info(data, p):
    c = collections.Counter()
    for r, n in p['refs'].items():
        b = data.bucket(r)
        if b: c[b[0]] += 1
    return c


def emit(a, data, per, bk, users, uses, hints):
    L = []; P = L.append
    nj = len(per)
    legacy = {j: p for j, p in per.items() if p['flags'] or any(k in p['ann'] for k in p['ann'])}
    P('Jars scanned: %d (%s), hand-written shim classes (1.7.10 + id facade): %d' % (nj, a.mc, len(data.shim)))
    P('')
    P('### Distinct referenced classes per bucket')
    P('')
    P('| Bucket | Distinct classes | Class-file references |')
    P('|---|---:|---:|')
    for b in ('present', 'renamed', 'synth', 'removed', 'modloader', 'unknown', 'obfuscated?'):
        P('| %s | %d | %d |' % (b, sum(1 for c in bk if bk[c] == b), sum(v for c, v in uses.items() if bk[c] == b)))
    P('')
    P('### Per jar')
    P('')
    P('| Jar | Classes | naming | present | renamed | synth | removed | modloader | unknown | numeric-id refs | texture refs | old net classes |')
    P('|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---|')
    for j, p in sorted(per.items()):
        d = jar_info(data, p)
        fl = p['flags']
        naming = 'flat' if fl['flat vanilla refs (class files)'] else ('notch' if fl['notch-named vanilla refs (class files)'] else '-')
        P('| %s | %d | %s | %d | %d | %d | %d | %d | %d | %d | %d | %s |' % (
            j, p['classes'], naming, d['present'], d['renamed'], d['synth'], d['removed'], d['modloader'], d['unknown'],
            sum(p['numeric'].values()), sum(p['texture'].values()), ', '.join(sorted(n.rsplit('/', 1)[-1] for n in p['net'])) or '-'))
    P('')

    def count(title, pred):
        k = sum(1 for p in per.values() if pred(p))
        P('- %s: %d of %d jars (%d%%)' % (title, k, nj, 100 * k // max(nj, 1)))
    count('notch (obfuscated) vanilla names', lambda p: p['flags']['notch-named vanilla refs (class files)'] > 0)
    count('flat net.minecraft.src vanilla names', lambda p: p['flags']['flat vanilla refs (class files)'] > 0)
    count('ModLoader style (`extends BaseMod`)', lambda p: p['flags']['extends BaseMod'] > 0)
    count('uses the ModLoader class (alone or next to `@Mod`)', lambda p: p['flags']['uses ModLoader'] > 0)
    count('`@Mod` (FML)', lambda p: p['ann']['Lcpw/mods/fml/common/Mod;'] > 0)
    count('both `@Mod` and `extends BaseMod`', lambda p: p['ann']['Lcpw/mods/fml/common/Mod;'] > 0 and p['flags']['extends BaseMod'] > 0)
    count('only ModLoader (`extends BaseMod`, no `@Mod`)', lambda p: p['ann']['Lcpw/mods/fml/common/Mod;'] == 0 and p['flags']['extends BaseMod'] > 0)
    count('`@NetworkMod`', lambda p: p['ann']['Lcpw/mods/fml/common/network/NetworkMod;'] > 0)
    count('`@PreInit/@Init/@PostInit`', lambda p: any(p['ann'][k] for k in LIFECYCLE))
    count('`@ForgeSubscribe`', lambda p: p['ann']['Lnet/minecraftforge/event/ForgeSubscribe;'] > 0)
    count('numeric ids', lambda p: sum(p['numeric'].values()) > 0)
    count('Forge texture-file API (setTextureFile/getTextureFile/preloadTexture)', lambda p: sum(v for k, v in p['texture'].items() if 'TextureFile' in k or 'preload' in k) > 0)
    count('block/item index fields (blockIndexInTexture/iconIndex/setIconIndex)', lambda p: sum(v for k, v in p['texture'].items() if 'index' in k.lower() or 'Index' in k) > 0)
    count('texture FX / ModLoader.addOverride', lambda p: sum(v for k, v in p['texture'].items() if 'FX' in k or 'Override' in k) > 0)
    count('`terrain.png` or `gui/items.png` sheet strings', lambda p: p['strings']['terrain.png sheet'] + p['strings']['gui/items.png sheet'] > 0)
    count('`/mods/<id>/` texture paths', lambda p: p['strings']['/mods/<id>/ path'] > 0)
    P('')
    num = collections.Counter()
    for p in per.values():
        for k, v in p['numeric'].items(): num[k] += v
    P('### Numeric-id API references')
    P('')
    P('| Reference | Count | Jars |')
    P('|---|---:|---:|')
    for k, v in num.most_common():
        P('| %s | %d | %d |' % (k, v, sum(1 for p in per.values() if k in p['numeric'])))
    P('')
    tex = collections.Counter()
    for p in per.values():
        for k, v in p['texture'].items(): tex[k] += v
    P('### Texture API references (sprite sheets)')
    P('')
    P('| Reference | Count | Jars |')
    P('|---|---:|---:|')
    for k, v in tex.most_common():
        P('| %s | %d | %d |' % (k, v, sum(1 for p in per.values() if k in p['texture'])))
    P('')
    st = collections.Counter()
    for p in per.values():
        for k, v in p['strings'].items(): st[k] += v
    P('### Texture path strings')
    P('')
    P('| Style | Strings | Jars |')
    P('|---|---:|---:|')
    for k, v in st.most_common():
        P('| %s | %d | %d |' % (k, v, sum(1 for p in per.values() if k in p['strings'])))
    P('')
    for b in ('unknown', 'removed', 'synth', 'renamed'):
        P('### Top %d %s classes by number of jars using them' % (a.top, b))
        P('')
        P('| Class | Jars | Class files | Hint |')
        P('|---|---:|---:|---|')
        for c in sorted((c for c in bk if bk[c] == b), key=lambda c: (-len(users[c]), -uses[c]))[:a.top]:
            P('| `%s` | %d | %d | %s |' % (c, len(users[c]), uses[c], hints.get(c) or ''))
        P('')
    txt = '\n'.join(L)
    if a.json:
        json.dump({j: dict(classes=p['classes'], numeric=dict(p['numeric']), texture=dict(p['texture']), strings=dict(p['strings']),
                           flags=dict(p['flags']), ann=dict(p['ann']), buckets=dict(jar_info(data, p))) for j, p in per.items()}, open(a.json, 'w'), indent=1)
    if a.md:
        open(a.md, 'w', encoding='utf-8').write(txt + '\n')
    else:
        sys.stdout.buffer.write((txt + '\n').encode('utf-8'))


if __name__ == '__main__':
    main()
