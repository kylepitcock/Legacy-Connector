package legacyconnector.vb1_7_3.net.minecraft.item;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;

/** Original Beta shears type and block-specific mining rules. */
public class ShearsItem extends Item {
    public ShearsItem(int id){super(id);setMaxCount(1);setMaxDamage(238);}
    @Override public boolean postMine(ItemStack stack,int blockId,int x,int y,int z,LivingEntity user){
        if(blockId==18||blockId==30)stack.damage(1,user);
        return super.postMine(stack,blockId,x,y,z,user);
    }
    @Override public boolean isSuitableFor(Block block){return block.id==30;}
    @Override public float getMiningSpeedMultiplier(ItemStack stack,Block block){
        if(block.id==30||block.id==18)return 15;
        if(block.id==35)return 5;
        return super.getMiningSpeedMultiplier(stack,block);
    }
}
