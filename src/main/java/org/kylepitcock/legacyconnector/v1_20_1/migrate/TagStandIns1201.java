package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

/**
 * Forge 1.20.1 convention tags that NeoForge 1.21.1 has no constant for (per-colour glass, heads, water biomes). The key keeps the {@code c:} convention
 * namespace; a pack that defines the tag fills it, otherwise it is an empty tag (a lookup finds nothing, nothing throws). Reached from the field table
 * {@code mc-fields.tsv} (rows with {@code none} as the new descriptor).
 */
public final class TagStandIns1201 {
    private TagStandIns1201() {}

    public static TagKey glassWhite() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "glass_blocks/white"));
    }

    public static TagKey glassOrange() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "glass_blocks/orange"));
    }

    public static TagKey glassMagenta() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "glass_blocks/magenta"));
    }

    public static TagKey glassLightBlue() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "glass_blocks/light_blue"));
    }

    public static TagKey glassYellow() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "glass_blocks/yellow"));
    }

    public static TagKey glassLime() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "glass_blocks/lime"));
    }

    public static TagKey glassPink() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "glass_blocks/pink"));
    }

    public static TagKey glassGray() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "glass_blocks/gray"));
    }

    public static TagKey glassLightGray() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "glass_blocks/light_gray"));
    }

    public static TagKey glassCyan() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "glass_blocks/cyan"));
    }

    public static TagKey glassPurple() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "glass_blocks/purple"));
    }

    public static TagKey glassBlue() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "glass_blocks/blue"));
    }

    public static TagKey glassBrown() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "glass_blocks/brown"));
    }

    public static TagKey glassGreen() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "glass_blocks/green"));
    }

    public static TagKey glassRed() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "glass_blocks/red"));
    }

    public static TagKey glassBlack() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "glass_blocks/black"));
    }

    public static TagKey stainedGlass() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "glass_blocks/stained"));
    }

    public static TagKey heads() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "heads"));
    }

    /**
     * The field-by-name rows ({@code none-name} in {@code mc-fields.tsv}): the field name of Forge's {@code Tags.Items/Blocks/Biomes} constant that
     * 1.21.1 lacks, as the {@code c:} convention tag the loader's data conversion spells the same Forge tag as. The tags of the colours, panes, quartz
     * blocks and armour pieces are filled by the layer's own data ({@code data/c/tags}), the rest stay empty unless a pack defines them.
     */
    public static TagKey itemTag(String field) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path(field)));
    }

    public static TagKey blockTag(String field) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", path(field)));
    }

    public static TagKey biomeTag(String field) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", path(field)));
    }

    static String path(String field) {
        String lower = field.toLowerCase(java.util.Locale.ROOT);
        if (field.equals("STAINED_GLASS")) return "glass_blocks/stained";
        if (field.equals("STAINED_GLASS_PANES")) return "glass_panes/stained";
        if (field.startsWith("GLASS_PANES_")) return "glass_panes/" + lower.substring("glass_panes_".length());
        if (field.startsWith("GLASS_")) return "glass_blocks/" + lower.substring("glass_".length());
        if (field.startsWith("STORAGE_BLOCKS_")) return "storage_blocks/" + lower.substring("storage_blocks_".length());
        if (field.startsWith("ARMORS_")) return "armors/" + lower.substring("armors_".length());
        return lower;
    }

    public static TagKey isWater() {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("c", "is_water"));
    }
}
