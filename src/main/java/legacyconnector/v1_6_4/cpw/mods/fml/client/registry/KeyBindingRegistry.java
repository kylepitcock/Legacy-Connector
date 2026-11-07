package legacyconnector.v1_6_4.cpw.mods.fml.client.registry;

import java.util.EnumSet;
import legacyconnector.v1_6_4.cpw.mods.fml.common.ITickHandler;
import legacyconnector.v1_6_4.cpw.mods.fml.common.TickType;
import legacyconnector.v1_7_10.cpw.mods.fml.client.registry.ClientRegistry;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import legacyconnector.v1_7_10.net.minecraft.client.settings.KeyBinding;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.Ticks164;

/** FML 1.6's key binding registry: a {@link KeyHandler} owns bindings and is called on key presses; registering it also lists the bindings in the controls screen. */
public final class KeyBindingRegistry {
    private KeyBindingRegistry() {}

    public static void registerKeyBinding(KeyHandler handler) {
        for (KeyBinding key : handler.getKeyBindings()) ClientRegistry.registerKeyBinding(key);
        Ticks164.register(handler, Side.CLIENT);
    }

    /** Polls its bindings on the client's ticks and calls {@link #keyDown} / {@link #keyUp} on changes (and every tick for repeating keys). */
    public abstract static class KeyHandler implements ITickHandler {
        protected KeyBinding[] keyBindings;
        protected boolean[] keyDown;
        protected boolean[] repeatings;

        public KeyHandler(KeyBinding[] keyBindings, boolean[] repeatings) {
            assert keyBindings.length == repeatings.length : "You need to pass two arrays of identical length";
            this.keyBindings = keyBindings;
            this.repeatings = repeatings;
            this.keyDown = new boolean[keyBindings.length];
        }

        public KeyHandler(KeyBinding[] keyBindings) {
            this.keyBindings = keyBindings;
            this.repeatings = new boolean[keyBindings.length];
            this.keyDown = new boolean[keyBindings.length];
        }

        public KeyBinding[] getKeyBindings() {
            return keyBindings;
        }

        @Override
        public final void tickStart(EnumSet<TickType> type, Object... tickData) {
            keyTick(type, false);
        }

        @Override
        public final void tickEnd(EnumSet<TickType> type, Object... tickData) {
            keyTick(type, true);
        }

        private void keyTick(EnumSet<TickType> type, boolean tickEnd) {
            for (int i = 0; i < keyBindings.length; i++) {
                KeyBinding key = keyBindings[i];
                boolean state = key.getIsKeyPressed();
                if (state != keyDown[i] || (state && repeatings[i])) {
                    if (state) keyDown(type, key, tickEnd, state != keyDown[i]);
                    else keyUp(type, key, tickEnd);
                    if (tickEnd) keyDown[i] = state;
                }
            }
        }

        public abstract void keyDown(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd, boolean isRepeat);

        public abstract void keyUp(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd);

        @Override
        public abstract EnumSet<TickType> ticks();
    }
}
