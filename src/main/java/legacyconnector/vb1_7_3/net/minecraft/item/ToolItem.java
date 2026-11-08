package legacyconnector.vb1_7_3.net.minecraft.item;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;

public class ToolItem extends Item {
    private final Block[] effectiveOnBlocks;
    protected float miningSpeed;
    private final int damage;
    protected ToolMaterial toolMaterial;
    protected ToolItem(int id,int attack,ToolMaterial material,Block[] blocks) {
        super(id);toolMaterial=material;effectiveOnBlocks=blocks;maxCount=1;
        setMaxDamage(material.getDurability());miningSpeed=material.getMiningSpeedMultiplier();damage=attack+material.getAttackDamage();
    }
    @Override public float getMiningSpeedMultiplier(ItemStack stack,Block block) {
        if(effectiveOnBlocks!=null)for(Block b:effectiveOnBlocks)if(b==block)return miningSpeed;
        return 1;
    }
    @Override public boolean postHit(ItemStack stack,LivingEntity target,LivingEntity attacker) { stack.damage(2,attacker);return true; }
    @Override public boolean postMine(ItemStack stack,int blockId,int x,int y,int z,LivingEntity miner) { stack.damage(1,miner);return true; }
    public int getAttackDamage(legacyconnector.vb1_7_3.net.minecraft.entity.Entity target) { return damage; }
    @Override public boolean isHandheld() { return true; }
}
