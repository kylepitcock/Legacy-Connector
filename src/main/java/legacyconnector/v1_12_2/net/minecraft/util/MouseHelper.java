package legacyconnector.v1_12_2.net.minecraft.util;

/**
 * 1.12.2's mouse grabber: grabs and releases the cursor through the game's mouse handler; {@link #mouseXYChange} measures the cursor
 * movement since the last call (1.12's deltas: x to the right, y upwards).
 */
public class MouseHelper {
    public int deltaX;
    public int deltaY;
    private double mcc$lastX = Double.NaN;
    private double mcc$lastY = Double.NaN;

    public void grabMouseCursor() {
        if (client()) ClientAccess.grab();
    }

    public void ungrabMouseCursor() {
        if (client()) ClientAccess.release();
    }

    public void mouseXYChange() {
        if (!client()) {
            deltaX = 0;
            deltaY = 0;
            return;
        }
        double x = ClientAccess.x();
        double y = ClientAccess.y();
        if (Double.isNaN(mcc$lastX)) {
            deltaX = 0;
            deltaY = 0;
        } else {
            deltaX = (int) Math.round(x - mcc$lastX);
            deltaY = (int) Math.round(mcc$lastY - y);
        }
        mcc$lastX = x;
        mcc$lastY = y;
    }

    private static boolean client() {
        return net.neoforged.fml.loading.FMLEnvironment.dist.isClient();
    }

    /** The game's mouse handler, kept apart so this class verifies on a dedicated server. */
    private static final class ClientAccess {
        private static net.minecraft.client.MouseHandler mouse() {
            return net.minecraft.client.Minecraft.getInstance().mouseHandler;
        }

        static void grab() {
            if (net.minecraft.client.Minecraft.getInstance().screen == null) mouse().grabMouse();
        }

        static void release() {
            mouse().releaseMouse();
        }

        static double x() {
            return mouse().xpos();
        }

        static double y() {
            return mouse().ypos();
        }
    }
}
