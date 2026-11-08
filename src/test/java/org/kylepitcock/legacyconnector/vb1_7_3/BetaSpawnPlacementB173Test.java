package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class BetaSpawnPlacementB173Test {
    @Test void originalFullCubeMetadataCoversEveryVanillaBlock()throws Exception{
        try(var in=getClass().getResourceAsStream("/legacyconnector/b1.7.3/vanilla-ids.tsv")){
            assertNotNull(in);int blocks=0;
            for(var line:new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split("\n")){
                if(!line.startsWith("block\t"))continue;
                assertNotNull(VanillaB173.fullCube(Integer.parseInt(line.split("\t")[2])));blocks++;
            }
            assertEquals(96,blocks);
        }
        for(int id:new int[]{1,43,88})assertEquals(Boolean.TRUE,VanillaB173.fullCube(id));
        for(int id:new int[]{29,30,33,34,36,44})assertEquals(Boolean.FALSE,VanillaB173.fullCube(id));
        assertNull(VanillaB173.fullCube(97));
    }
    @Test void weightedChoicePrecedesFixedHeightOriginDraws(){
        var bounds=new ArrayList<Integer>();IntUnaryOperator random=bound->{bounds.add(bound);return bound-1;};
        var roll=BetaSpawnPlacementB173.roll(()->random.applyAsInt(7),random,new ChunkPos(-2,3));
        assertEquals(List.of(7,16,128,16),bounds);assertEquals(6,roll.choice());assertEquals(new BlockPos(-17,127,63),roll.origin());
        bounds.clear();assertNull(BetaSpawnPlacementB173.roll(()->null,random,new ChunkPos(0,0)));assertTrue(bounds.isEmpty());
    }
    @Test void walkConsumesBothZeroHeightDrawsInOriginalOrder(){
        var bounds=new ArrayList<Integer>();int[] draws={5,0,0,0,0,5};int[] index={0};
        var position=BetaSpawnPlacementB173.walk(new BlockPos(0,127,0),bound->{bounds.add(bound);return draws[index[0]++];});
        assertEquals(List.of(6,6,1,1,6,6),bounds);assertEquals(new BlockPos(5,127,-5),position);
    }
    @Test void originRequiresExactGroupMaterialBeforeCandidateWalk(){
        assertTrue(BetaSpawnPlacementB173.originAllowed(SpawnGroup.CREATURE,Material.AIR,false));
        assertFalse(BetaSpawnPlacementB173.originAllowed(SpawnGroup.CREATURE,Material.PLANT,false));
        assertFalse(BetaSpawnPlacementB173.originAllowed(SpawnGroup.CREATURE,Material.AIR,true));
        assertTrue(BetaSpawnPlacementB173.originAllowed(SpawnGroup.WATER_CREATURE,Material.WATER,false));
        assertFalse(BetaSpawnPlacementB173.originAllowed(SpawnGroup.WATER_CREATURE,Material.LAVA,false));
    }
    @Test void candidateGeometryUsesSolidFloorAndTwoClearCellsWithOriginalFluidPolicy(){
        assertTrue(BetaSpawnPlacementB173.candidateAllowed(SpawnGroup.CREATURE,Material.PLANT,true,false,false));
        assertFalse(BetaSpawnPlacementB173.candidateAllowed(SpawnGroup.CREATURE,Material.AIR,false,false,false));
        assertFalse(BetaSpawnPlacementB173.candidateAllowed(SpawnGroup.CREATURE,Material.AIR,true,true,false));
        assertFalse(BetaSpawnPlacementB173.candidateAllowed(SpawnGroup.CREATURE,Material.AIR,true,false,true));
        assertFalse(BetaSpawnPlacementB173.candidateAllowed(SpawnGroup.CREATURE,Material.WATER,true,false,false));
        assertTrue(BetaSpawnPlacementB173.candidateAllowed(SpawnGroup.WATER_CREATURE,Material.WATER,false,false,false));
        assertTrue(BetaSpawnPlacementB173.candidateAllowed(SpawnGroup.WATER_CREATURE,Material.LAVA,false,false,false));
        assertFalse(BetaSpawnPlacementB173.candidateAllowed(SpawnGroup.WATER_CREATURE,Material.WATER,false,false,true));
        assertFalse(BetaSpawnPlacementB173.candidateAllowed(SpawnGroup.WATER_CREATURE,Material.AIR,false,false,false));
    }
    @Test void spawnPointDistanceIncludesExactlyTwentyFourBlocks(){
        assertFalse(BetaSpawnPlacementB173.spawnDistanceAllowed(575.999));assertTrue(BetaSpawnPlacementB173.spawnDistanceAllowed(576));
    }
}
