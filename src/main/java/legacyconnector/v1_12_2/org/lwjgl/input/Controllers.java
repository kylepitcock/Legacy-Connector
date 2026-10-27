package legacyconnector.v1_12_2.org.lwjgl.input;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.glfw.GLFW;

/** LWJGL 2 controller list: the GLFW joysticks present when {@link #create()} ran. Controller events are not reported. */
public class Controllers {
    private static final List<Controller> CONTROLLERS = new ArrayList<>();
    private static boolean created;

    public static synchronized void create() {
        if (created) return;
        created = true;
        for (int jid = GLFW.GLFW_JOYSTICK_1; jid <= GLFW.GLFW_JOYSTICK_LAST; jid++) {
            try {
                if (GLFW.glfwJoystickPresent(jid)) CONTROLLERS.add(new Joystick(jid, CONTROLLERS.size()));
            } catch (Throwable e) {
                break; // no GLFW (dedicated server)
            }
        }
    }

    public static synchronized Controller getController(int index) {
        return CONTROLLERS.get(index);
    }

    public static synchronized int getControllerCount() {
        return CONTROLLERS.size();
    }

    public static synchronized void poll() {
        for (Controller c : CONTROLLERS) c.poll();
    }

    public static boolean isCreated() {
        return created;
    }

    public static synchronized void destroy() {
        CONTROLLERS.clear();
        created = false;
    }

    public static boolean next() {
        return false;
    }

    public static void clearEvents() {
    }

    public static Controller getEventSource() {
        return null;
    }

    private static final class Joystick implements Controller {
        private final int jid;
        private final int index;
        private final String name;
        private float[] axes = new float[0];
        private byte[] buttons = new byte[0];
        private float[] deadZones = new float[0];

        Joystick(int jid, int index) {
            this.jid = jid;
            this.index = index;
            String n = GLFW.glfwGetJoystickName(jid);
            this.name = n == null ? "Joystick " + jid : n;
            poll();
        }

        @Override
        public void poll() {
            try {
                var a = GLFW.glfwGetJoystickAxes(jid);
                if (a != null) {
                    if (axes.length != a.remaining()) {
                        axes = new float[a.remaining()];
                        deadZones = java.util.Arrays.copyOf(deadZones, axes.length);
                    }
                    for (int i = 0; i < axes.length; i++) axes[i] = a.get(a.position() + i);
                }
                var b = GLFW.glfwGetJoystickButtons(jid);
                if (b != null) {
                    if (buttons.length != b.remaining()) buttons = new byte[b.remaining()];
                    for (int i = 0; i < buttons.length; i++) buttons[i] = b.get(b.position() + i);
                }
            } catch (Throwable e) {
                // unplugged: keep the last values
            }
        }

        private float axis(int i) {
            if (i < 0 || i >= axes.length) return 0.0F;
            float v = axes[i];
            return Math.abs(v) < deadZones[i] ? 0.0F : v;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public int getButtonCount() {
            return buttons.length;
        }

        @Override
        public String getButtonName(int i) {
            return "Button " + i;
        }

        @Override
        public boolean isButtonPressed(int i) {
            return i >= 0 && i < buttons.length && buttons[i] == GLFW.GLFW_PRESS;
        }

        @Override
        public float getPovX() {
            return 0.0F;
        }

        @Override
        public float getPovY() {
            return 0.0F;
        }

        @Override
        public float getDeadZone(int i) {
            return i >= 0 && i < deadZones.length ? deadZones[i] : 0.0F;
        }

        @Override
        public void setDeadZone(int i, float zone) {
            if (i >= 0 && i < deadZones.length) deadZones[i] = zone;
        }

        @Override
        public int getAxisCount() {
            return axes.length;
        }

        @Override
        public String getAxisName(int i) {
            return switch (i) {
                case 0 -> "X Axis";
                case 1 -> "Y Axis";
                case 2 -> "X Rotation";
                case 3 -> "Y Rotation";
                case 4 -> "Z Axis";
                case 5 -> "Z Rotation";
                default -> "Axis " + i;
            };
        }

        @Override
        public float getAxisValue(int i) {
            return axis(i);
        }

        @Override
        public float getXAxisValue() {
            return axis(0);
        }

        @Override
        public float getXAxisDeadZone() {
            return getDeadZone(0);
        }

        @Override
        public void setXAxisDeadZone(float zone) {
            setDeadZone(0, zone);
        }

        @Override
        public float getYAxisValue() {
            return axis(1);
        }

        @Override
        public float getYAxisDeadZone() {
            return getDeadZone(1);
        }

        @Override
        public void setYAxisDeadZone(float zone) {
            setDeadZone(1, zone);
        }

        @Override
        public float getZAxisValue() {
            return axis(4);
        }

        @Override
        public float getZAxisDeadZone() {
            return getDeadZone(4);
        }

        @Override
        public void setZAxisDeadZone(float zone) {
            setDeadZone(4, zone);
        }

        @Override
        public float getRXAxisValue() {
            return axis(2);
        }

        @Override
        public float getRXAxisDeadZone() {
            return getDeadZone(2);
        }

        @Override
        public void setRXAxisDeadZone(float zone) {
            setDeadZone(2, zone);
        }

        @Override
        public float getRYAxisValue() {
            return axis(3);
        }

        @Override
        public float getRYAxisDeadZone() {
            return getDeadZone(3);
        }

        @Override
        public void setRYAxisDeadZone(float zone) {
            setDeadZone(3, zone);
        }

        @Override
        public float getRZAxisValue() {
            return axis(5);
        }

        @Override
        public float getRZAxisDeadZone() {
            return getDeadZone(5);
        }

        @Override
        public void setRZAxisDeadZone(float zone) {
            setDeadZone(5, zone);
        }

        @Override
        public int getRumblerCount() {
            return 0;
        }

        @Override
        public String getRumblerName(int i) {
            return null;
        }

        @Override
        public void setRumblerStrength(int i, float strength) {
        }
    }
}
