#!/usr/bin/env python3
"""Scan a folder of Minecraft 1.6.4 Forge mod jars and measure which Minecraft/Forge APIs they use.

    python tools/compat/scan_164.py <jar-dir> [--json out.json] [--md out.md] [--top 60]

Reads every .class of every .jar/.zip with a small constant-pool parser (no JDK needed) and collects class references
(CONSTANT_Class and descriptors), member references with owners, and annotation types. Each referenced Minecraft/Forge
class is bucketed against the 1.7.10 shim source tree (src/main/java/legacyconnector/v1_7_10):

  present      the class exists in the shim at the same path
  renamed      listed in RENAMED (known 1.6.4 -> 1.7.10 rename or package move)
  moved        not at that path, but the same simple name exists elsewhere in the shim (probably a package move)
  obfuscated   default-package short name (notch name; the jar was not reobfuscated to MCP class names)
  missing      none of the above (removed, or only synthesized from the 1.7.10 jar by the loader at run time)

"present" is a lower bound for "will link": the loader also synthesizes API classes missing from the shim.
Plus the numeric-id API, the old network API and the old lifecycle annotations (see the sets below).
"""
import argparse, collections, json, os, re, struct, sys, zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SHIM = os.path.join(ROOT, 'src', 'main', 'java', 'legacyconnector', 'v1_7_10')

