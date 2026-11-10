package legacyconnector.vb1_7_3.net.minecraft.src;

/** ShockAhPI r8 compares harvest rows by block ID, independently of their power threshold. */
public class BlockHarvestPower {
    public final int blockID;
    public final float percentage;
    public BlockHarvestPower(int blockID,float percentage){this.blockID=blockID;this.percentage=percentage;}
    @Override public boolean equals(Object other){
        if(other instanceof BlockHarvestPower row)return blockID==row.blockID;
        return other instanceof Integer id&&blockID==id;
    }
}
