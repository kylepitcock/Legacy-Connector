package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Set;
import legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup;
import legacyconnector.vb1_7_3.net.minecraft.src.ModLoader;
import net.minecraft.world.entity.MobCategory;
import org.junit.jupiter.api.Test;

class SpawnCapsB173Test {
    @Test void successfulMappedWritesOverrideOnlyTheirCategoryAndReadCurrentCapacity() throws Exception {
        Field registry=SpawnCapsB173.class.getDeclaredField("overrides");registry.setAccessible(true);
        Object previous=registry.get(null);
        Field capacity=SpawnGroup.class.getDeclaredField("capacity");capacity.setAccessible(true);
        int original=SpawnGroup.CREATURE.getCapacity();
        try {
            registry.set(null,Set.of());
            assertEquals(10,SpawnCapsB173.capacity(MobCategory.CREATURE,10));
            ModLoader.setPrivateValue(SpawnGroup.class,SpawnGroup.CREATURE,"e",original);
            assertTrue(SpawnCapsB173.isOverridden(SpawnGroup.CREATURE),"same-value successful writes are explicit overrides");
            assertEquals(original,SpawnCapsB173.capacity(MobCategory.CREATURE,10));
            ModLoader.setPrivateValue(SpawnGroup.class,SpawnGroup.CREATURE,4,37);
            assertEquals(37,SpawnCapsB173.capacity(MobCategory.CREATURE,10));
            capacity.setInt(SpawnGroup.CREATURE,42);
            assertEquals(42,SpawnCapsB173.capacity(MobCategory.CREATURE,10),"read current field rather than a cached written value");
            assertEquals(70,SpawnCapsB173.capacity(MobCategory.MONSTER,70));
            assertEquals(5,SpawnCapsB173.capacity(MobCategory.WATER_CREATURE,5));
            assertEquals(15,SpawnCapsB173.capacity(MobCategory.AMBIENT,15));
            assertFalse(SpawnCapsB173.isOverridden(SpawnGroup.MONSTER));
            assertThrows(RuntimeException.class,()->ModLoader.setPrivateValue(SpawnGroup.class,SpawnGroup.MONSTER,4,"invalid"));
            assertFalse(SpawnCapsB173.isOverridden(SpawnGroup.MONSTER),"failed writes do not activate a cap");
            registry.set(null,Set.of());
            ModLoader.setPrivateValue(SpawnGroup.class,SpawnGroup.CREATURE,"g",true);
            assertFalse(SpawnCapsB173.hasOverrides(),"unrelated successful writes do not activate caps");
        } finally {capacity.setInt(SpawnGroup.CREATURE,original);registry.set(null,previous);}
    }
}