# 1.6.4 class -> 1.7.10 class (known renames and package moves)
RENAMED = {
    'net/minecraft/util/Icon': 'net/minecraft/util/IIcon',
    'net/minecraft/client/renderer/texture/IconRegister': 'net/minecraft/client/renderer/texture/IIconRegister',
    'net/minecraft/item/EnumToolMaterial': 'net/minecraft/item/Item$ToolMaterial',
    'net/minecraft/item/EnumArmorMaterial': 'net/minecraft/item/ItemArmor$ArmorMaterial',
    'net/minecraft/block/StepSound': 'net/minecraft/block/Block$SoundType',
    'net/minecraft/util/EnumMovingObjectType': 'net/minecraft/util/MovingObjectPosition$MovingObjectType',
    'net/minecraft/util/WeightedRandomItem': 'net/minecraft/util/WeightedRandom$Item',
    'net/minecraft/network/NetServerHandler': 'net/minecraft/network/NetHandlerPlayServer',
    'net/minecraft/network/NetLoginHandler': 'net/minecraft/network/NetHandlerLoginServer',
    'net/minecraft/network/INetworkManager': 'net/minecraft/network/NetworkManager',
    'net/minecraft/client/multiplayer/NetClientHandler': 'net/minecraft/client/network/NetHandlerPlayClient',
    'net/minecraft/util/StringTranslate': 'net/minecraft/util/StatCollector',
    'net/minecraft/client/renderer/entity/RenderEngine': 'net/minecraft/client/renderer/texture/TextureManager',
    'net/minecraft/client/renderer/RenderEngine': 'net/minecraft/client/renderer/texture/TextureManager',
    'net/minecraftforge/common/Configuration': 'net/minecraftforge/common/config/Configuration',
    'net/minecraftforge/common/Property': 'net/minecraftforge/common/config/Property',
    'net/minecraftforge/common/ConfigCategory': 'net/minecraftforge/common/config/ConfigCategory',
    'net/minecraftforge/common/ForgeDirection': 'net/minecraftforge/common/util/ForgeDirection',
    'net/minecraftforge/common/EnumHelper': 'net/minecraftforge/common/util/EnumHelper',
    'net/minecraftforge/event/ForgeSubscribe': 'cpw/mods/fml/common/eventhandler/SubscribeEvent',
    'net/minecraftforge/event/Event': 'cpw/mods/fml/common/eventhandler/Event',
    'net/minecraftforge/event/Cancelable': 'cpw/mods/fml/common/eventhandler/Cancelable',
    'net/minecraftforge/event/EventPriority': 'cpw/mods/fml/common/eventhandler/EventPriority',
    'net/minecraftforge/liquids/LiquidStack': 'net/minecraftforge/fluids/FluidStack',
    'net/minecraftforge/liquids/LiquidContainerRegistry': 'net/minecraftforge/fluids/FluidContainerRegistry',
    'net/minecraftforge/liquids/ILiquidTank': 'net/minecraftforge/fluids/IFluidTank',
    'net/minecraftforge/liquids/ITankContainer': 'net/minecraftforge/fluids/IFluidHandler',
    'net/minecraftforge/liquids/LiquidTank': 'net/minecraftforge/fluids/FluidTank',
    'net/minecraftforge/liquids/LiquidDictionary': 'net/minecraftforge/fluids/FluidRegistry',
    'net/minecraftforge/liquids/IBlockLiquid': 'net/minecraftforge/fluids/IFluidBlock',
    'cpw/mods/fml/common/registry/TickRegistry': 'removed (tick events in 1.7.10)',
    'cpw/mods/fml/common/ITickHandler': 'removed (tick events in 1.7.10)',
    'cpw/mods/fml/common/IScheduledTickHandler': 'removed (tick events in 1.7.10)',
    'cpw/mods/fml/common/TickType': 'removed (tick events in 1.7.10)',
    'cpw/mods/fml/common/IPlayerTracker': 'removed (PlayerEvent in 1.7.10)',
    'cpw/mods/fml/common/ICraftingHandler': 'removed (PlayerEvent.ItemCraftedEvent)',
    'cpw/mods/fml/common/IPickupNotifier': 'removed (PlayerEvent.ItemPickupEvent)',
    'cpw/mods/fml/common/network/IPacketHandler': 'removed (SimpleNetworkWrapper / FMLEventChannel)',
    'cpw/mods/fml/common/network/NetworkMod': 'removed (annotation)',
    'cpw/mods/fml/common/network/PacketDispatcher': 'removed',
    'cpw/mods/fml/common/network/Player': 'removed (EntityPlayer)',
    'cpw/mods/fml/common/network/IConnectionHandler': 'removed (FMLNetworkEvent)',
    'cpw/mods/fml/common/network/ITinyPacketHandler': 'removed',
    'net/minecraft/network/packet/Packet250CustomPayload': 'removed (FMLProxyPacket)',
    'net/minecraft/network/packet/Packet': 'removed (Packet in net/minecraft/network)',
    'net/minecraft/util/Vec3Pool': 'removed (Vec3.createVectorHelper)',
    'net/minecraft/util/AABBPool': 'removed (AxisAlignedBB.getBoundingBox is static)',
    'net/minecraft/util/ChatMessageComponent': 'net/minecraft/util/IChatComponent / ChatComponentText',
    'net/minecraft/block/BlockFluid': 'net/minecraft/block/BlockLiquid',
    'net/minecraft/block/BlockHalfSlab': 'net/minecraft/block/BlockSlab',
    'net/minecraft/network/packet/Packet132TileEntityData': 'net/minecraft/network/play/server/S35PacketUpdateTileEntity',
    'net/minecraft/network/packet/Packet131MapData': 'net/minecraft/network/play/server/S34PacketMaps',
    'net/minecraft/network/packet/Packet1Login': 'net/minecraft/network/play/server/S01PacketJoinGame',
    'net/minecraft/network/packet/Packet28EntityVelocity': 'net/minecraft/network/play/server/S12PacketEntityVelocity',
    'net/minecraft/network/packet/Packet51MapChunk': 'net/minecraft/network/play/server/S21PacketChunkData',
    'net/minecraft/network/packet/Packet100OpenWindow': 'net/minecraft/network/play/server/S2DPacketOpenWindow',
    'net/minecraft/network/packet/Packet70GameEvent': 'net/minecraft/network/play/server/S2BPacketChangeGameState',
    'net/minecraft/network/packet/Packet3Chat': 'net/minecraft/network/play/server/S02PacketChat',
    'net/minecraft/network/packet/NetHandler': 'net/minecraft/network/INetHandler',
    'cpw/mods/fml/client/registry/KeyBindingRegistry': 'removed (ClientRegistry.registerKeyBinding + KeyInputEvent)',
    'net/minecraft/entity/ai/attributes/Attribute': 'net/minecraft/entity/ai/attributes/IAttribute',
    'net/minecraft/entity/ai/attributes/AttributeInstance': 'net/minecraft/entity/ai/attributes/IAttributeInstance',
    'net/minecraft/client/resources/ResourceManager': 'net/minecraft/client/resources/IResourceManager',
    'net/minecraft/client/resources/Resource': 'net/minecraft/client/resources/IResource',
    'net/minecraft/client/resources/ResourcePack': 'net/minecraft/client/resources/IResourcePack',
    'net/minecraft/client/resources/ResourceManagerReloadListener': 'net/minecraft/client/resources/IResourceManagerReloadListener',
    'net/minecraft/client/resources/ReloadableResourceManager': 'net/minecraft/client/resources/IReloadableResourceManager',
    'net/minecraft/client/resources/data/MetadataSection': 'net/minecraft/client/resources/data/IMetadataSection',
    'net/minecraft/client/audio/SoundPool': 'removed (SoundHandler, sounds.json)',
    'net/minecraft/client/audio/SoundPoolEntry': 'removed (SoundHandler, sounds.json)',
    'net/minecraft/entity/EntityLivingData': 'net/minecraft/entity/IEntityLivingData',
    'net/minecraft/client/renderer/tileentity/TileEntityRenderer': 'net/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher',
    'net/minecraft/client/renderer/texture/TextureObject': 'net/minecraft/client/renderer/texture/ITextureObject',
    'cpw/mods/fml/common/network/NetworkModHandler': 'removed',
    'net/minecraft/world/gen/structure/ComponentVillageStartPiece': 'net/minecraft/world/gen/structure/StructureVillagePieces$Start',
    'net/minecraft/world/gen/structure/ComponentVillage': 'net/minecraft/world/gen/structure/StructureVillagePieces$Village',
    'net/minecraft/world/gen/structure/StructureVillagePieceWeight': 'net/minecraft/world/gen/structure/StructureVillagePieces$PieceWeight',
    'net/minecraft/world/gen/structure/StructurePieceBlockSelector': 'net/minecraft/world/gen/structure/StructureComponent$BlockSelector',
    'net/minecraft/entity/EntityEggInfo': 'net/minecraft/entity/EntityList$EntityEggInfo',
    'net/minecraft/entity/ai/EntityAITaskEntry': 'net/minecraft/entity/ai/EntityAITasks$EntityAITaskEntry',
    'net/minecraft/world/biome/SpawnListEntry': 'net/minecraft/world/biome/BiomeGenBase$SpawnListEntry',
    'net/minecraft/entity/item/EntityFallingSand': 'net/minecraft/entity/item/EntityFallingBlock',
    'net/minecraft/client/settings/EnumOptions': 'net/minecraft/client/settings/GameSettings$Options',
    'net/minecraft/tileentity/TileEntityRecordPlayer': 'net/minecraft/block/BlockJukebox$TileEntityJukebox',
    'net/minecraft/entity/EntityOwnable': 'net/minecraft/entity/IEntityOwnable',
    'net/minecraft/entity/player/EnumStatus': 'net/minecraft/entity/player/EntityPlayer$EnumStatus',
    'net/minecraftforge/client/event/sound/PlaySoundEvent': 'net/minecraftforge/client/event/sound/PlaySoundEvent17',
    'net/minecraftforge/client/event/sound/PlayStreamingEvent': 'net/minecraftforge/client/event/sound/PlayStreamingSourceEvent',
    'net/minecraft/server/management/PlayerInstance': 'net/minecraft/server/management/PlayerManager$PlayerInstance',
    'net/minecraft/block/BlockTripWireSource': 'net/minecraft/block/BlockTripWireHook',
    'net/minecraft/world/EnumGameType': 'net/minecraft/world/WorldSettings$GameType',
    'net/minecraft/world/storage/MapCoord': 'net/minecraft/world/storage/MapData$MapCoord',
    'net/minecraft/world/storage/MapInfo': 'net/minecraft/world/storage/MapData$MapInfo',
    'net/minecraft/util/EnumArt': 'net/minecraft/entity/item/EntityPainting$EnumArt',
    'net/minecraft/logging/ILogAgent': 'removed (log4j)',
    'net/minecraft/crash/CallableMinecraftVersion': 'removed (crash report internals)',
    'net/minecraft/src/ModLoader': 'removed (ModLoader base mods)',
    'cpw/mods/fml/common/registry/ItemData': 'removed',
    'net/minecraft/item/ItemMultiTextureTile': 'net/minecraft/item/ItemMultiTexture',
    'net/minecraft/client/gui/GuiSmallButton': 'net/minecraft/client/gui/GuiOptionButton',
    'net/minecraft/block/EnumMobType': 'removed',
    'net/minecraft/block/BlockJukeBox': 'net/minecraft/block/BlockJukebox',
}
REMOVED_PREFIX = ('net/minecraft/network/packet/Packet',)

