package legacyconnector.vb1_7_3.net.minecraft.item;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;

/** Original shovel type, snow drop predicate and effective-block list. */
public class ShovelItem extends ToolItem {
    public ShovelItem(int id,ToolMaterial material){this(id,material,false);}
    private ShovelItem(int id,ToolMaterial material,boolean vanillaView){super(id,1,material,effectiveBlocks(2,3,12,13,78,80,82,60),vanillaView);}
    public static ShovelItem mcc$vanilla(int id,ToolMaterial material){return new ShovelItem(id,material,true);}
    @Override public boolean isSuitableFor(Block block){return block.id==78||block.id==80;}
}
