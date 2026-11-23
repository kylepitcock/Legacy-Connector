package legacyconnector.v1_12_2.org.lwjgl.input;

/** LWJGL 2 keyboard polling for 1.12.2 mods: the 1.7.10 shim's implementation on the modern window. */
public class Keyboard extends legacyconnector.v1_7_10.org.lwjgl.input.Keyboard {
    // The event being handled: while a 1.12.2 key event is dispatched (a legacy screen's handleKeyboardInput, Forge's GuiScreenEvent.KeyboardInputEvent
    // and InputEvent.KeyInputEvent) its LWJGL 2 state is in InputEvent1122; otherwise the 1.7.10 shim's.

    public static int getEventKey() {
        return org.kylepitcock.legacyconnector.v1_12_2.client.InputEvent1122.keyDispatching
                ? org.kylepitcock.legacyconnector.v1_12_2.client.InputEvent1122.eventKey : legacyconnector.v1_7_10.org.lwjgl.input.Keyboard.getEventKey();
    }

    public static boolean getEventKeyState() {
        return org.kylepitcock.legacyconnector.v1_12_2.client.InputEvent1122.keyDispatching
                ? org.kylepitcock.legacyconnector.v1_12_2.client.InputEvent1122.eventKeyState : legacyconnector.v1_7_10.org.lwjgl.input.Keyboard.getEventKeyState();
    }

    public static char getEventCharacter() {
        return org.kylepitcock.legacyconnector.v1_12_2.client.InputEvent1122.keyDispatching
                ? org.kylepitcock.legacyconnector.v1_12_2.client.InputEvent1122.eventCharacter : legacyconnector.v1_7_10.org.lwjgl.input.Keyboard.getEventCharacter();
    }
}
