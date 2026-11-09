package legacyconnector.vb1_7_3.net.minecraft.src;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;

/** ScotTools' shared Beta custom-material tool API, retaining its original Item inheritance. */
public class ItemCustomMaterialTool extends Item {
    private Material[] materialEffectiveAgainst;
    public float efficiencyOnProperMaterial;
    public int toolHarvestLevel;
    public boolean toolIsPick;
    private int damageVsEntity;

    public ItemCustomMaterialTool(int id,int attack,int harvestLevel,int durability,float efficiency,
            int materialDamage,boolean pick,Material[] materials){
        super(id);materialEffectiveAgainst=materials;toolHarvestLevel=harvestLevel;maxCount=1;
        setMaxDamage(durability);efficiencyOnProperMaterial=efficiency;damageVsEntity=attack+materialDamage;toolIsPick=pick;
    }
    @Override public float getMiningSpeedMultiplier(ItemStack stack,Block block){
        for(Material material:materialEffectiveAgainst)if(material==block.material)return efficiencyOnProperMaterial;
        return isSuitableFor(block)&&toolIsPick?efficiencyOnProperMaterial:1F;
    }
    @Override public boolean isSuitableFor(Block block){
        if(!toolIsPick)return false;
        // These identity comparisons are the original vanilla block gates, before custom declarations.
        if(block==Block.BLOCKS[49])return toolHarvestLevel>=3;
        if(block==Block.BLOCKS[57]||block==Block.BLOCKS[56])return toolHarvestLevel>=2;
        if(block==Block.BLOCKS[41]||block==Block.BLOCKS[14])return toolHarvestLevel>=2;
        if(block==Block.BLOCKS[42]||block==Block.BLOCKS[15])return toolHarvestLevel>=1;
        if(block==Block.BLOCKS[22]||block==Block.BLOCKS[21])return toolHarvestLevel>=1;
        if(block==Block.BLOCKS[73]||block==Block.BLOCKS[74])return toolHarvestLevel>=2;
        if(CanHarvest.isDeclared(block))return CanHarvest.getBlockLevel(block)<=toolHarvestLevel;
        return block.material==Material.STONE||block.material==Material.METAL;
    }
    @Override public boolean postHit(ItemStack stack,LivingEntity target,LivingEntity attacker){stack.damage(2,attacker);return true;}
    @Override public boolean postMine(ItemStack stack,int blockId,int x,int y,int z,LivingEntity miner){stack.damage(1,miner);return true;}
    public int getAttackDamage(Entity target){return damageVsEntity;}
    @Override public boolean isHandheld(){return true;}
}
