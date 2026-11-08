package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.world.phys.Vec3;

/**
 * Stands in for {@code net.minecraft.world.level.portal.PortalInfo} (1.20.1), which 1.21.1 replaced by {@code DimensionTransition}: the position, speed and
 * rotation an entity arrives with.
 */
public class LegacyPortalInfo {
    public final Vec3 pos;
    public final Vec3 speed;
    public final float yRot;
    public final float xRot;

    public LegacyPortalInfo(Vec3 pos, Vec3 speed, float yRot, float xRot) {
        this.pos = pos;
        this.speed = speed;
        this.yRot = yRot;
        this.xRot = xRot;
    }
}
