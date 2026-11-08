package legacyconnector.v1_20_1.net.minecraftforge.common;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;

/** Forge 1.20.1's translation helper (the loading screen's language lookups). */
public final class ForgeI18n {
    private ForgeI18n() {}

    public static String getPattern(String translationKey) {
        return Language.getInstance().getOrDefault(translationKey);
    }

    public static String parseMessage(String i18nMessage, Object... args) {
        String pattern = getPattern(i18nMessage);
        try {
            return args.length == 0 ? pattern : String.format(pattern, args);
        } catch (java.util.IllegalFormatException e) {
            return pattern;
        }
    }

    public static String parseFormat(String format, Object... args) {
        try {
            return String.format(format, args);
        } catch (java.util.IllegalFormatException e) {
            return format;
        }
    }

    public static String stripControlCodes(String value) {
        return net.minecraft.ChatFormatting.stripFormatting(value);
    }

    public static void loadLanguageData() {
    }
}