NUMERIC_FIELD = {  # (MCP name, SRG name) -> label
    'blocksList': 'Block.blocksList', 'field_71973_m': 'Block.blocksList',
    'itemsList': 'Item.itemsList', 'field_77698_e': 'Item.itemsList',
    'blockID': 'blockID', 'field_71990_ca': 'blockID',
    'itemID': 'itemID', 'field_77779_bT': 'itemID', 'field_77993_c': 'itemID',
    'biomeList': 'BiomeGenBase.biomeList', 'field_76773_a': 'BiomeGenBase.biomeList',
    'enchantmentsList': 'Enchantment.enchantmentsList', 'field_77331_b': 'Enchantment.enchantmentsList',
    'potionTypes': 'Potion.potionTypes', 'field_76425_a': 'Potion.potionTypes',
    'effectId': 'effectId', 'biomeID': 'biomeID', 'field_76756_M': 'biomeID',
}
NUMERIC_METHOD = {
    'getBlockId': 'World.getBlockId', 'func_72798_a': 'World.getBlockId',
    'getBlockMetadata': None,
    'setBlock': 'World.setBlock(int ids)', 'func_94575_c': 'World.setBlock(int ids)', 'func_72832_d': 'World.setBlock(int ids)',
    'setBlockAndMetadataWithNotify': 'World.setBlock(int ids)', 'func_72822_b': 'World.setBlock(int ids)',
    'func_94577_B': None, 'getBlockId1': None,
    'func_72805_g': None,
    'func_71876_a': None,
}
NUMERIC_CTOR_OWNERS = re.compile(r'^net/minecraft/(block/Block|item/Item|item/ItemStack|enchantment/Enchantment|potion/Potion|world/biome/BiomeGenBase)')
NUMERIC_INIT_ARGS0 = '(I'

