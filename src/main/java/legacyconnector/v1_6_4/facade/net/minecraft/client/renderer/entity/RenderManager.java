package legacyconnector.v1_6_4.facade.net.minecraft.client.renderer.entity;

/** Facade of the 1.7.10 shim's {@code RenderManager} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class RenderManager {
    private RenderManager() {}

    /** 1.6.4 {@code renderEntityWithPosYaw(entity, x, y, z, yaw, partialTicks)} returned nothing. */
    public static void renderEntityWithPosYaw(legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderManager self,
            legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        self.renderEntityWithPosYaw(entity, x, y, z, yaw, partialTicks);
    }

    /** 1.6.4 {@code renderEntity(entity, partialTicks)} returned nothing. */
    public static void renderEntity(legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderManager self,
            legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, float partialTicks) {
        self.renderEntitySimple(entity, partialTicks);
    }
}
