package legacyconnector.vb1_7_3.net.minecraft.src;

import java.util.ArrayList;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;

/** ShockAhPI's explicit tool API; original custom mining and drop callbacks remain virtual. */
public class Tool extends Item {
    public final boolean usingSAPI;
    public ToolBase toolBase;
    public final float baseDamage,basePower,defaultSpeed,toolSpeed;
    public ArrayList<BlockHarvestPower> mineBlocks=new ArrayList<>();
    public ArrayList<Material> mineMaterials=new ArrayList<>();
    public Tool(boolean usingSAPI,ToolBase family,int id,int uses,float damage,float power,float speed){this(usingSAPI,family,id,uses,damage,power,speed,1F);}
    public Tool(boolean usingSAPI,ToolBase family,int id,int uses,float damage,float power,float speed,float defaultSpeed){
        super(id);setMaxDamage(uses);maxCount=1;this.usingSAPI=usingSAPI;toolBase=family;
        baseDamage=damage;basePower=power;toolSpeed=speed;this.defaultSpeed=defaultSpeed;
    }
    @Override public boolean isHandheld(){return true;}
    @Override public boolean postHit(ItemStack stack,LivingEntity target,LivingEntity attacker){stack.damage(2,attacker);return true;}
    @Override public boolean postMine(ItemStack stack,int blockId,int x,int y,int z,LivingEntity miner){stack.damage(1,miner);return true;}
    @Override public int getAttackDamage(Entity target){return (int)Math.floor(baseDamage);}
    public float getPower(){return basePower;}
    @Override public float getMiningSpeedMultiplier(ItemStack stack,Block block){return canHarvest(block)?toolSpeed:defaultSpeed;}
    public boolean canHarvest(Block block){
        if(toolBase!=null&&toolBase.canHarvest(block,getPower()))return true;
        for(Material material:mineMaterials)if(material==block.material)return true;
        for(BlockHarvestPower row:mineBlocks)if(block.id==row.blockID||getPower()>=row.percentage)return true;
        return false;
    }
}
