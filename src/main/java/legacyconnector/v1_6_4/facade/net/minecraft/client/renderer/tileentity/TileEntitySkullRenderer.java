package legacyconnector.v1_6_4.facade.net.minecraft.client.renderer.tileentity;

/** Facade of the 1.7.10 shim's {@code TileEntitySkullRenderer} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class TileEntitySkullRenderer {
    private TileEntitySkullRenderer() {}

    /** 1.6.4 {@code TileEntitySkullRenderer.skullRenderer} (static): the registered instance, 1.7.10's {@code field_147536_b}. */
    public static legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer mcc$get$skullRenderer() {
        return legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer.field_147536_b;
    }

    /**
     * 1.6.4 {@code func_82393_a(x, y, z, face, rotation, type, owner)}: draws a head with an owner's name (1.7.10 takes the owner's game profile; the skin
     * is that of the player of that name when the client knows them, the default skin otherwise).
     */
    public static void func_82393_a(legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer self, float x, float y, float z, int face,
            float rotation, int type, String owner) {
        self.func_152674_a(x, y, z, face, rotation, type,
                legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer.mcc$profile(owner));
    }
}
