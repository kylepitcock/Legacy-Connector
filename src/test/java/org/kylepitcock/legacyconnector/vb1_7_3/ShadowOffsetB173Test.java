package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.vb1_7_3.client.ShadowOffsetB173;

class ShadowOffsetB173Test {
    @Test void invokesOriginalOverrideAndConvertsItsLegacyCoordinateOrigin(){
        class Original extends legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity {
            int calls;
            Original(){super(null);standingEyeHeight=.125F;}
            @Override public float getShadowRadius(){calls++;return -.5F;}
        }
        var original=new Original();
        assertEquals(-.375,ShadowOffsetB173.offset(original));
        assertEquals(1,original.calls);
        assertEquals(10,ShadowOffsetB173.sourceY(10));
    }
}