NET_CLASSES = ['cpw/mods/fml/common/network/NetworkMod', 'cpw/mods/fml/common/network/IPacketHandler',
               'net/minecraft/network/packet/Packet250CustomPayload', 'cpw/mods/fml/common/network/NetworkRegistry',
               'cpw/mods/fml/common/network/PacketDispatcher', 'cpw/mods/fml/common/network/IConnectionHandler',
               'cpw/mods/fml/common/network/ITinyPacketHandler', 'cpw/mods/fml/common/network/Player',
               'cpw/mods/fml/common/network/IGuiHandler', 'cpw/mods/fml/common/registry/TickRegistry',
               'cpw/mods/fml/common/ITickHandler', 'net/minecraft/network/packet/Packet']
OLD_ANN = ['Lcpw/mods/fml/common/Mod$PreInit;', 'Lcpw/mods/fml/common/Mod$Init;', 'Lcpw/mods/fml/common/Mod$PostInit;',
           'Lcpw/mods/fml/common/Mod$ServerStarting;', 'Lcpw/mods/fml/common/Mod$ServerStopping;', 
           'Lcpw/mods/fml/common/Mod$ServerStarted;', 'Lcpw/mods/fml/common/Mod$ServerAboutToStart;']
NEW_ANN = ['Lcpw/mods/fml/common/Mod$EventHandler;']
OLD_EVT = 'Lnet/minecraftforge/event/ForgeSubscribe;'
NEW_EVT = 'Lcpw/mods/fml/common/eventhandler/SubscribeEvent;'

TOP_JARS = ['industrialcraft', 'TConstruct', 'forestry', 'twilightforest', 'EnderIO', 'NotEnoughItems', 'witchery', 'mystcraft', 'JourneyMap', 'DrZharks']
MC_PREFIX = ('net/minecraft/', 'net/minecraftforge/', 'cpw/mods/fml/', 'paulscode/', 'org/lwjgl/')
MC_CORE = ('net/minecraft/', 'net/minecraftforge/', 'cpw/mods/fml/')
OBF = re.compile(r'^[a-z]{1,3}$')


