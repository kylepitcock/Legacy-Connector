package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import legacyconnector.v1_7_10.net.minecraft.client.settings.KeyBinding;

/**
 * ModLoader 1.2.5's mod keys while a screen is open. {@code ModLoader.onTick} read {@code Keyboard.isKeyDown} for every key a mod registered, whatever screen was
 * open (checked in the real class); the modern key mappings are released when a screen opens, so the layer's key handler never sees such a press.
 * This list polls the keys' state itself and fires {@code keyboardEvent} on a press, or on every poll while held when the mod registered the key with
 * {@code allowRepeat}, but only while a screen is open (without one the layer's key handler already fires). The state is tracked on every poll, so a key
 * still held when a screen opens does not fire as a new press.
 */
public final class ModKeys125 {
    private static final List<Key> KEYS = new CopyOnWriteArrayList<>();

    private ModKeys125() {}

    /** One registered mod key: the action to run, whether it repeats while held, and its raw state. */
    public static final class Key {
        private final Runnable fire;
        private final boolean allowRepeat;
        private final BooleanSupplier isDown;
        private boolean down;

        public Key(Runnable fire, boolean allowRepeat, BooleanSupplier isDown) {
            this.fire = fire;
            this.allowRepeat = allowRepeat;
            this.isDown = isDown;
        }

        /** One poll: fires on a press, or while held with allowRepeat, when {@code screenOpen}; always records the state. */
        public void step(boolean screenOpen) {
            boolean now = isDown.getAsBoolean();
            boolean fireNow = screenOpen && now && (!down || allowRepeat);
            down = now;
            if (fireNow) fire.run();
        }
    }

    public static void add(Key key) {
        KEYS.add(key);
    }

    /** Polls every mod key (client tick). */
    public static void poll(boolean screenOpen) {
        for (Key k : KEYS) k.step(screenOpen);
    }

    /** Whether the key a binding is bound to is physically down right now (keyboard or mouse button), as LWJGL's {@code Keyboard.isKeyDown} answered. */
    public static boolean rawDown(KeyBinding binding) {
        var mapping = binding.mcc$modern();
        if (mapping == null) return false;
        var key = mapping.getKey();
        long window = net.minecraft.client.Minecraft.getInstance().getWindow().getWindow();
        if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE) {
            return org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, key.getValue()) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        }
        if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM) {
            return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, key.getValue());
        }
        return false;
    }
}
