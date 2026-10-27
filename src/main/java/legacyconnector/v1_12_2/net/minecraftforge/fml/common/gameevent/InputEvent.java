package legacyconnector.v1_12_2.net.minecraftforge.fml.common.gameevent;

import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/**
 * FML's input events, fired on the client after a key or mouse button changed while playing; handlers check their key bindings
 * ({@code isPressed}) or read {@code Keyboard.getEventKey()}.
 */
public class InputEvent extends Event {
    public static class KeyInputEvent extends InputEvent {
    }

    public static class MouseInputEvent extends InputEvent {
    }
}