def parse(data):
    """Return (classes, members, utf8) of one class file. members: (kind, owner, name, desc)."""
    if data[:4] != b'\xca\xfe\xba\xbe':
        return None
    n = struct.unpack('>H', data[8:10])[0]
    pos = 10
    pool = [None] * n
    i = 1
    while i < n:
        t = data[pos]
        pos += 1
        if t == 1:
            ln = struct.unpack('>H', data[pos:pos + 2])[0]
            pool[i] = ('u', data[pos + 2:pos + 2 + ln].decode('utf-8', 'replace'))
            pos += 2 + ln
        elif t in (3, 4):
            pos += 4
        elif t in (5, 6):
            pos += 8
            i += 1
        elif t == 7:
            pool[i] = ('c', struct.unpack('>H', data[pos:pos + 2])[0]); pos += 2
        elif t == 8 or t == 16 or t == 19 or t == 20:
            pos += 2
        elif t in (9, 10, 11):
            pool[i] = ('m', t, struct.unpack('>HH', data[pos:pos + 4])); pos += 4
        elif t == 12:
            pool[i] = ('nt', struct.unpack('>HH', data[pos:pos + 4])); pos += 4
        elif t in (15,):
            pos += 3
        elif t in (17, 18):
            pos += 4
        else:
            return None
        i += 1
    u = lambda k: pool[k][1]
    classes, members, utf8 = set(), [], set()
    for e in pool:
        if not e:
            continue
        if e[0] == 'u':
            utf8.add(e[1])
        elif e[0] == 'c':
            classes.add(u(e[1]))
        elif e[0] == 'm':
            ci, nti = e[2]
            owner = u(pool[ci][1])
            nm, ds = pool[nti][1]
            members.append((e[1], owner, u(nm), u(ds)))
    this = struct.unpack('>H', data[pos + 2:pos + 4])[0]
    sup = struct.unpack('>H', data[pos + 4:pos + 6])[0]
    return classes, members, utf8, u(pool[this][1]), (u(pool[sup][1]) if sup else None)


def desc_classes(desc):
    return re.findall(r'L([^;]+);', desc)


def load_shim():
    paths, simple = set(), collections.defaultdict(list)
    for dp, _, fs in os.walk(SHIM):
        for f in fs:
            if f.endswith('.java'):
                rel = os.path.relpath(os.path.join(dp, f), SHIM).replace(os.sep, '/')[:-5]
                paths.add(rel)
                simple[rel.rsplit('/', 1)[-1]].append(rel)
    return paths, simple


def entries(z, depth=0):
    """Yield (name, bytes) of the class files of an archive, including nested jars/zips (some zips ship the mod jar inside)."""
    import io
    for n in z.namelist():
        if n.endswith('.class'):
            yield n, z.read(n)
        elif depth < 2 and n.lower().endswith(('.jar', '.zip')):
            try:
                yield from entries(zipfile.ZipFile(io.BytesIO(z.read(n))), depth + 1)
            except Exception:
                pass


def scan(jardir):
    shim, simple = load_shim()
    jars = sorted(f for f in os.listdir(jardir) if f.lower().endswith(('.jar', '.zip', '.litemod')))
    per = {}
    for j in jars:
        try:
            z = zipfile.ZipFile(os.path.join(jardir, j))
        except Exception as e:
            print('skip', j, e, file=sys.stderr)
            continue
        files = list(entries(z))
        own = {n[:-6] for n, _ in files}
        refs = collections.Counter()   # class -> number of class files that mention it
        numeric = collections.Counter()
        net = collections.Counter()
        ann = collections.Counter()
        mcrefs_members = collections.Counter()
        nclass = 0
        for n, raw in files:
            nclass += 1
            try:
                r = parse(raw)
            except Exception:
                r = None
            if not r:
                continue
            classes, members, utf8, this, sup = r
            seen = set()
            for c in classes:
                c = c.lstrip('[')
                if c.startswith('L') and c.endswith(';'):
                    c = c[1:-1]
                seen.add(c)
            for k, o, nm, ds in members:
                seen.update(desc_classes(ds))
            for s in utf8:
                if s.startswith('(') or s.startswith('L'):
                    seen.update(desc_classes(s))
            for c in seen:
                if c in own:
                    continue
                refs[c] += 1
            for a in OLD_ANN + NEW_ANN + [OLD_EVT, NEW_EVT, 'Lcpw/mods/fml/common/network/NetworkMod;', 'Lcpw/mods/fml/common/Mod;']:
                if a in utf8:
                    ann[a] += 1
            for kind, o, nm, ds in members:
                if o in own:
                    continue
                if o.startswith(MC_CORE):
                    mcrefs_members[(o, nm)] += 1
                if kind == 9 and nm in NUMERIC_FIELD and (o.startswith(MC_CORE)):
                    numeric[NUMERIC_FIELD[nm]] += 1
                elif kind in (10, 11) and NUMERIC_METHOD.get(nm) and (o.startswith(MC_CORE)):
                    numeric[NUMERIC_METHOD[nm]] += 1
                elif nm == '<init>' and ds.startswith(NUMERIC_INIT_ARGS0) and NUMERIC_CTOR_OWNERS.match(o):
                    numeric['new %s(int ...)' % o.rsplit('/', 1)[-1]] += 1
                elif nm == '<init>' and ds.startswith('(III') and o.endswith('ItemStack'):
                    numeric['new ItemStack(int,int,int)'] += 1
            # a subclass of Block/Item calling super(int, ...) is the same ctor ref via owner = superclass (covered above)
        per[j] = dict(classes=nclass, refs=refs, numeric=numeric, ann=ann, members=mcrefs_members)
    return shim, simple, per


