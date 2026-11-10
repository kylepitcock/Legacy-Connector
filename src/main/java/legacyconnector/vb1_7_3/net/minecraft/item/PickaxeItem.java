package legacyconnector.vb1_7_3.net.minecraft.item;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;

/** Original pickaxe type, tier predicates and effective blocks; native vanilla items remain native. */
public class PickaxeItem extends ToolItem {
    protected PickaxeItem(int id,ToolMaterial material){this(id,material,false);}
    private PickaxeItem(int id,ToolMaterial material,boolean vanillaView){super(id,2,material,effectiveBlocks(4,43,44,1,24,48,15,42,16,41,14,56,57,79,87,21,22),vanillaView);}
    public static PickaxeItem mcc$vanilla(int id,ToolMaterial material){return new PickaxeItem(id,material,true);}
    @Override public boolean isSuitableFor(Block block){
        int level=toolMaterial.getMiningLevel();
        if(block.id==49)return level==3;
        if(block.id==57||block.id==56||block.id==41||block.id==14||block.id==73||block.id==74)return level>=2;
        if(block.id==42||block.id==15||block.id==22||block.id==21)return level>=1;
        return block.material==Material.STONE||block.material==Material.METAL;
    }
}
