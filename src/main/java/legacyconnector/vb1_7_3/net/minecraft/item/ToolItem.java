package legacyconnector.vb1_7_3.net.minecraft.item;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.src.BlockHarvestPower;
import legacyconnector.vb1_7_3.net.minecraft.src.Tool;
import legacyconnector.vb1_7_3.net.minecraft.src.ToolBase;

public class ToolItem extends Tool {
    private final Block[] effectiveOnBlocks;
    protected float miningSpeed;
    private final int damage;
    protected ToolMaterial toolMaterial;
    protected ToolItem(int id,int attack,ToolMaterial material,Block[] blocks) {
        this(id,attack,material,blocks,false);
    }
    protected ToolItem(int id,int attack,ToolMaterial material,Block[] blocks,boolean vanillaView) {
        super(false,null,id,material.getDurability(),attack+material.getAttackDamage(),getToolPower(material),material.getMiningSpeedMultiplier(),1F,vanillaView);
        toolMaterial=material;effectiveOnBlocks=blocks;miningSpeed=material.getMiningSpeedMultiplier();
        damage=attack+material.getAttackDamage();
        toolBase=getToolBase();
        if(blocks!=null)for(Block block:blocks)if(block!=null)mineBlocks.add(new BlockHarvestPower(block.id,0));
    }
    public ToolBase getToolBase(){return this instanceof PickaxeItem?ToolBase.Pickaxe:this instanceof AxeItem?ToolBase.Axe:this instanceof ShovelItem?ToolBase.Shovel:null;}
    protected static Block[] effectiveBlocks(int... ids){var blocks=new Block[ids.length];for(int i=0;i<ids.length;i++)blocks[i]=Block.BLOCKS[ids[i]];return blocks;}
    private boolean listed(int block){
        for(var row:mineBlocks)if(row.blockID==block)return true;
        if(toolBase!=null)for(var row:toolBase.mineBlocks)if(row.blockID==block)return true;
        return false;
    }
    @Override public boolean canHarvest(Block block){
        if(!usingSAPI&&!listed(block.id)){
            if(this instanceof PickaxeItem){
                if(id<=369)return false;
                if(block.material==Material.STONE||block.material==Material.ICE||block.material==Material.METAL&&basePower>=40)return true;
            }else if(this instanceof AxeItem){
                if(id<=369)return false;
                if(block.material==Material.WOOD||block.material==Material.LEAVES||block.material==Material.PLANT||block.material==Material.CACTUS||block.material==Material.PUMPKIN)return true;
            }else if(this instanceof ShovelItem){
                if(id<=369)return false;
                if(block.material==Material.SOLID_ORGANIC||block.material==Material.SOIL||block.material==Material.SAND||block.material==Material.SNOW_LAYER||block.material==Material.SNOW_BLOCK||block.material==Material.CLAY)return true;
            }
        }
        return super.canHarvest(block);
    }
    public static float getToolPower(ToolMaterial material){return material==ToolMaterial.DIAMOND?80:material==ToolMaterial.IRON?60:material==ToolMaterial.STONE?40:20;}
    @Override public float getMiningSpeedMultiplier(ItemStack stack,Block block) {
        if(usingSAPI||toolBase!=null)return super.getMiningSpeedMultiplier(stack,block);
        if(effectiveOnBlocks!=null)for(Block b:effectiveOnBlocks)if(b==block)return miningSpeed;
        return 1;
    }
    @Override public int getAttackDamage(legacyconnector.vb1_7_3.net.minecraft.entity.Entity target){return damage;}
}
