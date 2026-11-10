package legacyconnector.vb1_7_3.net.minecraft.item;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.src.BlockHarvestPower;
import legacyconnector.vb1_7_3.net.minecraft.src.Tool;
import legacyconnector.vb1_7_3.net.minecraft.src.ToolBase;

public class ToolItem extends Tool {
    private final Block[] effectiveOnBlocks;
    protected float miningSpeed;
    private final int damage;
    protected ToolMaterial toolMaterial;
    protected ToolItem(int id,int attack,ToolMaterial material,Block[] blocks) {
        super(false,null,id,material.getDurability(),attack+material.getAttackDamage(),getToolPower(material),material.getMiningSpeedMultiplier());
        toolMaterial=material;effectiveOnBlocks=blocks;miningSpeed=material.getMiningSpeedMultiplier();
        damage=attack+material.getAttackDamage();
        toolBase=getToolBase();
        if(blocks!=null)for(Block block:blocks)if(block!=null)mineBlocks.add(new BlockHarvestPower(block.id,0));
    }
    /** A non-null original override opts this tool into shared SAPI tables; ordinary Beta tools keep their existing speed. */
    public ToolBase getToolBase(){return null;}
    public static float getToolPower(ToolMaterial material){return material==ToolMaterial.DIAMOND?80:material==ToolMaterial.IRON?60:material==ToolMaterial.STONE?40:20;}
    @Override public float getMiningSpeedMultiplier(ItemStack stack,Block block) {
        if(usingSAPI||toolBase!=null)return super.getMiningSpeedMultiplier(stack,block);
        if(effectiveOnBlocks!=null)for(Block b:effectiveOnBlocks)if(b==block)return miningSpeed;
        return 1;
    }
    @Override public int getAttackDamage(legacyconnector.vb1_7_3.net.minecraft.entity.Entity target){return damage;}
}
