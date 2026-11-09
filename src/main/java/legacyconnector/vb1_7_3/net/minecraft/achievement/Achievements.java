package legacyconnector.vb1_7_3.net.minecraft.achievement;

import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;

/** Original Beta identities needed by legacy parent chains and furnace output callbacks. */
public final class Achievements {
    public static int minColumn,minRow,maxColumn,maxRow;
    public static final java.util.List<Achievement> ACHIEVEMENTS=new java.util.ArrayList<>();
    public static final Achievement OPEN_INVENTORY=vanilla(0,"openInventory",0,0,340,null).localOnly();
    public static final Achievement MINE_WOOD=vanilla(1,"mineWood",2,1,17,OPEN_INVENTORY);
    public static final Achievement BUILD_WORKBENCH=vanilla(2,"buildWorkBench",4,-1,58,MINE_WOOD);
    public static final Achievement BUILD_PICKAXE=vanilla(3,"buildPickaxe",4,2,270,BUILD_WORKBENCH);
    public static final Achievement BUILD_FURNACE=vanilla(4,"buildFurnace",3,4,61,BUILD_PICKAXE);
    public static final Achievement ACQUIRE_IRON=vanilla(5,"acquireIron",1,4,265,BUILD_FURNACE);
    public static final Achievement COOK_FISH=vanilla(10,"cookFish",2,6,350,BUILD_FURNACE);
    private static Achievement vanilla(int id,String name,int column,int row,int icon,Achievement parent){
        var a=new Achievement(id,name,column,row,new ItemStack(icon,1,0),parent,"legacyconnector");
        a.mcc$vanilla=true;
        return a.addStat();
    }
    private Achievements(){}
}