def bucket(c, shim, simple):
    base = c.split('$')[0]
    h = RENAMED.get(c) or RENAMED.get(base)
    if h:
        return 'removed' if h.startswith('removed') else 'renamed'
    if base in shim:
        return 'present'
    if '/' not in c:
        return 'obfuscated' if OBF.match(c) else None
    if not c.startswith(MC_CORE):
        return None
    sn = base.rsplit('/', 1)[-1]
    if c.startswith(REMOVED_PREFIX):
        return 'missing'
    if sn in simple:
        return 'moved'
    return 'missing'


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('jardir')
    ap.add_argument('--json')
    ap.add_argument('--md')
    ap.add_argument('--top', type=int, default=60)
    a = ap.parse_args()
    shim, simple, per = scan(a.jardir)
    tot = collections.Counter()           # bucket -> distinct classes
    users = collections.defaultdict(set)  # class -> jars
    uses = collections.Counter()          # class -> class files
    bk = {}
    for j, p in per.items():
        for c, n in p['refs'].items():
            b = bucket(c, shim, simple)
            if not b:
                continue
            bk[c] = b
            users[c].add(j)
            uses[c] += n
    for c, b in bk.items():
        tot[b] += 1
    out = dict(jars=len(per), shim_classes=len(shim), buckets=dict(tot), classes={}, per_jar={})
    for c, b in bk.items():
        out['classes'][c] = dict(bucket=b, jars=len(users[c]), uses=uses[c],
                                 hint=RENAMED.get(c.split('$')[0]) or (simple[c.split('$')[0].rsplit('/', 1)[-1]][:1] if b == 'moved' else None))
    for j, p in per.items():
        d = collections.Counter(bucket(c, shim, simple) for c in p['refs'] if bucket(c, shim, simple))
        out['per_jar'][j] = dict(classes=p['classes'], buckets=dict(d), numeric=dict(p['numeric']), ann={k: v for k, v in p['ann'].items()},
                                 net=sorted(c for c in p['refs'] if c in NET_CLASSES))
    if a.json:
        json.dump(out, open(a.json, 'w'), indent=1)
    emit(a, out, per, bk, users, uses)


