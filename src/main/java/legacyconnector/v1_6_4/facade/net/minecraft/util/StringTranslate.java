package legacyconnector.v1_6_4.facade.net.minecraft.util;

/** Facade of the 1.7.10 shim's {@code StringTranslate} for 1.5.2 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class StringTranslate {
    private StringTranslate() {}

    /** StringTranslate.currentLanguage: the language code the game is set to. */
    public static String mcc$get$currentLanguage(legacyconnector.v1_7_10.net.minecraft.util.StringTranslate self) {
        return self.getCurrentLanguage();
    }
}
