package legacyconnector.vb1_7_3.net.minecraft.util.math;
public class ChunkPos {
 public int x,z;
 public ChunkPos(int x,int z){this.x=x;this.z=z;}
 public static int hashCode(int x,int z){return (x<0?Integer.MIN_VALUE:0)|(x&32767)<<16|(z<0?32768:0)|(z&32767);}
 @Override public int hashCode(){return hashCode(x,z);}
 @Override public boolean equals(Object o){return o instanceof ChunkPos p&&p.x==x&&p.z==z;}
}
