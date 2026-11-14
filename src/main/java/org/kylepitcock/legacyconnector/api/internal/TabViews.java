package org.kylepitcock.legacyconnector.api.internal;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.kylepitcock.legacyconnector.api.LegacyMod;
import org.kylepitcock.legacyconnector.api.LegacyVersion;

/** Legacy creative tab to modern tab id, per version family. */
public final class TabViews {
    private TabViews() {}

    public static Optional<ResourceLocation> modernTab(Object legacyTab) {
        if (legacyTab == null) return Optional.empty();
        if (legacyTab instanceof legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs t) {
            return Optional.ofNullable(org.kylepitcock.legacyconnector.v1_7_10.bridge.CreativeTabBridge.modernTabId(t));
        }
        if (legacyTab instanceof legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs t) {
            return Optional.ofNullable(org.kylepitcock.legacyconnector.v1_12_2.bridge.CreativeTabBridge1122.modernTabId(t));
        }
        if (legacyTab instanceof CreativeModeTab t) return Optional.ofNullable(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(t));
        return Optional.empty();
    }

    public static List<ResourceLocation> modernTabs(LegacyMod mod) {
        if (mod == null) return List.of();
        LegacyVersion v = mod.minecraftVersion();
        if (v.isBeta()) {
            ResourceLocation id = org.kylepitcock.legacyconnector.vb1_7_3.CreativeTabsB173.tabOfMod(mod.id());
            return id == null ? List.of() : List.of(id);
        }
        if (v.family() == LegacyVersion.V1_7_10) return org.kylepitcock.legacyconnector.v1_7_10.bridge.CreativeTabBridge.modernTabIdsOf(mod.id());
        return List.of();
    }
}
