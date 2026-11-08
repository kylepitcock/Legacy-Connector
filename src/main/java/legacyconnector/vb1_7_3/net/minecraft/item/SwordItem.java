package legacyconnector.vb1_7_3.net.minecraft.item;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;

public class SwordItem extends Item {
    private final int damage;
    public SwordItem(int id,ToolMaterial material) {
        super(id);maxCount=1;setMaxDamage(material.getDurability());damage=4+material.getAttackDamage()*2;
    }
    @Override public float getMiningSpeedMultiplier(ItemStack stack,Block block) { return block.id==30?15:1.5F; }
    @Override public boolean postHit(ItemStack stack,LivingEntity target,LivingEntity attacker) { stack.damage(1,attacker);return true; }
    @Override public boolean postMine(ItemStack stack,int blockId,int x,int y,int z,LivingEntity miner) { stack.damage(2,miner);return true; }
    public int getAttackDamage(legacyconnector.vb1_7_3.net.minecraft.entity.Entity target) { return damage; }
    @Override public boolean isHandheld() { return true; }
    @Override public boolean isSuitableFor(Block block) { return block.id==30; }
}
