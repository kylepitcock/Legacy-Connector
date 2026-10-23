package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.IdentityHashMap;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Content that old mods added because 1.7.10 lacked it but modern vanilla has (Chisel's andesite, diorite and granite).
 * A mod block registered under one of these names stands for the vanilla block in its plain form (metadata 0): placing,
 * world generation, drops and recipe ingredients/results use the vanilla block and item, so recipes fuse with vanilla.
 * Its other metadata values (polished, bricks, ...) stay the mod's own.
 */
public final class VanillaEquivalents {
    private VanillaEquivalents() {}

    /** Block registry name (without domain) -> modern vanilla block id. */
    private static final Map<String, String> BY_NAME = Map.of(
            "andesite", "minecraft:andesite",
            "diorite", "minecraft:diorite",
            "granite", "minecraft:granite");

    private static final Map<Block, net.minecraft.world.level.block.Block> BLOCKS = new IdentityHashMap<>();

    /** Called when a mod registers a block; {@code legacyName} is "domain:name". */
    static synchronized void consider(Block block, String legacyName) {
        int colon = legacyName.indexOf(':');
        if (colon < 0 || legacyName.startsWith("minecraft:")) return;
        String vanilla = BY_NAME.get(legacyName.substring(colon + 1).toLowerCase(java.util.Locale.ROOT));
        if (vanilla == null) return;
        var modern = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(vanilla));
        if (modern != null) BLOCKS.put(block, modern);
    }

    /** The vanilla block a plain (metadata 0) mod block stands for, or null. */
    public static synchronized net.minecraft.world.level.block.Block block(Block block, int meta) {
        return meta == 0 && !BLOCKS.isEmpty() ? BLOCKS.get(block) : null;
    }

    public static Item item(Block block, int meta) {
        var modern = block(block, meta);
        return modern == null ? null : modern.asItem();
    }
}
