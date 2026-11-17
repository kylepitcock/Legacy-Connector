package org.kylepitcock.legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.MiscCompat;
import org.junit.jupiter.api.Test;

class MiscCompat1201Test {
    @Test
    void blockPositionAabbFactoryPreservesRawUpperCoordinates() {
        AABB box = MiscCompat.newAabb(new BlockPos(4, 5, 6), new BlockPos(7, 8, 9));

        assertEquals(4.0, box.minX, 0.0);
        assertEquals(5.0, box.minY, 0.0);
        assertEquals(6.0, box.minZ, 0.0);
        assertEquals(7.0, box.maxX, 0.0);
        assertEquals(8.0, box.maxY, 0.0);
        assertEquals(9.0, box.maxZ, 0.0);
    }

    @Test
    void blockPositionAabbFactoryStillNormalizesReversedPositionsWithoutExpansion() {
        AABB box = MiscCompat.newAabb(new BlockPos(7, 8, 9), new BlockPos(4, 5, 6));

        assertEquals(4.0, box.minX, 0.0);
        assertEquals(5.0, box.minY, 0.0);
        assertEquals(6.0, box.minZ, 0.0);
        assertEquals(7.0, box.maxX, 0.0);
        assertEquals(8.0, box.maxY, 0.0);
        assertEquals(9.0, box.maxZ, 0.0);
    }
}
