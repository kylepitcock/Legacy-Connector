package legacyconnector.vb1_7_3.net.minecraft.entity.ai.pathing;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d;

/** Beta cursor over a native path; original custom controllers may advance the cursor themselves. */
public class Path {
    public final PathNode[] nodes;
    public int length,currentNodeIndex;
    private final net.minecraft.world.level.pathfinder.Path modern;
    public Path(PathNode[] nodes){this(nodes,null);}
    private Path(PathNode[] nodes,net.minecraft.world.level.pathfinder.Path modern){this.nodes=nodes;length=nodes.length;this.modern=modern;}
    public static Path mcc$wrap(net.minecraft.world.level.pathfinder.Path path){
        // Beta PathNodeNavigator returns null when its closest node remains the start.
        // Native A* represents that failure as an unreached singleton instead. A
        // reached singleton (already at the goal) and useful partial routes remain valid.
        if(path==null||path.getNodeCount()==0||(!path.canReach()&&path.getNodeCount()==1))return null;
        PathNode[] nodes=new PathNode[path.getNodeCount()];
        for(int i=0;i<nodes.length;i++){var n=path.getNode(i);nodes[i]=new PathNode(n.x,n.y,n.z);}
        Path wrapped=new Path(nodes,path);wrapped.currentNodeIndex=path.getNextNodeIndex();return wrapped;
    }
    public net.minecraft.world.level.pathfinder.Path mcc$modern(){if(modern!=null)modern.setNextNodeIndex(Math.min(currentNodeIndex,length));return modern;}
    public void next(){if(currentNodeIndex<length)currentNodeIndex++;if(modern!=null)modern.setNextNodeIndex(currentNodeIndex);}
    public boolean isFinished(){if(modern!=null)currentNodeIndex=modern.getNextNodeIndex();return currentNodeIndex>=length;}
    public PathNode getEnd(){return length==0?null:nodes[length-1];}
    public Vec3d getNodePosition(Entity entity){if(isFinished())return null;var node=nodes[currentNodeIndex];double offset=(int)(entity.width+1F)*.5;return Vec3d.create(node.x+offset,node.y,node.z+offset);}
}
