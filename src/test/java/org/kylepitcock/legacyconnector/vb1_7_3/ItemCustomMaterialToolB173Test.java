package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.src.CanHarvest;
import legacyconnector.vb1_7_3.net.minecraft.src.ItemCustomMaterialTool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemCustomMaterialToolB173Test {
    private Block[] previousBlocks;private Item previousItem;private int[] contentMark;
    @BeforeEach void before(){previousBlocks=Block.BLOCKS.clone();previousItem=Item.ITEMS[31000];contentMark=ContentB173.mark();new CanHarvest();}
    @AfterEach void after(){ContentB173.release(contentMark);Item.ITEMS[31000]=previousItem;System.arraycopy(previousBlocks,0,Block.BLOCKS,0,previousBlocks.length);new CanHarvest();}
    private static Block block(int id,Material material){
        // Use the same constructor-free vanilla views as the runtime; no native registries/FML are involved.
        try{
            var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
            var block=(Block)((sun.misc.Unsafe)field.get(null)).allocateInstance(Block.class);
            block.mcc$initVanilla(id,0,material,1,null,"fixture");return block;
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    private static ItemCustomMaterialTool tool(int level,boolean pick,Material... materials){return new ItemCustomMaterialTool(31000-256,4,level,32,12F,1,pick,materials);}
    @Test void constructorRetainsRealItemIdentityAndInheritedFluentApi()throws ReflectiveOperationException {
        var tool=tool(2,false,Material.WOOD);assertEquals(31000,tool.id);assertSame(tool,Item.ITEMS[31000]);
        assertEquals(1,tool.getMaxCount());assertEquals(32,tool.getMaxDamage());assertEquals(12F,tool.efficiencyOnProperMaterial);
        assertEquals(2,tool.toolHarvestLevel);assertFalse(tool.toolIsPick);assertEquals(5,tool.getAttackDamage(null));assertTrue(tool.isHandheld());
        assertSame(tool,tool.setTranslationKey("sharedTool"));assertEquals("item.sharedTool",tool.getTranslationKey());
        assertEquals(Item.class,ItemCustomMaterialTool.class.getMethod("setTranslationKey",String.class).getDeclaringClass());
    }
    @Test void listedMaterialSpeedIsIndependentOfPickHarvesting(){
        var tool=tool(0,false,Material.WOOD,Material.PLANT);Block wood=block(200,Material.WOOD),plant=block(201,Material.PLANT),stone=block(202,Material.STONE);
        assertEquals(12F,tool.getMiningSpeedMultiplier(null,wood));assertEquals(12F,tool.getMiningSpeedMultiplier(null,plant));
        assertEquals(1F,tool.getMiningSpeedMultiplier(null,stone));assertFalse(tool.isSuitableFor(wood));assertFalse(tool.isSuitableFor(stone));
    }
    @Test void originalVanillaOreLevelsPrecedeMaterialAndCustomRules(){
        int[][] gates={{49,3},{57,2},{56,2},{41,2},{14,2},{42,1},{15,1},{22,1},{21,1},{73,2},{74,2}};
        var tool=tool(0,true);
        for(var gate:gates){var block=block(gate[0],Material.STONE);Block.BLOCKS[gate[0]]=block;CanHarvest.addBlocks(new Block[]{block},1);
            tool.toolHarvestLevel=gate[1]-1;assertFalse(tool.isSuitableFor(block),"under-level ore "+gate[0]);
            tool.toolHarvestLevel=gate[1];assertTrue(tool.isSuitableFor(block),"matching ore level "+gate[0]);}
        Block stone=block(210,Material.STONE),metal=block(211,Material.METAL),wood=block(212,Material.WOOD);
        tool.toolHarvestLevel=0;assertTrue(tool.isSuitableFor(stone));assertTrue(tool.isSuitableFor(metal));assertFalse(tool.isSuitableFor(wood));
        assertEquals(12F,tool.getMiningSpeedMultiplier(null,stone));
    }
    @Test void customDeclarationsAndOriginalLevelSixLookupQuirkRemainShared(){
        var custom=block(220,Material.WOOD);var tool=tool(2,true);
        CanHarvest.addBlocks(new Block[]{custom},3);assertTrue(CanHarvest.isDeclared(custom));assertEquals(3,CanHarvest.getBlockLevel(custom));
        assertFalse(tool.isSuitableFor(custom));tool.toolHarvestLevel=3;assertTrue(tool.isSuitableFor(custom));assertEquals(12F,tool.getMiningSpeedMultiplier(null,custom));
        CanHarvest.addBlocks(new Block[]{custom},2);assertEquals(3,CanHarvest.getBlockLevel(custom),"highest declared level wins");
        var levelSix=block(221,Material.WOOD);CanHarvest.addBlocks(new Block[]{levelSix},6);
        assertTrue(CanHarvest.isDeclared(levelSix));assertEquals(0,CanHarvest.getBlockLevel(levelSix),"original v10.1 lookup loop excludes list six");
        assertNotNull(CanHarvest.getInstance());new CanHarvest();assertFalse(CanHarvest.isDeclared(custom));
        CanHarvest.addBlocks(new Block[]{custom},0);assertFalse(CanHarvest.isDeclared(custom));
        assertThrows(RuntimeException.class,()->CanHarvest.addBlocks(new Block[]{null},1));
    }
    @Test void originalHitAndMiningCallbacksWearActualStack(){
        var tool=new ItemCustomMaterialTool(31000-256,2,0,5,4F,3,false,new Material[0]);var stack=new ItemStack(tool);
        assertTrue(tool.postHit(stack,null,null));assertEquals(2,stack.getDamage());
        assertTrue(tool.postMine(stack,1,0,0,0,null));assertEquals(3,stack.getDamage());
        tool.postHit(stack,null,null);assertEquals(5,stack.getDamage());assertEquals(1,stack.count);
        tool.postMine(stack,1,0,0,0,null);assertEquals(0,stack.count);assertEquals(0,stack.getDamage());
    }
}
