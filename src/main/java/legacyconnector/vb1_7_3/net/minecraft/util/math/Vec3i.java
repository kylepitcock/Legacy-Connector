package legacyconnector.vb1_7_3.net.minecraft.util.math;

/** Beta integer position ({@code ChunkCoordinates}); mutable coordinates with value equality. */
public class Vec3i implements Comparable<Vec3i> {
    public int x,y,z;
    public Vec3i(){}
    public Vec3i(int x,int y,int z){this.x=x;this.y=y;this.z=z;}
    public Vec3i(Vec3i other){this(other.x,other.y,other.z);}
    @Override public boolean equals(Object other){return other instanceof Vec3i p&&x==p.x&&y==p.y&&z==p.z;}
    @Override public int hashCode(){return (x+z)<<(8+y)<<16;}
    @Override public int compareTo(Vec3i other){return y==other.y?(z==other.z?x-other.x:z-other.z):y-other.y;}
    public double distanceTo(int x,int y,int z){int dx=this.x-x,dy=this.y-y,dz=this.z-z;return Math.sqrt(dx*dx+dy*dy+dz*dz);}
}
