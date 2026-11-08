package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.assertSame;
import legacyconnector.vb1_7_3.net.minecraft.entity.Monster;
import legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup;
import legacyconnector.vb1_7_3.net.minecraft.entity.WaterCreatureEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.passive.AnimalEntity;
import org.junit.jupiter.api.Test;

class SpawnGroupCreatureClassB173Test {
    @Test void exposesOriginalBetaCountClasses(){
        assertSame(Monster.class,SpawnGroup.MONSTER.getCreatureClass());
        assertSame(AnimalEntity.class,SpawnGroup.CREATURE.getCreatureClass());
        assertSame(WaterCreatureEntity.class,SpawnGroup.WATER_CREATURE.getCreatureClass());
    }
}
