package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import net.minecraft.locale.Language;
import org.kylepitcock.legacyconnector.core.pack.GeneratedTranslations;

/**
 * Legacy translation keys the modern language does not hold, resolved as 1.7.10 resolved them (asked by the client language on a
 * miss, see {@code ClientLanguageLegacyKeysMixin}): keys mods register from code at any time (LanguageRegistry, StringTranslate),
 * also after the generated resource pack was built; the en_US lang files of the legacy jars; and 1.7.10 vanilla keys
 * ({@code tile.stone.name}) through their modern counterparts. So a {@code Component.translatable} of a legacy key (item and block
 * names, Jade's tooltip) shows what 1.7.10 showed.
 */
public final class LegacyLang {
    private LegacyLang() {
    }

    /** The text of {@code key}, or null when no legacy source has it. O(1): a few map lookups. */
    public static String lookup(Language lang, String key) {
        if (key == null) return null;
        String registered = GeneratedTranslations.get("en_US", key);
        if (registered != null) return registered;
        String modern = legacyconnector.v1_7_10.net.minecraft.util.StatCollector.mcc$modernKey(key);
        if (modern == null || modern.equals(key) || !lang.has(modern)) return null;
        return lang.getOrDefault(modern, key);
    }
}
