package legacyconnector.v1_7_10.net.minecraftforge.client.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;

/** A mouse button press or wheel movement; cancelling stops the game from acting on it. */
@Cancelable
public class MouseEvent extends Event {
    public final int x;
    public final int y;
    public final int dx;
    public final int dy;
    public final int dwheel;
    public final int button;
    public final boolean buttonstate;
    public final long nanoseconds;

    public MouseEvent(int x, int y, int dx, int dy, int dwheel, int button, boolean buttonstate) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.dwheel = dwheel;
        this.button = button;
        this.buttonstate = buttonstate;
        this.nanoseconds = System.nanoTime();
    }
}
