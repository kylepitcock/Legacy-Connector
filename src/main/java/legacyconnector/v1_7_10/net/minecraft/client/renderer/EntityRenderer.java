package legacyconnector.v1_7_10.net.minecraft.client.renderer;

/** 1.7.10's world renderer object: mods set a couple of its debug fields; rendering itself is the modern game's. */
public class EntityRenderer {
    public int debugViewDirection;
    public boolean debugView;
    public float farPlaneDistance;
    public int rendererUpdateCount;

    public void updateCameraAndRender(float partialTicks) {
    }

    public void disableLightmap(double partialTicks) {
    }

    public void enableLightmap(double partialTicks) {
    }

    public void func_78483_a(double partialTicks) {
    }

    public void func_78463_b(double partialTicks) {
    }
}
