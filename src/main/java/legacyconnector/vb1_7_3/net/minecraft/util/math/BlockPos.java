package legacyconnector.vb1_7_3.net.minecraft.util.math;

/** Beta immutable integer position, also used as explosion affected-block set keys. */
public class BlockPos {
    public final int x,y,z;
    public BlockPos(int x,int y,int z){this.x=x;this.y=y;this.z=z;}
    @Override public boolean equals(Object other){return other instanceof BlockPos p&&x==p.x&&y==p.y&&z==p.z;}
    @Override public int hashCode(){return (x*31+y)*31+z;}
}
