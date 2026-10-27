package org.kylepitcock.legacyconnector.v1_12_2.client;

/**
 * The LWJGL 2 input event being dispatched to a 1.12.2 screen ({@code GuiScreen.handleMouseInput}/{@code handleKeyboardInput}). Legacy
 * screens read it through {@code org.lwjgl.input.Mouse}/{@code Keyboard} ({@code getEventButton}, {@code getEventDWheel}, ...): the LWJGL
 * shim should answer those calls from here while {@link #mouseDispatching}/{@link #keyDispatching} are set. Plain state, no client classes.
 */
public final class InputEvent1122 {
    private InputEvent1122() {}

    public static boolean mouseDispatching;
    /** The button of the event (-1 for a move or wheel event). */
    public static int eventButton = -1;
    public static boolean eventButtonState;
    /** Wheel movement of the event, 120 per notch, positive away from the user. */
    public static int eventDWheel;
    /** Position of the event in GUI coordinates (top-left origin). */
    public static int guiX;
    public static int guiY;
    /** Position of the event in window pixels with LWJGL 2's bottom-left origin. */
    public static int eventX;
    public static int eventY;

    public static boolean keyDispatching;
    public static int eventKey;
    public static char eventCharacter;
    public static boolean eventKeyState;
}
