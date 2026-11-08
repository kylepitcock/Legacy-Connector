package legacyconnector.v1_12_2.net.minecraftforge.client.settings;

/** Forge's key modifiers (Ctrl, Shift, Alt, none), mapped onto NeoForge's identical ones. Key codes are LWJGL 2 ones. */
public enum KeyModifier {
    CONTROL(29, 157, 219, 220),
    SHIFT(42, 54),
    ALT(56, 184),
    NONE();

    public static final KeyModifier[] MODIFIER_VALUES = {SHIFT, CONTROL, ALT};

    private final int[] keys;

    KeyModifier(int... keys) {
        this.keys = keys;
    }

    /** NeoForge's modifier of the same name; resolved on use, because that class is client-only and a dedicated server may still load this one (a mod's key tracker). */
    public net.neoforged.neoforge.client.settings.KeyModifier mcc$modern() {
        return net.neoforged.neoforge.client.settings.KeyModifier.valueOf(name());
    }

    public static KeyModifier mcc$of(net.neoforged.neoforge.client.settings.KeyModifier modern) {
        for (KeyModifier k : values()) if (k.name().equals(modern.name())) return k;
        return NONE;
    }

    public static KeyModifier getActiveModifier() {
        for (KeyModifier m : MODIFIER_VALUES) if (m.isActive(null)) return m;
        return NONE;
    }

    public static boolean isKeyCodeModifier(int keyCode) {
        for (KeyModifier m : MODIFIER_VALUES) if (m.matches(keyCode)) return true;
        return false;
    }

    public static KeyModifier valueFromString(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return NONE;
        }
    }

    public boolean matches(int keyCode) {
        for (int k : keys) if (k == keyCode) return true;
        return false;
    }

    /** Whether the modifier is held now, in any context. */
    public boolean isActive() {
        return isActive(null);
    }

    public boolean isActive(IKeyConflictContext context) {
        return mcc$modern().isActive(context == null ? null : context.mcc$modern());
    }

    public String getLocalizedComboName(int keyCode) {
        String key = legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings.getKeyDisplayString(keyCode);
        if (this == NONE) return key;
        return mcc$modern().getCombinedName(com.mojang.blaze3d.platform.InputConstants.UNKNOWN, () -> net.minecraft.network.chat.Component.literal(key)).getString();
    }
}
