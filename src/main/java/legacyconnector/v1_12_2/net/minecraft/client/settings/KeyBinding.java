package legacyconnector.v1_12_2.net.minecraft.client.settings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraftforge.client.settings.IKeyConflictContext;
import legacyconnector.v1_12_2.net.minecraftforge.client.settings.KeyConflictContext;
import legacyconnector.v1_12_2.net.minecraftforge.client.settings.KeyModifier;

/**
 * A 1.12.2 key binding (with Forge's conflict context and modifier), backed by a modern {@code KeyMapping} that is added to the controls
 * screen when the mod registers it through {@code ClientRegistry.registerKeyBinding}. Key codes are LWJGL 2 ones (mouse buttons are
 * {@code -100 + button}); the key the player chose in the controls screen is read back from the modern mapping.
 */
public class KeyBinding implements Comparable<KeyBinding> {
    private static final List<KeyBinding> REGISTERED = new ArrayList<>();
    private static final Map<String, KeyBinding> KEYBIND_ARRAY = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Set<String> KEYBIND_SET = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final Set<String> USED_NAMES = new HashSet<>();
    private static boolean modernRegistered;

    private final String keyDescription;
    private final int keyCodeDefault;
    private final String keyCategory;
    private int keyCode;
    private IKeyConflictContext keyConflictContext = KeyConflictContext.UNIVERSAL;
    private final KeyModifier keyModifierDefault;
    private KeyModifier keyModifier;
    /** The modern mapping: a vanilla one for the game's own bindings, created on registration for mods'. */
    private Object modern;

    public KeyBinding(String description, int keyCode, String category) {
        this(description, KeyConflictContext.UNIVERSAL, KeyModifier.NONE, keyCode, category);
    }

    public KeyBinding(String description, IKeyConflictContext keyConflictContext, int keyCode, String category) {
        this(description, keyConflictContext, KeyModifier.NONE, keyCode, category);
    }

    public KeyBinding(String description, IKeyConflictContext keyConflictContext, KeyModifier keyModifier, int keyCode, String category) {
        this.keyDescription = description;
        this.keyCode = keyCode;
        this.keyCodeDefault = keyCode;
        this.keyCategory = category;
        this.keyConflictContext = keyConflictContext == null ? KeyConflictContext.UNIVERSAL : keyConflictContext;
        this.keyModifier = keyModifier == null ? KeyModifier.NONE : keyModifier;
        this.keyModifierDefault = this.keyModifier;
        if (description != null) KEYBIND_ARRAY.put(description, this);
        if (category != null) KEYBIND_SET.add(category);
    }

    /** A binding over one of the game's own modern mappings. */
    public static KeyBinding mcc$vanilla(Object mapping) {
        var m = (net.minecraft.client.KeyMapping) mapping;
        KeyBinding k = new KeyBinding(m.getName(), KeyConflictContext.mcc$of(m.getKeyConflictContext()), KeyModifier.mcc$of(m.getKeyModifier()),
                legacyCode(m.getKey()), m.getCategory());
        k.modern = m;
        return k;
    }

