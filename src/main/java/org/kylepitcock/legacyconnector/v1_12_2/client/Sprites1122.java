package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.core.Ids;

/**
 * Names of 1.12.2 atlas sprites and textures in the modern game. A mod's sprite keeps its name ("mod:blocks/ore": the loader adds the
 * legacy blocks/ and items/ folders to the block atlas); vanilla's moved ("minecraft:blocks/stone" is "minecraft:block/stone").
 * Sprites a mod registers in other folders are added to the block atlas by {@link #contribute}.
 */
public final class Sprites1122 {
    /** Sprites registered through TextureMap.registerSprite outside the folders the atlas already scans. */
    private static final Set<ResourceLocation> EXTRA = ConcurrentHashMap.newKeySet();
    private static final Map<String, ResourceLocation> NAMES = new ConcurrentHashMap<>();

    private Sprites1122() {}

    /** The modern sprite id of a legacy sprite name ("mod:blocks/x", "minecraft:items/apple", "x" = minecraft:x). */
    public static ResourceLocation modernName(String legacy) {
        if (legacy == null) return ResourceLocation.withDefaultNamespace("missingno");
        return NAMES.computeIfAbsent(legacy, Sprites1122::computeName);
    }

    private static ResourceLocation computeName(String legacy) {
        int colon = legacy.indexOf(':');
        String ns = colon < 0 ? "minecraft" : Ids.namespace(legacy.substring(0, colon));
        String path = colon < 0 ? legacy : legacy.substring(colon + 1);
        if (path.endsWith(".png")) path = path.substring(0, path.length() - 4);
        if (path.startsWith("textures/")) path = path.substring("textures/".length());
        if (ns.equals("minecraft")) {
            if (path.equals("missingno")) return ResourceLocation.withDefaultNamespace("missingno");
            // already a modern name (a legacy sprite made for a modern one)
            if (path.startsWith("block/") || path.startsWith("item/") || path.startsWith("entity/")) return ResourceLocation.withDefaultNamespace(path);
            String modern = org.kylepitcock.legacyconnector.v1_7_10.Assets1710.texture(path, !path.startsWith("items/"));
            int c = modern.indexOf(':');
            return ResourceLocation.fromNamespaceAndPath(modern.substring(0, c), modern.substring(c + 1));
        }
        return ResourceLocation.fromNamespaceAndPath(ns, Ids.normalizePath(path));
    }

    /** The block atlas, or null before the client has one. */
    public static TextureAtlas blockAtlas() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getModelManager() == null) return null;
            return mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** The modern sprite of a legacy name (the missing sprite when the atlas has none of that name), or null with no atlas yet. */
    public static TextureAtlasSprite sprite(String legacy) {
        TextureAtlas atlas = blockAtlas();
        if (atlas == null) return null;
        try {
            return atlas.getSprite(modernName(legacy));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Remembers a sprite a mod registered, so the next atlas build includes it. */
    public static void register(String legacy) {
        ResourceLocation id = modernName(legacy);
        String p = id.getPath();
        if (p.startsWith("blocks/") || p.startsWith("items/") || p.startsWith("block/") || p.startsWith("item/") || p.equals("missingno")) return;
        EXTRA.add(id);
    }

    /** Atlas sources for the registered sprites in other folders ({@code single}: the sprite named like its texture file). */
    public static void contribute(Map<String, java.util.function.Supplier<byte[]>> out) {
        if (EXTRA.isEmpty()) return;
        com.google.gson.JsonArray sources = new com.google.gson.JsonArray();
        for (ResourceLocation id : EXTRA) {
            com.google.gson.JsonObject single = new com.google.gson.JsonObject();
            single.addProperty("type", "single");
            single.addProperty("resource", id.toString());
            sources.add(single);
        }
        com.google.gson.JsonObject atlas = new com.google.gson.JsonObject();
        atlas.add("sources", sources);
        byte[] bytes = atlas.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        out.put("assets/minecraft/atlases/blocks.json", () -> bytes);
    }
}
