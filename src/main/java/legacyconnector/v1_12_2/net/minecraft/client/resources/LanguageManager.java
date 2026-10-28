package legacyconnector.v1_12_2.net.minecraft.client.resources;

/** The client's languages as a view over the modern language manager. */
public class LanguageManager implements IResourceManagerReloadListener {
    public static final LanguageManager mcc$INSTANCE = new LanguageManager();

    private static net.minecraft.client.resources.language.LanguageManager modern() {
        return net.minecraft.client.Minecraft.getInstance().getLanguageManager();
    }

    private static Language of(String code, net.minecraft.client.resources.language.LanguageInfo info) {
        if (info == null) return new Language(code, "", code, false);
        return new Language(code, info.region(), info.name(), info.bidirectional());
    }

    /**
     * The selected language. Mods load inside the modern client's constructor, before its language manager exists (1.12 created it
     * first; Immersive Engineering's and Tinkers' fonts ask during init): until then the language from the options, else en_us.
     */
    public Language getCurrentLanguage() {
        var m = modern();
        if (m == null) {
            var options = net.minecraft.client.Minecraft.getInstance().options;
            String code = options != null && options.languageCode != null ? options.languageCode : "en_us";
            return of(code, null);
        }
        String code = m.getSelected();
        return of(code, m.getLanguage(code));
    }

    public void setCurrentLanguage(Language language) {
        var m = modern();
        if (language != null && m != null) m.setSelected(language.getLanguageCode());
    }

    /** Whether the font has to use the unicode pages (the player forced it). */
    public boolean isCurrentLocaleUnicode() {
        return net.minecraft.client.Minecraft.getInstance().options.forceUnicodeFont().get();
    }

    public boolean isCurrentLanguageBidirectional() {
        return getCurrentLanguage().isBidirectional();
    }

    public java.util.SortedSet<Language> getLanguages() {
        java.util.SortedSet<Language> out = new java.util.TreeSet<>();
        modern().getLanguages().forEach((code, info) -> out.add(of(code, info)));
        return out;
    }

    public Language getLanguage(String code) {
        var info = modern().getLanguage(code);
        return info == null ? null : of(code, info);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
    }
}
