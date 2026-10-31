package org.kylepitcock.legacyconnector.core.event;

import java.util.Set;

/**
 * Mod-specific: legacy event handlers that are not registered. Each replaces a vanilla HUD element with its own drawing that does not
 * show on the modern HUD, so the vanilla element is left to draw instead.
 */
final class SkippedHandlers {
    private static final Set<String> SKIPPED = Set.of(
            // Tinkers' Mantle (1.12.2): cancels the vanilla hearts and draws extra heart rows from icons.png; nothing showed
            "slimeknights.mantle.client.ExtraHeartRenderHandler#*",
            // Tinkers' Construct (1.7.10): the same for its heart rows
            "tconstruct.armor.ArmorProxyClient#renderHealthbar");

    private SkippedHandlers() {}

    /** Handlers the user switched off ({@code events.skip} in config/legacyconnector.properties). */
    private static volatile Set<String> configured;

    static boolean skip(String owner, String method) {
        Set<String> c = configured;
        if (c == null) configured = c = org.kylepitcock.legacyconnector.core.LegacyConfig.skippedEventHandlers();
        if (c.contains(owner + "#*") || c.contains(owner + "#" + method)) return true;
        return SKIPPED.contains(owner + "#*") || SKIPPED.contains(owner + "#" + method);
    }
}
