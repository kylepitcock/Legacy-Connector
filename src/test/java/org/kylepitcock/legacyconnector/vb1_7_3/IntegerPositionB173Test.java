package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.List;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3i;
import org.junit.jupiter.api.Test;

class IntegerPositionB173Test {
    @Test void connectedPositionWalkDeduplicatesEqualCoordinatesAwayFromOrigin(){
        var seen=new HashSet<Vec3i>();var first=new Vec3i(48,72,-15);
        assertTrue(seen.add(first));assertTrue(seen.add(new Vec3i(49,72,-15)));
        assertFalse(seen.add(new Vec3i(48,72,-15)));assertEquals(2,seen.size());
        var copy=new Vec3i(first);assertEquals(first,copy);assertEquals(first.hashCode(),copy.hashCode());
        copy.x++;assertNotEquals(first,copy);assertEquals(1,copy.distanceTo(48,72,-15));
    }
    @Test void originalOrderingUsesHeightThenDepthThenWidth(){
        var positions=new java.util.ArrayList<>(List.of(new Vec3i(0,1,0),new Vec3i(1,0,0),new Vec3i(0,0,1),new Vec3i(0,0,0)));
        positions.sort(null);assertEquals(List.of(new Vec3i(0,0,0),new Vec3i(1,0,0),new Vec3i(0,0,1),new Vec3i(0,1,0)),positions);
    }
}
