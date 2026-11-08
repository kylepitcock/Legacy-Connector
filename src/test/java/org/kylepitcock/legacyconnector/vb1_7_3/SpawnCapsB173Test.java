package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup;
import legacyconnector.vb1_7_3.net.minecraft.src.ModLoader;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class SpawnCapsB173Test {
    @Test void betaDefaultsUseInclusiveScaledBudget() {
        assertEquals(79,SpawnCapsB173.limit(SpawnGroup.MONSTER,289));
        assertEquals(16,SpawnCapsB173.limit(SpawnGroup.CREATURE,289));
        assertEquals(5,SpawnCapsB173.limit(SpawnGroup.WATER_CREATURE,289));
        assertTrue(SpawnCapsB173.allows(SpawnGroup.CREATURE,16,289));
        assertFalse(SpawnCapsB173.allows(SpawnGroup.CREATURE,17,289));
        assertEquals(15,SpawnCapsB173.limit(SpawnGroup.CREATURE,256));
        assertEquals(0,SpawnCapsB173.limit(SpawnGroup.CREATURE,0));
    }

    @Test void mappedWritesRemainDiagnosticAndBudgetReadsCurrentField() throws Exception {
        Field registry=SpawnCapsB173.class.getDeclaredField("overrides");registry.setAccessible(true);Object previous=registry.get(null);
        Field capacity=SpawnGroup.class.getDeclaredField("capacity");capacity.setAccessible(true);int original=SpawnGroup.CREATURE.getCapacity();
        try {
            registry.set(null,Set.of());
            assertEquals(original*289/256,SpawnCapsB173.limit(SpawnGroup.CREATURE,289));
            ModLoader.setPrivateValue(SpawnGroup.class,SpawnGroup.CREATURE,"e",original);
            assertTrue(SpawnCapsB173.isOverridden(SpawnGroup.CREATURE));
            ModLoader.setPrivateValue(SpawnGroup.class,SpawnGroup.CREATURE,4,37);
            assertEquals(41,SpawnCapsB173.limit(SpawnGroup.CREATURE,289));
            capacity.setInt(SpawnGroup.CREATURE,42);
            assertEquals(47,SpawnCapsB173.limit(SpawnGroup.CREATURE,289));
            assertFalse(SpawnCapsB173.isOverridden(SpawnGroup.MONSTER));
            assertThrows(RuntimeException.class,()->ModLoader.setPrivateValue(SpawnGroup.class,SpawnGroup.MONSTER,4,"invalid"));
            assertFalse(SpawnCapsB173.isOverridden(SpawnGroup.MONSTER));
            registry.set(null,Set.of());ModLoader.setPrivateValue(SpawnGroup.class,SpawnGroup.CREATURE,"g",true);
            assertFalse(SpawnCapsB173.hasOverrides());
        }finally{capacity.setInt(SpawnGroup.CREATURE,original);registry.set(null,previous);}
    }

    @Test void footprintIncludesEdgesNegativeCoordinatesAndUnionsOverlaps() {
        var single=SpawnCapsB173.footprint(List.of(new ChunkPos(-1,-2)));
        assertEquals(289,single.size());assertTrue(single.contains(new ChunkPos(-9,-10)));assertTrue(single.contains(new ChunkPos(7,6)));assertFalse(single.contains(new ChunkPos(8,6)));
        assertEquals(306,SpawnCapsB173.footprint(List.of(new ChunkPos(0,0),new ChunkPos(1,0))).size());
        assertEquals(289,SpawnCapsB173.footprint(List.of(new ChunkPos(0,0),new ChunkPos(0,0))).size());
        assertTrue(SpawnCapsB173.footprint(List.of()).isEmpty());
    }

    @Test void originalAncestryTakesPrecedenceOverNativeHostAndGroupsCanOverlap() {
        assertTrue(SpawnCapsB173.countsType(SpawnGroup.MONSTER,legacyconnector.vb1_7_3.net.minecraft.entity.mob.MonsterEntity.class,net.minecraft.world.entity.animal.Cow.class));
        assertFalse(SpawnCapsB173.countsType(SpawnGroup.CREATURE,legacyconnector.vb1_7_3.net.minecraft.entity.mob.MonsterEntity.class,net.minecraft.world.entity.animal.Cow.class));
        assertTrue(SpawnCapsB173.countsType(SpawnGroup.CREATURE,legacyconnector.vb1_7_3.net.minecraft.entity.passive.AnimalEntity.class,net.minecraft.world.entity.monster.Zombie.class));
        assertTrue(SpawnCapsB173.countsType(SpawnGroup.WATER_CREATURE,legacyconnector.vb1_7_3.net.minecraft.entity.WaterCreatureEntity.class,Object.class));
        assertFalse(SpawnCapsB173.countsType(SpawnGroup.WATER_CREATURE,legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity.class,Object.class));
        assertTrue(SpawnCapsB173.countsType(SpawnGroup.MONSTER,null,net.minecraft.world.entity.monster.Zombie.class));
        assertTrue(SpawnCapsB173.countsType(SpawnGroup.CREATURE,null,net.minecraft.world.entity.animal.Cow.class));
        assertTrue(SpawnCapsB173.countsType(SpawnGroup.WATER_CREATURE,null,net.minecraft.world.entity.animal.Squid.class));
        assertTrue(SpawnCapsB173.countsType(SpawnGroup.CREATURE,OriginalEnemyAnimal.class,Object.class));
        assertTrue(SpawnCapsB173.countsType(SpawnGroup.MONSTER,OriginalEnemyAnimal.class,Object.class));
    }
    private abstract static class OriginalEnemyAnimal extends legacyconnector.vb1_7_3.net.minecraft.entity.passive.AnimalEntity implements legacyconnector.vb1_7_3.net.minecraft.entity.Monster {
        OriginalEnemyAnimal(legacyconnector.vb1_7_3.net.minecraft.world.World world){super(world);}
    }
}
