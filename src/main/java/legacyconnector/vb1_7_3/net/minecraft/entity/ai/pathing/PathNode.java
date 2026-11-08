package legacyconnector.vb1_7_3.net.minecraft.entity.ai.pathing;

/** Beta path node coordinates and search bookkeeping. */
public class PathNode {
    public final int x,y,z,hashCode;
    public int heapIndex=-1;
    public float penalizedPathLength,distanceToNearestTarget,heapWeight;
    public PathNode previous;
    public boolean visited;
    public PathNode(int x,int y,int z){this.x=x;this.y=y;this.z=z;hashCode=hash(x,y,z);}
    public static int hash(int x,int y,int z){return y&255|(x&32767)<<8|(z&32767)<<24|(x<0?Integer.MIN_VALUE:0)|(z<0?32768:0);}
    public float getDistance(PathNode node){double dx=x-node.x,dy=y-node.y,dz=z-node.z;return (float)Math.sqrt(dx*dx+dy*dy+dz*dz);}
    @Override public boolean equals(Object other){return other instanceof PathNode n&&n.x==x&&n.y==y&&n.z==z;}
    @Override public int hashCode(){return hashCode;}
    public boolean isInHeap(){return heapIndex>=0;}
    @Override public String toString(){return x+", "+y+", "+z;}
}
