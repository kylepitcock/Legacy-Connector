package legacyconnector.v1_7_10.net.minecraft.client.settings;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import org.kylepitcock.legacyconnector.v1_7_10.client.KeyCodes;

/**
 * A 1.7.10 key binding, backed by a modern {@link KeyMapping} (registered with the controls screen when the mod
 * registers it through {@code ClientRegistry.registerKeyBinding}). The key code is an LWJGL 2 one.
 */
public class KeyBinding implements Comparable<KeyBinding> {
    private static final List<KeyBinding> ALL = new ArrayList<>();
    /** Every binding made, vanilla and mods' (1.7.10 kept them for the controls screen and the key-code lookup). */
    public static final List keybindArray = new ArrayList();
    /** The categories of all bindings. */
    private static final java.util.Set keybindSet = new java.util.HashSet();
    /** Key code to binding, as 1.7.10 kept it (HBM looks up which binding owns a key to resolve overlaps). */
    public static final legacyconnector.v1_7_10.net.minecraft.util.IntHashMap hash = new legacyconnector.v1_7_10.net.minecraft.util.IntHashMap();

    private final String description;
    private final String category;
    private int keyCode;
    private final int keyCodeDefault;
    private KeyMapping modern;

    public KeyBinding(String description, int keyCode, String category) {
        this.description = description;
        this.keyCode = keyCode;
        this.keyCodeDefault = keyCode;
        this.category = category;
        mcc$listed(this);
    }

    private static synchronized void mcc$listed(KeyBinding key) {
        keybindArray.add(key);
        keybindSet.add(key.category);
        hash.addKey(key.keyCode, key);
    }

    /** A binding over an existing modern mapping (vanilla's). */
    public KeyBinding(KeyMapping existing) {
        this.description = existing.getName();
        this.category = existing.getCategory();
        this.keyCode = KeyCodes.fromGlfw(existing.getKey().getValue());
        this.keyCodeDefault = KeyCodes.fromGlfw(existing.getDefaultKey().getValue());
        this.modern = existing;
        mcc$listed(this);
    }

    public static synchronized List<KeyBinding> mcc$registered() {
        return new ArrayList<>(ALL);
    }

    public static synchronized void mcc$register(KeyBinding key) {
        if (ALL.contains(key)) return;
        ALL.add(key);
        if (mappingsCollected) addLate(key);
    }

    /** Whether the game already collected the key mappings (RegisterKeyMappingsEvent): later bindings join the options directly. */
    private static boolean mappingsCollected;

    public static synchronized void mcc$mappingsCollected() {
        mappingsCollected = true;
    }

    /**
     * A binding registered after the game collected the key mappings (DragonAPI's KeybindHandler registers ChromatiCraft's from
     * the client proxy) is added to the options as 1.7.10's ClientRegistry did, with its saved key read back.
     */
    private static void addLate(KeyBinding key) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || mc.options == null) return;
        KeyMapping m = key.mcc$modern();
        for (KeyMapping existing : mc.options.keyMappings) if (existing == m) return;
        mc.options.keyMappings = org.apache.commons.lang3.ArrayUtils.add(mc.options.keyMappings, m);
        try {
            mc.options.load();
        } catch (RuntimeException e) {
            // keeps its default key
        }
        KeyMapping.resetMapping();
    }

    /** The modern mapping, created on first use. */
    public synchronized KeyMapping mcc$modern() {
        if (modern == null) {
            // 1.7.10 stored mouse buttons as -100 + button (middle click -98)
            modern = keyCode < 0 && keyCode >= -100
                    ? new KeyMapping(uniqueName(description), com.mojang.blaze3d.platform.InputConstants.Type.MOUSE, keyCode + 100, category)
                    : new KeyMapping(uniqueName(description), com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, KeyCodes.toGlfw(keyCode), category);
        }
        return modern;
    }

    private static final java.util.Set<String> USED_NAMES = new java.util.HashSet<>();

    /**
     * Options are saved by key name, and a duplicate name makes the options file unreadable (the early loading window
     * collects it into a map and aborts). Legacy mods may register several bindings under one description (JourneyMap's minimap type).
     */
    private static synchronized String uniqueName(String name) {
        String candidate = name;
        for (int i = 2; !USED_NAMES.add(candidate); i++) candidate = name + "." + i;
        return candidate;
    }

    public boolean getIsKeyPressed() {
        return modern != null && modern.isDown();
    }

    public boolean isPressed() {
        return modern != null && modern.consumeClick();
    }

    public String getKeyDescription() {
        return description;
    }

    public String getKeyCategory() {
        return category;
    }

    /** The bound key in 1.7.10 codes: what the player set in the controls screen once the modern mapping exists. */
    public int getKeyCode() {
        KeyMapping m = modern;
        if (m == null) return keyCode;
        var key = m.getKey();
        if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE) return key.getValue() - 100;
        return key.getValue() == com.mojang.blaze3d.platform.InputConstants.UNKNOWN.getValue() ? 0 : KeyCodes.fromGlfw(key.getValue());
    }

    /** The key the binding was made with. */
    public int getKeyCodeDefault() {
        return keyCodeDefault;
    }

    public static java.util.Set getKeybinds() {
        return keybindSet;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
        KeyMapping m = modern;
        if (m != null) {
            m.setKey(keyCode < 0 && keyCode >= -100 ? com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(keyCode + 100)
                    : keyCode == 0 ? com.mojang.blaze3d.platform.InputConstants.UNKNOWN
                    : com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(KeyCodes.toGlfw(keyCode)));
            KeyMapping.resetMapping();
        }
    }

    public static void onTick(int keyCode) {
    }

    public static void setKeyBindState(int keyCode, boolean pressed) {
    }

    public static void unPressAllKeys() {
    }

    public static synchronized void resetKeyBindingArrayAndHash() {
        hash.clearMap();
        for (Object o : keybindArray) hash.addKey(((KeyBinding) o).getKeyCode(), o);
    }

    @Override
    public int compareTo(KeyBinding other) {
        int c = category.compareTo(other.category);
        return c != 0 ? c : description.compareTo(other.description);
    }
}
