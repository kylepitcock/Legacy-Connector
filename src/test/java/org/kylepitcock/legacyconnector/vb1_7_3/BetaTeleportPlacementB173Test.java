package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import org.junit.jupiter.api.Test;

class BetaTeleportPlacementB173Test {
    private static final class OriginalEntity extends Entity {
        OriginalEntity(){super(null);}
    }

    @Test void originalTeleporterPlacementTakesFeetYAndKeepsPreviousAngles() {
        var entity=new OriginalEntity();
        entity.standingEyeHeight=1.62F;
        entity.prevYaw=-47;entity.prevPitch=13;
        entity.setPositionAndAnglesKeepPrevAngles(-7.25,64.5,12.75,120,-20);
        double legacyY=64.5+(double)entity.standingEyeHeight;
        assertEquals(-7.25,entity.x);assertEquals(entity.x,entity.prevX);assertEquals(entity.x,entity.lastTickX);
        assertEquals(legacyY,entity.y);assertEquals(entity.y,entity.prevY);assertEquals(entity.y,entity.lastTickY);
        assertEquals(12.75,entity.z);assertEquals(entity.z,entity.prevZ);assertEquals(entity.z,entity.lastTickZ);
        assertEquals(64.5,entity.boundingBox.minY);
        assertEquals(120,entity.yaw);assertEquals(-20,entity.pitch);
        assertEquals(-47,entity.prevYaw);assertEquals(13,entity.prevPitch);
    }

    @Test void zeroEyeHeightAndRawPlacementRetainTheirDistinctContracts() {
        var entity=new OriginalEntity();
        entity.setPositionAndAnglesKeepPrevAngles(1,3,5,7,9);
        assertEquals(3,entity.y);assertEquals(3,entity.boundingBox.minY);
        entity.standingEyeHeight=1.62F;
        entity.setPositionAndAngles(11,20,31,45,60);
        assertEquals(20,entity.y);
        assertEquals(20-(double)entity.standingEyeHeight,entity.boundingBox.minY);
        assertEquals(45,entity.prevYaw);assertEquals(60,entity.prevPitch);
    }
}
