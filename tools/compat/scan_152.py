#!/usr/bin/env python3
"""Scan a folder of Minecraft 1.5.2 Forge mod jars/zips and measure which Minecraft/Forge APIs they use.

    python tools/compat/scan_152.py <jar-dir> [--json out.json] [--md out.md] [--top 60]

The 1.6.4 scanner (scan_164.py) with the 1.5.2 differences applied on top of it:
  * the 1.5.2 lifecycle is @PreInit/@Init/@PostInit (there is no @EventHandler), so those count as the current form here;
  * EntityLiving is the base of every living entity (no EntityLivingBase) and there are no attributes, horses or ResourceLocation;
  * textures are string paths (RenderEngine.bindTexture(String), Texture, TextureStitched), the classes behind the 'texture'
    bucket (also reported separately below);
  * some Forge classes live in their 1.5 places (net.minecraftforge.common.ISidedInventory, IThrowableEntity, liquids).
Everything else (buckets, numeric ids, old network API) is the 1.6.4 logic: 1.5.2 forwards to the 1.6.4 facades.
"""
import collections, importlib.util, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location('scan_164', os.path.join(HERE, 'scan_164.py'))
s = importlib.util.module_from_spec(spec)
spec.loader.exec_module(s)

# 1.5.2-only classes and where 1.7.10 has them
s.RENAMED.update({
    'net/minecraft/client/renderer/RenderEngine': 'net/minecraft/client/renderer/texture/TextureManager (string paths)',
    'net/minecraft/client/renderer/texture/TextureStitched': 'net/minecraft/client/renderer/texture/TextureAtlasSprite',
    'net/minecraft/client/renderer/texture/Texture': 'removed (TextureAtlasSprite / DynamicTexture)',
    'net/minecraft/client/texturepacks/ITexturePack': 'removed (IResourcePack)',
    'net/minecraft/client/texturepacks/TexturePackList': 'removed (ResourcePackRepository)',
    'net/minecraftforge/common/ISidedInventory': 'net/minecraft/inventory/ISidedInventory',
    'net/minecraftforge/common/IThrowableEntity': 'removed (no 1.7.10 equivalent)',
    'net/minecraftforge/common/IArmorTextureProvider': 'removed (Item.getArmorTexture)',
    'net/minecraft/block/BlockCloth': 'net/minecraft/block/BlockColored',
    'net/minecraft/client/gui/GuiSmallButton': 'net/minecraft/client/gui/GuiOptionButton',
    'cpw/mods/fml/client/ITextureFX': 'removed (TextureAtlasSprite.updateAnimation)',
    'cpw/mods/fml/relauncher/RelaunchClassLoader': 'removed (LaunchClassLoader)',
    'cpw/mods/fml/relauncher/IClassTransformer': 'removed (coremod transformers are not executed)',
    'net/minecraftforge/client/event/TextureLoadEvent': 'removed',
})
# 1.5.2 lifecycle: the annotations ARE the current form
s.OLD_ANN = ['Lcpw/mods/fml/common/Mod$ServerStarting;', 'Lcpw/mods/fml/common/Mod$ServerStopping;']
s.NEW_ANN = ['Lcpw/mods/fml/common/Mod$PreInit;', 'Lcpw/mods/fml/common/Mod$Init;', 'Lcpw/mods/fml/common/Mod$PostInit;']

TEXTURE_CLASSES = ['net/minecraft/client/renderer/RenderEngine', 'net/minecraft/client/renderer/texture/TextureStitched',
                   'net/minecraft/client/renderer/texture/Texture', 'net/minecraft/client/texturepacks/ITexturePack',
                   'net/minecraft/client/texturepacks/TexturePackList', 'cpw/mods/fml/client/ITextureFX']
LIVING = 'net/minecraft/entity/EntityLiving'
LIVING_BASE = 'net/minecraft/entity/EntityLivingBase'

_orig_emit = s.emit


def emit(a, out, per, bk, users, uses):
    _orig_emit(a, out, per, bk, users, uses)
    nj = len(per)
    if a.md:  # the 1.6.4 labels name @EventHandler the current form; in 1.5.2 it is @PreInit/@Init/@PostInit
        text = open(a.md, encoding='utf-8').read()
        for old, new in (('old lifecycle annotations (@PreInit/@Init/@PostInit)', '@ServerStarting/@ServerStopping'),
                         ('only old lifecycle annotations (no @EventHandler)', 'only @ServerStarting/@ServerStopping'),
                         ('new lifecycle annotation (@EventHandler)', '@PreInit/@Init/@PostInit lifecycle'),
                         ('old lifecycle | new lifecycle', 'server hooks | @PreInit/@Init/@PostInit')):
            text = text.replace(old, new)
        open(a.md, 'w', encoding='utf-8').write(text)
    L = ['', '### 1.5.2 specifics', '']
    tex = sum(1 for p in per.values() if any(c in p['refs'] for c in TEXTURE_CLASSES))
    liv = sum(1 for p in per.values() if LIVING in p['refs'])
    base = sum(1 for p in per.values() if LIVING_BASE in p['refs'])
    L.append('- string-path texture API (RenderEngine/Texture/TextureStitched/ITexturePack): %d of %d jars' % (tex, nj))
    L.append('- `EntityLiving` referenced: %d of %d jars (class-file references: %d); `EntityLivingBase` (a 1.6-only class, so a mod that ships both versions): %d jars'
             % (liv, nj, sum(p['refs'].get(LIVING, 0) for p in per.values()), base))
    for c in TEXTURE_CLASSES:
        L.append('- `%s`: %d jars, %d class files' % (c, sum(1 for p in per.values() if c in p['refs']), sum(p['refs'].get(c, 0) for p in per.values())))
    txt = '\n'.join(L) + '\n'
    if a.md:
        open(a.md, 'a', encoding='utf-8').write(txt)
    else:
        sys.stdout.buffer.write(txt.encode('utf-8'))


s.emit = emit
s.TOP_JARS = ['buildcraft', 'forestry', 'twilight', 'mystcraft', 'appeng', 'factorization', 'binnie', 'computercraft', 'CustomNPCs', 'immibis-core']

if __name__ == '__main__':
    s.main()
