package legacyconnector.vb1_7_3.net.minecraft.item;

/** Original axe type and effective-block list. */
public class AxeItem extends ToolItem {
    protected AxeItem(int id,ToolMaterial material){this(id,material,false);}
    private AxeItem(int id,ToolMaterial material,boolean vanillaView){super(id,3,material,effectiveBlocks(5,47,17,54),vanillaView);}
    public static AxeItem mcc$vanilla(int id,ToolMaterial material){return new AxeItem(id,material,true);}
}
