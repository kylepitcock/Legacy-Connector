package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/**
 * A mouse event while playing (no screen open): button presses and releases ({@code button} 0-2, -1 for moves and the wheel) and wheel
 * notches ({@code dwheel}, 120 per notch). Cancelling stops the game from handling it (attacking, using, changing the hotbar slot).
 */
@Cancelable
public class MouseEvent extends Event {
    private final int x;
    private final int y;
    private final int dx;
    private final int dy;
    private final int dwheel;
    private final int button;
    private final boolean buttonstate;
    private final long nanoseconds;

    public MouseEvent(int x, int y, int dx, int dy, int dwheel, int button, boolean buttonstate, long nanoseconds) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.dwheel = dwheel;
        this.button = button;
        this.buttonstate = buttonstate;
        this.nanoseconds = nanoseconds;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }

    public int getDwheel() {
        return dwheel;
    }

    public int getButton() {
        return button;
    }

    public boolean isButtonstate() {
        return buttonstate;
    }

    public long getNanoseconds() {
        return nanoseconds;
    }
}
