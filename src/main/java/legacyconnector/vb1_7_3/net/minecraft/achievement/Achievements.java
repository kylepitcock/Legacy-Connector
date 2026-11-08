package legacyconnector.vb1_7_3.net.minecraft.achievement;

import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;

/** Vanilla parent identity used by legacy custom achievement chains. */
public final class Achievements {
    public static int minColumn,minRow,maxColumn,maxRow;
    public static final java.util.List<Achievement> ACHIEVEMENTS=new java.util.ArrayList<>();
    public static final Achievement OPEN_INVENTORY=inventory();
    private static Achievement inventory(){var a=new Achievement(0,"openInventory",0,0,new ItemStack(340,1,0),null);a.mcc$vanilla=true;ACHIEVEMENTS.add(a);return a;}
    private Achievements(){}
}
