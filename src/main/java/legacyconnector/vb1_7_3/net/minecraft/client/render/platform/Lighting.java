package legacyconnector.vb1_7_3.net.minecraft.client.render.platform;

/** Original GUI item/entity lighting uses the native inventory lighting setup. */
public final class Lighting {
    private Lighting() {}
    public static void turnOn() { org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173.lighting(true); }
    public static void turnOff() { org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173.lighting(false); }
}
