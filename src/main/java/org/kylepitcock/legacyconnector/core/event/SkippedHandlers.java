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

    static boolean skip(String owner, String method) {
        return SKIPPED.contains(owner + "#*") || SKIPPED.contains(owner + "#" + method);
    }
}
