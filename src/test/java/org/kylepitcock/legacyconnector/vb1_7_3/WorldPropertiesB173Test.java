package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.junit.jupiter.api.Test;

class WorldPropertiesB173Test {
    static final class ClockWorld extends World {
        long time;
        @Override public long getTime(){return time;}
    }
    @Test void propertiesFollowTheirOwningWorldClockWithoutDetachedTime(){
        var first=new ClockWorld();var second=new ClockWorld();var properties=first.properties;
        first.time=24001;second.time=17;
        assertEquals(40,first.saveInterval);
        assertDoesNotThrow(()->first.properties.getTime()%first.saveInterval);
        assertEquals(first.getTime(),properties.getTime());assertEquals(17,second.properties.getTime());
        first.time=48002;
        assertSame(properties,first.properties);assertEquals(48002,properties.getTime());assertEquals(17,second.properties.getTime());
    }
}