def emit(a, out, per, bk, users, uses):
    L = []
    P = L.append
    nj = len(per)
    P('Jars scanned: %d, shim classes (1.7.10 tree): %d' % (nj, out['shim_classes']))
    P('')
    P('### Distinct referenced classes per bucket')
    P('')
    P('| Bucket | Distinct classes | Class-file references |')
    P('|---|---:|---:|')
    for b in ('present', 'renamed', 'removed', 'moved', 'missing', 'obfuscated'):
        P('| %s | %d | %d |' % (b, out['buckets'].get(b, 0), sum(v for c, v in uses.items() if bk[c] == b)))
    P('')
    P('### Per-jar share of Minecraft/Forge class references that are not present in the shim')
    P('')
    P('| Jar | Classes | present | renamed+removed | moved | missing | obfuscated | numeric-id refs | old net classes | old lifecycle | new lifecycle |')
    P('|---|---:|---:|---:|---:|---:|---:|---:|---|---|---|')
    for j, d in sorted(out['per_jar'].items()):
        bs = d['buckets']
        oa = sum(v for k, v in d['ann'].items() if k in OLD_ANN)
        na = sum(v for k, v in d['ann'].items() if k in NEW_ANN)
        P('| %s | %d | %d | %d+%d | %d | %d | %d | %d | %s | %s | %s |' % (
            j, d['classes'], bs.get('present', 0), bs.get('renamed', 0), bs.get('removed', 0), bs.get('moved', 0), bs.get('missing', 0), bs.get('obfuscated', 0),
            sum(d['numeric'].values()), ', '.join(n.rsplit('/', 1)[-1] for n in d['net']) or '-', 'yes' if oa else '-', 'yes' if na else '-'))
    P('')
    for title, pred in (('numeric ids', lambda d: sum(d['numeric'].values()) > 0),
                        ('old network API (Packet250CustomPayload / IPacketHandler / NetworkMod / PacketDispatcher)',
                         lambda d: any(n.split('/')[-1] in ('Packet250CustomPayload', 'IPacketHandler', 'PacketDispatcher') for n in d['net'])
                         or 'Lcpw/mods/fml/common/network/NetworkMod;' in d['ann']),
                        ('old lifecycle annotations (@PreInit/@Init/@PostInit)', lambda d: any(k in OLD_ANN for k in d['ann'])),
                        ('only old lifecycle annotations (no @EventHandler)', lambda d: any(k in OLD_ANN for k in d['ann']) and not any(k in NEW_ANN for k in d['ann'])),
                        ('new lifecycle annotation (@EventHandler)', lambda d: any(k in NEW_ANN for k in d['ann'])),
                        ('@ForgeSubscribe', lambda d: OLD_EVT in d['ann']),
                        ('@SubscribeEvent', lambda d: NEW_EVT in d['ann']),
                        ('obfuscated (notch) class names', lambda d: d['buckets'].get('obfuscated', 0) > 20),
                        ('a @Mod class at all', lambda d: 'Lcpw/mods/fml/common/Mod;' in d['ann'])):
        k = sum(1 for d in out['per_jar'].values() if pred(d))
        P('- %s: %d of %d jars (%d%%)' % (title, k, nj, 100 * k // max(nj, 1)))
    P('')
    num = collections.Counter()
    for d in out['per_jar'].values():
        for k, v in d['numeric'].items():
            num[k] += v
    P('### Numeric-id API references (all jars)')
    P('')
    P('| Reference | Count | Jars |')
    P('|---|---:|---:|')
    for k, v in num.most_common():
        P('| %s | %d | %d |' % (k, v, sum(1 for d in out['per_jar'].values() if k in d['numeric'])))
    P('')
    for b in ('missing', 'removed', 'renamed', 'moved'):
        P('### Top %d %s classes by number of jars using them' % (a.top, b))
        P('')
        P('| Class | Jars | Class files | Hint |')
        P('|---|---:|---:|---|')
        items = sorted((c for c in bk if bk[c] == b), key=lambda c: (-len(users[c]), -uses[c]))[:a.top]
        for c in items:
            P('| `%s` | %d | %d | %s |' % (c, len(users[c]), uses[c], out['classes'][c]['hint'] or ''))
        P('')
    P('### Blockers of the most-used jars (non-present classes by number of class files, then numeric-id references)')
    P('')
    for frag in TOP_JARS:
        jn = next((j for j in per if frag.lower() in j.lower()), None)
        if not jn:
            continue
        p = per[jn]
        bl = sorted(((n, c) for c, n in p['refs'].items() if bk.get(c) not in (None, 'present')), reverse=True)[:6]
        P('- **%s**: %s; numeric: %s' % (jn, ', '.join('`%s` (%s, %d)' % (c.rsplit('/', 1)[-1], bk[c], n) for n, c in bl) or 'none',
                                      ', '.join('%s %d' % kv for kv in p['numeric'].most_common(4)) or 'none'))
    P('')
    txt = '\n'.join(L)
    if a.md:
        open(a.md, 'w', encoding='utf-8').write(txt + '\n')
    else:
        sys.stdout.buffer.write((txt + '\n').encode('utf-8'))


if __name__ == '__main__':
    main()
