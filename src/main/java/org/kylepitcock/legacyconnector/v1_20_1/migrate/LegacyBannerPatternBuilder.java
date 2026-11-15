package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;

/**
 * Stands in for {@code BannerPattern.Builder} (1.20.1): collects banner layers by pattern key and dye and hands them out as the legacy {@code Patterns} list
 * ({@code Pattern} = the pattern key, {@code Color} = the dye id). 1.21.1 has {@code BannerPatternLayers.Builder} over holders, which needs a registry.
 */
public class LegacyBannerPatternBuilder {
    private final ListTag patterns = new ListTag();

    public LegacyBannerPatternBuilder addPattern(ResourceKey<BannerPattern> pattern, DyeColor color) {
        CompoundTag tag = new CompoundTag();
        tag.put("Pattern", StringTag.valueOf(pattern.location().toString()));
        tag.putInt("Color", color.getId());
        patterns.add(tag);
        return this;
    }

    public LegacyBannerPatternBuilder addPattern(net.minecraft.core.Holder<BannerPattern> pattern, DyeColor color) {
        return addPattern(pattern.unwrapKey().orElseThrow(), color);
    }

    public ListTag toListTag() {
        return patterns.copy();
    }
}
