package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/**
 * Forge 1.5.2 named textures by string path ({@code RenderEngine.bindTexture("/mods/modid/textures/gui/x.png")}, {@code "/terrain.png"},
 * {@code "/gui/container.png"}, {@code "/mob/zombie.png"}); 1.6 introduced {@link ResourceLocation}. This resolves such a path to the location
 * the rest of the shim understands:
 * <ul>
 * <li>{@code domain:path} is a plain location;</li>
 * <li>{@code /mods/<id>/rest}: the mod's own file, which the loader also ships as {@code assets/<id>/rest} (the same file is reachable as a class path file);</li>
 * <li>the vanilla sheets of 1.5.2 ({@code /terrain.png}, {@code /gui/*.png}, {@code /mob/*.png}, {@code /item/*.png}, {@code /armor/*.png}, ...): the vanilla
 * texture of 1.7.10's layout, which {@code TextureLocations} then maps to 1.21.1's;</li>
 * <li>anything else starting with a slash: a class path file of a mod (the shim's {@code ClasspathTextures});</li>
 * <li>a relative path ({@code textures/blocks/x.png}) is taken as is.</li>
 * </ul>
 */
public final class Textures152 {
    private static final Map<String, String> VANILLA = Map.ofEntries(
            Map.entry("terrain.png", "textures/atlas/blocks.png"),
            Map.entry("gui/items.png", "textures/atlas/items.png"),
            Map.entry("gui/gui.png", "textures/gui/widgets.png"),
            Map.entry("gui/icons.png", "textures/gui/icons.png"),
            Map.entry("gui/container.png", "textures/gui/container/generic_54.png"),
            Map.entry("gui/inventory.png", "textures/gui/container/inventory.png"),
            Map.entry("gui/crafting.png", "textures/gui/container/crafting_table.png"),
            Map.entry("gui/furnace.png", "textures/gui/container/furnace.png"),
            Map.entry("gui/trap.png", "textures/gui/container/dispenser.png"),
            Map.entry("gui/allitems.png", "textures/gui/container/creative_inventory/tab_items.png"),
            Map.entry("gui/background.png", "textures/gui/options_background.png"),
            Map.entry("gui/slot.png", "textures/gui/container/inventory.png"),
            Map.entry("font/default.png", "textures/font/ascii.png"),
            Map.entry("particles.png", "textures/particle/particles.png"),
            Map.entry("mob/steve.png", "textures/entity/steve.png"),
            Map.entry("mob/char.png", "textures/entity/steve.png"),
            Map.entry("item/chest.png", "textures/entity/chest/normal.png"),
            Map.entry("item/largechest.png", "textures/entity/chest/normal_double.png"),
            Map.entry("item/enderchest.png", "textures/entity/chest/ender.png"),
            Map.entry("item/arrows.png", "textures/entity/arrow.png"),
            Map.entry("item/sign.png", "textures/entity/sign.png"),
            Map.entry("item/book.png", "textures/entity/enchanting_table_book.png"),
            Map.entry("misc/shadow.png", "textures/misc/shadow.png"),
            Map.entry("misc/mapbg.png", "textures/map/map_background.png"),
            Map.entry("art/kz.png", "textures/painting/paintings_kristoffer_zetterstrand.png"),
            Map.entry("mob/zombie.png", "textures/entity/zombie/zombie.png"),
            Map.entry("mob/creeper.png", "textures/entity/creeper/creeper.png"),
            Map.entry("mob/skeleton.png", "textures/entity/skeleton/skeleton.png"),
            Map.entry("mob/spider.png", "textures/entity/spider/spider.png"),
            Map.entry("mob/pig.png", "textures/entity/pig/pig.png"),
            Map.entry("mob/cow.png", "textures/entity/cow/cow.png"),
            Map.entry("mob/chicken.png", "textures/entity/chicken.png"),
            Map.entry("mob/sheep.png", "textures/entity/sheep/sheep.png"),
            Map.entry("mob/enderman.png", "textures/entity/enderman/enderman.png"),
            Map.entry("mob/villager/villager.png", "textures/entity/villager/villager.png"),
            Map.entry("mob/wolf.png", "textures/entity/wolf/wolf.png"),
            Map.entry("mob/slime.png", "textures/entity/slime/slime.png"),
            Map.entry("mob/ghast.png", "textures/entity/ghast/ghast.png"),
            Map.entry("mob/blaze.png", "textures/entity/blaze.png"),
            Map.entry("mob/bat.png", "textures/entity/bat.png"));

    private static final java.util.Map<String, Boolean> MISSING = new java.util.concurrent.ConcurrentHashMap<>();

    private Textures152() {}

    /** Whether no resource pack has the asset; only a definite answer (the resource manager is up and has the pack) counts. */
    private static boolean assetMissing(ResourceLocation asset) {
        var key = asset.toString();
        Boolean known = MISSING.get(key);
        if (known != null) return known;
        // modern resource names are lower case: such a file cannot be an asset (and asking the pack logs an invalid path)
        if (!asset.getResourcePath().equals(asset.getResourcePath().toLowerCase(java.util.Locale.ROOT))
                || !asset.getResourceDomain().equals(asset.getResourceDomain().toLowerCase(java.util.Locale.ROOT))) {
            MISSING.put(key, true);
            return true;
        }
        try {
            if (org.kylepitcock.legacyconnector.core.pack.GeneratedPack.building()) return false;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.getResourceManager() == null) return false;
            var modern = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(asset.getResourceDomain(), asset.getResourcePath());
            boolean missing = mc.getResourceManager().getResource(modern).isEmpty();
            MISSING.put(key, missing);
            return missing;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    /** The location a 1.5.2 texture path names. */
    public static ResourceLocation location(String path) {
        if (path == null) return null;
        int colon = path.indexOf(':');
        if (colon > 0 && path.indexOf('/') != 0) return new ResourceLocation(path.substring(0, colon), path.substring(colon + 1));
        String p = path.replace('\\', '/');
        boolean slash = p.startsWith("/");
        String bare = slash ? p.substring(1) : p;
        if (bare.startsWith("mods/")) {
            int next = bare.indexOf('/', "mods/".length());
            if (next > 0) {
                var asset = new ResourceLocation(bare.substring("mods/".length(), next), bare.substring(next + 1));
                // The first segment is the mod id by convention, but a mod may use its package ("/mods/immibis/chunkloader/..." of a mod
                // whose id is "dimensionalanchors"): no such asset, so it is a class path file like any other path.
                return assetMissing(asset) ? new ResourceLocation("minecraft", "/" + bare) : asset;
            }
        }
        String vanilla = VANILLA.get(bare);
        if (vanilla != null) return new ResourceLocation("minecraft", vanilla);
        if (bare.startsWith("armor/")) return new ResourceLocation("minecraft", "textures/models/armor/" + bare.substring(6).replace("_1.png", "_layer_1.png").replace("_2.png", "_layer_2.png"));
        if (bare.startsWith("gui/") || bare.startsWith("misc/") || bare.startsWith("environment/")) return new ResourceLocation("minecraft", "textures/" + bare);
        if (bare.startsWith("mob/") || bare.startsWith("item/")) return new ResourceLocation("minecraft", "textures/entity/" + bare.substring(bare.indexOf('/') + 1));
        if (bare.startsWith("textures/")) return new ResourceLocation("minecraft", bare);
        // a class path file of a mod (a path with a leading slash is not an asset): the shim reads it from the mods' class path
        return new ResourceLocation("minecraft", slash ? p : "/" + p);
    }
}
