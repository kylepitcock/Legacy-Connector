package legacyconnector.v1_6_4.facade.net.minecraft.client.settings;

import legacyconnector.v1_7_10.net.minecraft.client.settings.KeyBinding;

/** Facade of the 1.7.10 shim's {@code GameSettings} for 1.6.4 mods: the key binding list by index. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class GameSettings {
    private GameSettings() {}

    private static KeyBinding binding(legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings self, int index) {
        KeyBinding[] all = self.keyBindings;
        return index >= 0 && index < all.length ? all[index] : null;
    }

    /** 1.6.4 {@code getKeyBindingDescription(index)}: the translated description of the key binding at the index in the list. */
    public static String getKeyBindingDescription(legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings self, int index) {
        KeyBinding key = binding(self, index);
        return key == null ? "" : legacyconnector.v1_7_10.net.minecraft.client.resources.I18n.format(KeyBinding.mcc$get$keyDescription(key));
    }

    /** 1.6.4 {@code getOptionDisplayString(index)}: the name of the key bound at the index in the list. */
    public static String getOptionDisplayString(legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings self, int index) {
        KeyBinding key = binding(self, index);
        return key == null ? "" : legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings.getKeyDisplayString(key.getKeyCode());
    }

    /** 1.6.4 {@code setKeyBinding(index, code)}: binds the key code to the entry at the index in the list and saves the options. */
    public static void setKeyBinding(legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings self, int index, int code) {
        KeyBinding key = binding(self, index);
        if (key == null) return;
        key.setKeyCode(code);
        self.saveOptions();
    }
}
