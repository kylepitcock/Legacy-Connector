package legacyconnector.v1_7_10.net.minecraft.client.resources;

import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

/** The language the game runs in, as legacy code asks for it (the modern game's current language). */
public class LanguageManager implements IResourceManagerReloadListener {
    /** The translations of the current language over English ones, as 1.7.10's I18n read them. */
    protected static final legacyconnector.v1_7_10.net.minecraft.client.resources.Locale currentLocale = new legacyconnector.v1_7_10.net.minecraft.client.resources.Locale();

    /** Reloads {@link #currentLocale}: English, then the current language over it. */
    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        java.util.List<String> languages = new java.util.ArrayList<>();
        languages.add("en_US");
        String current = getCurrentLanguage().getLanguageCode();
        if (!"en_US".equals(current)) languages.add(current);
        currentLocale.loadLocaleDataFiles(manager, languages);
    }

    public Language getCurrentLanguage() {
        String selected = "en_us";
        try {
            selected = net.minecraft.client.Minecraft.getInstance().getLanguageManager().getSelected();
        } catch (RuntimeException | LinkageError e) {
            // not on a client
        }
        String[] parts = selected.split("_", 2);
        String code = parts.length == 2 ? parts[0].toLowerCase(Locale.ROOT) + "_" + parts[1].toUpperCase(Locale.ROOT) : selected;
        return new Language(code, parts.length == 2 ? parts[1].toUpperCase(Locale.ROOT) : "", parts[0], false);
    }

    public void setCurrentLanguage(Language language) {
    }

    public boolean isCurrentLocaleUnicode() {
        return false;
    }

    public boolean isCurrentLanguageBidirectional() {
        return false;
    }

    public SortedSet<Language> getLanguages() {
        SortedSet<Language> set = new TreeSet<>();
        set.add(getCurrentLanguage());
        return set;
    }
}
