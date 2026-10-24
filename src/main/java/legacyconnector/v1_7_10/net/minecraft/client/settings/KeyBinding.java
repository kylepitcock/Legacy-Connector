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

    private final String description;
    private final String category;
    private int keyCode;
    private KeyMapping modern;

    public KeyBinding(String description, int keyCode, String category) {
        this.description = description;
        this.keyCode = keyCode;
        this.category = category;
    }

    /** A binding over an existing modern mapping (vanilla's). */
    public KeyBinding(KeyMapping existing) {
        this.description = existing.getName();
        this.category = existing.getCategory();
        this.keyCode = KeyCodes.fromGlfw(existing.getKey().getValue());
        this.modern = existing;
    }

    public static synchronized List<KeyBinding> mcc$registered() {
        return new ArrayList<>(ALL);
    }

    public static synchronized void mcc$register(KeyBinding key) {
        if (!ALL.contains(key)) ALL.add(key);
    }

    /** The modern mapping, created on first use. */
    public synchronized KeyMapping mcc$modern() {
        if (modern == null) {
            int glfw = KeyCodes.toGlfw(keyCode);
            modern = new KeyMapping(uniqueName(description), com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, glfw, category);
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

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public static void onTick(int keyCode) {
    }

    public static void setKeyBindState(int keyCode, boolean pressed) {
    }

    public static void unPressAllKeys() {
    }

    public static void resetKeyBindingArrayAndHash() {
    }

    @Override
    public int compareTo(KeyBinding other) {
        int c = category.compareTo(other.category);
        return c != 0 ? c : description.compareTo(other.description);
    }
}
