package legacyconnector.v1_7_10.cpw.mods.fml.common.gameevent;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;

/**
 * FML's input events, posted on the FML bus for each keyboard or mouse event while no screen is open (mods read the event
 * through {@code Keyboard.getEventKey()} / {@code Mouse.getEventButton()}). Key bindings have their new state already.
 */
public class InputEvent extends Event {
    public static class KeyInputEvent extends InputEvent {
    }

    public static class MouseInputEvent extends InputEvent {
    }
}
