package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.MapColor;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.item.*;
import legacyconnector.vb1_7_3.net.minecraft.src.*;
import org.junit.jupiter.api.Test;

class ShockAhToolB173Test {
    private static int nextId=31000;
    private static Block block(int id,Material material)throws Exception{
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        var block=(Block)((sun.misc.Unsafe)field.get(null)).allocateInstance(Block.class);block.id=id;block.material=material;return block;
    }
    private static class Ordinary extends ToolItem {
        Ordinary(Block... blocks){super(nextId++,2,ToolMaterial.IRON,blocks);}
    }
    private static final class OriginalOverride extends Ordinary {
        private static final ToolBase FAMILY=new ToolBase();
        OriginalOverride(){super();}
        @Override public ToolBase getToolBase(){return FAMILY;}
        @Override public float getPower(){return -1;}
        @Override public float getMiningSpeedMultiplier(ItemStack stack,Block block){return super.getMiningSpeedMultiplier(stack,block)*2;}
        @Override public boolean isSuitableFor(Block block){return block.id==211;}
    }
    @Test void familyTablesMatchActualR8IdsAndThresholds(){
        assertEquals(List.of(1,4,16,21,22,23,24,43,44,45,48,52,61,62,67,77,79,87,89,93,94,15,42,71,14,41,56,57,73,74,49),
                ToolBase.Pickaxe.mineBlocks.stream().map(row->row.blockID).toList());
        assertEquals(List.of(2,3,12,13,78,80,82),ToolBase.Shovel.mineBlocks.stream().map(row->row.blockID).toList());
        assertEquals(List.of(5,17,25,47,53,54,58,63,64,65,66,68,69,81,84,85),ToolBase.Axe.mineBlocks.stream().map(row->row.blockID).toList());
        for(int i=0;i<31;i++)assertEquals(i<21?20F:i<24?40F:i<30?60F:80F,ToolBase.Pickaxe.mineBlocks.get(i).percentage);
        assertTrue(ToolBase.Pickaxe.mineMaterials.isEmpty());assertTrue(ToolBase.Shovel.mineMaterials.isEmpty());assertTrue(ToolBase.Axe.mineMaterials.isEmpty());
    }
    @Test void r8PredicateUsesIdentityOrIdOrPowerIncludingIeeeComparisons()throws Exception{
        var material=new Material(MapColor.GRAY);var target=block(211,material);var family=new ToolBase();
        assertFalse(family.canHarvest(target,Float.POSITIVE_INFINITY));
        family.mineBlocks.add(new BlockHarvestPower(212,60));
        assertFalse(family.canHarvest(target,59));assertTrue(family.canHarvest(target,60));
        assertFalse(family.canHarvest(target,Float.NaN));assertTrue(family.canHarvest(target,Float.POSITIVE_INFINITY));
        family.mineBlocks.add(new BlockHarvestPower(211,80));
        assertTrue(family.canHarvest(target,Float.NEGATIVE_INFINITY));assertTrue(family.canHarvest(target,Float.NaN));
        family.mineBlocks.clear();family.mineMaterials.add(new Material(MapColor.GRAY));assertFalse(family.canHarvest(target,100));
        family.mineMaterials.add(material);assertTrue(family.canHarvest(target,-100));
        var row=new BlockHarvestPower(211,80);assertEquals(row,new BlockHarvestPower(211,0));assertTrue(row.equals(211));assertFalse(row.equals(null));assertFalse(row.equals("211"));
    }
    @Test void virtualOptInUsesLiveFamilyTablesAndPreservesCustomSpeedAndDropOverrides()throws Exception{
        var target=block(211,Material.STONE);var other=block(212,Material.STONE);
        OriginalOverride.FAMILY.mineBlocks.clear();var tool=new OriginalOverride();
        assertSame(OriginalOverride.FAMILY,tool.toolBase);assertEquals(2,tool.getMiningSpeedMultiplier(null,target));
        OriginalOverride.FAMILY.mineBlocks.add(new BlockHarvestPower(211,80));
        try{
            assertEquals(12,tool.getMiningSpeedMultiplier(null,target));assertEquals(2,tool.getMiningSpeedMultiplier(null,other));
            assertTrue(tool.isSuitableFor(target));assertFalse(tool.isSuitableFor(other));
        }finally{OriginalOverride.FAMILY.mineBlocks.clear();}
    }
    @Test void ordinaryToolsKeepVanillaSpeedAndNativeMeleeContribution()throws Exception{
        var target=block(211,Material.STONE);var other=block(212,Material.STONE);var ordinary=new Ordinary(target);
        assertNull(ordinary.toolBase);assertEquals(6,ordinary.getMiningSpeedMultiplier(null,target));assertEquals(1,ordinary.getMiningSpeedMultiplier(null,other));
        assertFalse(ordinary.isSuitableFor(target));assertEquals(4,ordinary.getAttackDamage(null));
        assertEquals(3,ItemCallbacksB173.attackDamageBonus(ordinary,null,1));
        assertFalse(new ItemCallbacksB173(ordinary).canPerformAction(net.neoforged.neoforge.common.ItemAbilities.PICKAXE_DIG));
        var explicit=new Tool(true,ToolBase.Axe,nextId++,100,3.9F,20,7);
        assertEquals(3,explicit.getAttackDamage(null));assertEquals(20,explicit.getPower());
        var callbacks=new ItemCallbacksB173(explicit);
        assertTrue(callbacks.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.AXE_DIG));
        assertFalse(callbacks.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.AXE_STRIP));
        assertFalse(callbacks.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.PICKAXE_DIG));
        assertEquals(80,ToolItem.getToolPower(ToolMaterial.DIAMOND));assertEquals(60,ToolItem.getToolPower(ToolMaterial.IRON));
        assertEquals(40,ToolItem.getToolPower(ToolMaterial.STONE));assertEquals(20,ToolItem.getToolPower(ToolMaterial.GOLD));
    }
}
