package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.EnumSet;
import com.mojang.serialization.Lifecycle;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BlockHeightmapsB173Test {
    @BeforeAll static void bootstrap()throws Exception{NativeStackComponentsB173Test.bootstrap();}
    private static ProtoChunk chunk(){
        // A private biome registry supplies the real native section palette; global registries remain frozen.
        var biomes=new MappedRegistry<Biome>(Registries.BIOME,Lifecycle.stable());
        var biome=new Biome.BiomeBuilder().hasPrecipitation(true).temperature(.8F).downfall(.4F)
                .specialEffects(new BiomeSpecialEffects.Builder().fogColor(1).waterColor(1).waterFogColor(1).skyColor(1).build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY).generationSettings(BiomeGenerationSettings.EMPTY).build();
        Registry.register(biomes,Biomes.PLAINS,biome);biomes.freeze();
        return new ProtoChunk(new ChunkPos(0,0),UpgradeData.EMPTY,LevelHeightAccessor.create(-64,384),biomes,null);
    }
    @Test void staleSavedRawMapsRecalculateFromNativePredicatesAndSecondPassIsUnchanged(){
        var chunk=chunk();var solid=LegacyBlockSolidityB173Test.state(Material.SOIL,true,1,true);
        chunk.setBlockState(new BlockPos(0,80,0),solid,false);
        chunk.setBlockState(new BlockPos(1,75,0),LegacyBlockSolidityB173Test.state(Material.WOOD,false,.5F,false),false);
        chunk.setBlockState(new BlockPos(2,78,0),Blocks.WATER.defaultBlockState(),false);
        var types=EnumSet.of(Heightmap.Types.MOTION_BLOCKING,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Heightmap.Types.OCEAN_FLOOR);
        for(var type:types){
            var map=chunk.getOrCreateHeightmapUnprimed(type);Arrays.fill(map.getRawData(),0);
            // Seed a stale HIGH column as well as old all-zero solid columns; empty must reset to minY.
            map.update(15,200,15,Blocks.STONE.defaultBlockState());
        }
        var surface=chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE);surface.update(0,99,0,Blocks.STONE.defaultBlockState());
        var preserved=surface.getRawData().clone();assertTrue(BlockHeightmapsB173.refresh(chunk));
        for(var type:types){var map=chunk.getOrCreateHeightmapUnprimed(type);assertEquals(81,map.getFirstAvailable(0,0));assertEquals(76,map.getFirstAvailable(1,0));assertEquals(-64,map.getFirstAvailable(15,15));}
        assertEquals(79,chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING).getFirstAvailable(2,0));
        assertEquals(-64,chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR).getFirstAvailable(2,0));
        assertArrayEquals(preserved,surface.getRawData());
        chunk.setUnsaved(false);assertFalse(BlockHeightmapsB173.refresh(chunk));assertFalse(chunk.isUnsaved());
    }
    @Test void vanillaAndNonmovingBetaPalettesDoNotTouchExistingMaps(){
        for(boolean beta:new boolean[]{false,true}){
            var chunk=chunk();chunk.setBlockState(new BlockPos(0,80,0),beta
                    ?LegacyBlockSolidityB173Test.state(Material.PLANT,true,1,false):Blocks.STONE.defaultBlockState(),false);
            var map=chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING);Arrays.fill(map.getRawData(),0);var before=map.getRawData().clone();
            chunk.setUnsaved(false);assertFalse(BlockHeightmapsB173.refresh(chunk));assertArrayEquals(before,map.getRawData());assertFalse(chunk.isUnsaved());
        }
    }
}
