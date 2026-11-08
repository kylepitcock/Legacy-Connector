package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import org.junit.jupiter.api.Test;

class EntityEnvironmentB173Test {
    static class View extends Entity {
        View(){super(null);}
        boolean originalWaterField(){return submergedInWater;}
    }
    @Test void cachedWaterContactMatchesOriginalProtectedFieldAndQuery() {
        var view=new View();assertFalse(view.originalWaterField());assertFalse(view.isSubmergedInWater());
        view.mcc$environment(true);assertTrue(view.originalWaterField());assertTrue(view.isSubmergedInWater());
        view.mcc$environment(false);assertFalse(view.originalWaterField());assertFalse(view.isSubmergedInWater());
    }
}
