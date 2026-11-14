package org.kylepitcock.legacyconnector.api;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.api.internal.TabViews;

/**
 * The modern creative tab of a legacy one (added in API 0.3). Look the tab up in
 * {@code BuiltInRegistries.CREATIVE_MODE_TAB} by the returned id (the tabs exist from the end of legacy loading on).
 */
public final class LegacyTabs {
    private LegacyTabs() {}

    /**
     * The id of the modern tab made for a legacy {@code CreativeTabs} object (1.4.7 to 1.7.10 and 1.12.2). Empty for vanilla tabs, for
     * tabs not registered yet (they are registered when legacy loading ends) and for objects that are no tab. For a Forge 1.20.1 mod the tab
     * is the modern {@code CreativeModeTab} itself, and its registry id is returned.
     */
    public static Optional<ResourceLocation> modernTab(Object legacyTab) {
        return TabViews.modernTab(legacyTab);
    }

    /**
     * The ids of the modern tabs a legacy mod's content is shown in: 1.4.7 to 1.7.10 the tabs its own tab objects became; Beta 1.7.3 the
     * one tab made for the mod (Beta had no tabs); 1.12.2 and 1.20.1: empty (use {@link #modernTab(Object)} with the tab object).
     */
    public static List<ResourceLocation> modernTabs(LegacyMod mod) {
        return TabViews.modernTabs(mod);
    }
}
