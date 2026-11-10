package org.kylepitcock.legacyconnector.vb1_7_3.dimension;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome;
import org.junit.jupiter.api.Test;

class BiomeIdentityB173Test {
    private static final class CustomBiome extends Biome {
        CustomBiome(String name) { this.name = name; }
    }

    @Test void savedBiomeIdentitySurvivesBlockRemappingAndConfigurationChanges() {
        var original = new CustomBiome("Highlands");
        var reloaded = new CustomBiome("Highlands");
        reloaded.topBlockId = (byte) 240;
        reloaded.soilBlockId = (byte) 241;
        reloaded.grassColor = 0xabcdef;
        reloaded.spawnablePassive.add(new legacyconnector.vb1_7_3.net.minecraft.world.biome.EntitySpawnGroup(
                legacyconnector.vb1_7_3.net.minecraft.entity.passive.CowEntity.class, 17));
        assertEquals(DimensionRegistrationsB173.fingerprint(original),
                DimensionRegistrationsB173.fingerprint(reloaded));
        assertNotEquals(DimensionRegistrationsB173.fingerprint(original),
                DimensionRegistrationsB173.fingerprint(new CustomBiome("Lowlands")));
    }
    @Test void sharedOriginalBiomeResolvesEveryScopedDimensionKeyAndRecreatedIdentity(){
        var shared=new CustomBiome("Highlands");var discovered=new java.util.LinkedHashMap<net.minecraft.resources.ResourceLocation,Biome>();
        discovered.put(net.minecraft.resources.ResourceLocation.parse("fixture:dimension_2/highlands"),shared);
        discovered.put(net.minecraft.resources.ResourceLocation.parse("fixture:dimension_3/highlands"),shared);
        discovered.put(net.minecraft.resources.ResourceLocation.parse("fixture:dimension_3/lowlands"),new CustomBiome("Lowlands"));
        var expected=java.util.List.of("fixture:dimension_2/highlands","fixture:dimension_3/highlands");
        assertEquals(expected,DimensionRegistrationsB173.biomeIds(shared,discovered));
        assertEquals(expected,DimensionRegistrationsB173.biomeIds(new CustomBiome("Highlands"),discovered));
        assertEquals(java.util.List.of("minecraft:forest"),DimensionRegistrationsB173.biomeIds(Biome.FOREST,discovered));
        assertTrue(DimensionRegistrationsB173.biomeIds(new CustomBiome("Absent"),discovered).isEmpty());
    }
}
