package legacyconnector.v1_6_4.facade.net.minecraft.util;

/**
 * Facade of the 1.7.10 shim's {@code Vec3} for the versions before 1.6.4's removal of the pools: {@code Vec3.vec3dPool}, the static pool 1.4.7 to 1.6.4
 * mods took vectors from. See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class Vec3 {
    private Vec3() {}

    /** Vec3.vec3dPool */
    public static legacyconnector.v1_6_4.net.minecraft.util.Vec3Pool mcc$get$vec3dPool() {
        return legacyconnector.v1_6_4.net.minecraft.util.Vec3Pool.INSTANCE;
    }
}