    private static int legacyCode(com.mojang.blaze3d.platform.InputConstants.Key key) {
        if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE) return key.getValue() - 100;
        if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM) return org.kylepitcock.legacyconnector.v1_7_10.client.KeyCodes.fromGlfw(key.getValue());
        return 0;
    }

    private static com.mojang.blaze3d.platform.InputConstants.Key modernKey(int legacy) {
        if (legacy < 0) return com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(legacy + 100);
        int glfw = org.kylepitcock.legacyconnector.v1_7_10.client.KeyCodes.toGlfw(legacy);
        return glfw <= 0 ? com.mojang.blaze3d.platform.InputConstants.UNKNOWN : com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(glfw);
    }

    // ------------------------------------------------------------------ registration with the modern controls

    public static synchronized void mcc$register(KeyBinding key) {
        if (REGISTERED.contains(key)) return;
        REGISTERED.add(key);
        if (modernRegistered) key.mcc$addLate();
    }

    public static synchronized List<KeyBinding> mcc$registered() {
        return new ArrayList<>(REGISTERED);
    }

    /** Called from the key mapping registration event: the mappings of the bindings registered so far. */
    public static synchronized List<Object> mcc$modernMappings() {
        modernRegistered = true;
        List<Object> out = new ArrayList<>();
        for (KeyBinding k : REGISTERED) out.add(k.mcc$modern());
        return out;
    }

    /** A binding registered after the controls were set up (in a mod's init or later) is appended to the options' list. */
    private void mcc$addLate() {
        try {
            var options = net.minecraft.client.Minecraft.getInstance().options;
            var mapping = (net.minecraft.client.KeyMapping) mcc$modern();
            for (var existing : options.keyMappings) if (existing == mapping) return;
            options.keyMappings = org.apache.commons.lang3.ArrayUtils.add(options.keyMappings, mapping);
            net.minecraft.client.KeyMapping.resetMapping();
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Keys").warn("Could not add key binding {}: {}", keyDescription, e.toString());
        }
    }

    public synchronized Object mcc$modern() {
        if (modern == null) {
            modern = new net.minecraft.client.KeyMapping(uniqueName(keyDescription), keyConflictContext.mcc$modern(), keyModifier.mcc$modern(),
                    modernKey(keyCode), keyCategory);
        }
        return modern;
    }

    /** Options are saved by key name and a duplicate name makes the options file unreadable. */
    private static synchronized String uniqueName(String name) {
        String candidate = name == null ? "key.legacy" : name;
        for (int i = 2; !USED_NAMES.add(candidate); i++) candidate = name + "." + i;
        return candidate;
    }

    private net.minecraft.client.KeyMapping mapping() {
        return (net.minecraft.client.KeyMapping) modern;
    }

    // ------------------------------------------------------------------ 1.12.2 API

    public static void onTick(int keyCode) {
        if (keyCode == 0) return;
        net.minecraft.client.KeyMapping.click(modernKey(keyCode));
    }

    public static void setKeyBindState(int keyCode, boolean pressed) {
        if (keyCode == 0) return;
        net.minecraft.client.KeyMapping.set(modernKey(keyCode), pressed);
    }

    public static void updateKeyBindState() {
        net.minecraft.client.KeyMapping.setAll();
    }

    public static void unPressAllKeys() {
        net.minecraft.client.KeyMapping.releaseAll();
    }

    public static void resetKeyBindingArrayAndHash() {
        net.minecraft.client.KeyMapping.resetMapping();
    }

    public static Set<String> getKeybinds() {
        return KEYBIND_SET;
    }

    /** True while the key is held (in its conflict context, with its modifier). */
    public boolean isKeyDown() {
        return modern != null && mapping().isDown();
    }

    /** True once per press (the presses are counted). */
    public boolean isPressed() {
        return modern != null && mapping().consumeClick();
    }

    public String getKeyCategory() {
        return keyCategory;
    }

    public String getKeyDescription() {
        return keyDescription;
    }

    public int getKeyCodeDefault() {
        return keyCodeDefault;
    }

    /** The key the player chose (LWJGL 2 code). */
    public int getKeyCode() {
        return modern != null ? legacyCode(mapping().getKey()) : keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
        if (modern != null) {
            mapping().setKey(modernKey(keyCode));
            net.minecraft.client.KeyMapping.resetMapping();
        }
    }

    @Override
    public int compareTo(KeyBinding other) {
        int c = net.minecraft.locale.Language.getInstance().getOrDefault(keyCategory).compareTo(net.minecraft.locale.Language.getInstance().getOrDefault(other.keyCategory));
        return c != 0 ? c : net.minecraft.locale.Language.getInstance().getOrDefault(keyDescription).compareTo(net.minecraft.locale.Language.getInstance().getOrDefault(other.keyDescription));
    }

    /** The key's name as the controls screen shows it. */
    public String getDisplayName() {
        if (modern != null) return mapping().getTranslatedKeyMessage().getString();
        return keyModifier.getLocalizedComboName(keyCode);
    }

    public boolean isSetToDefaultValue() {
        return getKeyCode() == keyCodeDefault && getKeyModifier() == keyModifierDefault;
    }

    // Forge additions

    public boolean isActiveAndMatches(int keyCode) {
        return keyCode != 0 && keyCode == getKeyCode() && getKeyConflictContext().isActive() && getKeyModifier().isActive(getKeyConflictContext());
    }

    public void setKeyConflictContext(IKeyConflictContext context) {
        this.keyConflictContext = context == null ? KeyConflictContext.UNIVERSAL : context;
        if (modern != null) mapping().setKeyConflictContext(keyConflictContext.mcc$modern());
    }

    public IKeyConflictContext getKeyConflictContext() {
        return keyConflictContext;
    }

    public KeyModifier getKeyModifierDefault() {
        return keyModifierDefault;
    }

    public KeyModifier getKeyModifier() {
        return modern != null ? KeyModifier.mcc$of(mapping().getKeyModifier()) : keyModifier;
    }

    public void setKeyModifierAndCode(KeyModifier keyModifier, int keyCode) {
        this.keyModifier = keyModifier == null ? KeyModifier.NONE : keyModifier;
        this.keyCode = keyCode;
        if (modern != null) {
            mapping().setKeyModifierAndCode(this.keyModifier.mcc$modern(), modernKey(keyCode));
            net.minecraft.client.KeyMapping.resetMapping();
        }
    }

    public void setToDefault() {
        setKeyModifierAndCode(keyModifierDefault, keyCodeDefault);
    }

    public boolean conflicts(KeyBinding other) {
        if (other == null) return false;
        if (!getKeyConflictContext().conflicts(other.getKeyConflictContext()) && !other.getKeyConflictContext().conflicts(getKeyConflictContext())) return false;
        return getKeyCode() == other.getKeyCode() && getKeyModifier() == other.getKeyModifier();
    }

    public boolean hasKeyCodeModifierConflict(KeyBinding other) {
        return KeyModifier.isKeyCodeModifier(getKeyCode()) && getKeyModifier() == KeyModifier.NONE && other.getKeyModifier().matches(getKeyCode());
    }

    public String getDisplayNameWithModifier() {
        return getDisplayName();
    }
}
