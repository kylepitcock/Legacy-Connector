package legacyconnector.vb1_7_3.net.minecraft.src;

import java.util.ArrayList;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.world.BlockView;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;

/** Immutable original API coordinates; integer access deliberately truncates toward zero. */
public class Loc {
    public final double x,y,z;
    public Loc(){this(0,0,0);}
    public Loc(int x,int z){this(x,0,z);}
    public Loc(int x,int y,int z){this((double)x,y,z);}
    public Loc(double x,double z){this(x,0,z);}
    public Loc(double x,double y,double z){this.x=x;this.y=y;this.z=z;}
    public Loc(World world){this(world.mcc$level().getSharedSpawnPos().getX(),world.mcc$level().getSharedSpawnPos().getY(),world.mcc$level().getSharedSpawnPos().getZ());}
    public int x(){return (int)x;}public int y(){return (int)y;}public int z(){return (int)z;}
    public Loc add(int x,int y,int z){return add((double)x,y,z);}
    public Loc add(double x,double y,double z){return new Loc(this.x+x,this.y+y,this.z+z);}
    public Loc add(Loc other){return add(other.x,other.y,other.z);}
    public Loc subtract(int x,int y,int z){return subtract((double)x,y,z);}
    public Loc subtract(double x,double y,double z){return add(-x,-y,-z);}
    public Loc subtract(Loc other){return subtract(other.x,other.y,other.z);}
    public Loc multiply(double x,double y,double z){return new Loc(this.x*x,this.y*y,this.z*z);}
    public Loc getSide(int side){return switch(side){case 0->add(0,-1,0);case 1->add(0,1,0);case 2->add(0,0,-1);case 3->add(0,0,1);case 4->add(-1,0,0);case 5->add(1,0,0);default->this;};}
    @Override public boolean equals(Object other){return other instanceof Loc value&&x==value.x&&y==value.y&&z==value.z;}
    @Override public int hashCode(){return java.util.Objects.hash(x==0?0D:x,y==0?0D:y,z==0?0D:z);}
    @Override public String toString(){return "("+x+", "+y+", "+z+")";}
    public int distSimple(Loc other){return (int)(Math.abs(x-other.x)+Math.abs(y-other.y)+Math.abs(z-other.z));}
    public double distAdv(Loc other){double dx=x-other.x,dy=y-other.y,dz=z-other.z;return Math.sqrt(dx*dx+dy*dy+dz*dz);}
    public static Loc[] vecAdjacent(){return new Loc[]{new Loc(0,0,1),new Loc(0,0,-1),new Loc(0,1,0),new Loc(0,-1,0),new Loc(1,0,0),new Loc(-1,0,0)};}
    public static Loc[] vecAdjacent2D(){return new Loc[]{new Loc(0,1),new Loc(0,-1),new Loc(1,0),new Loc(-1,0)};}
    private Loc[] translated(Loc[] offsets){for(int i=0;i<offsets.length;i++)offsets[i]=add(offsets[i]);return offsets;}
    public Loc[] adjacent(){return translated(vecAdjacent());}
    // This entrypoint historically returns all six neighbors despite its name.
    public Loc[] adjacent2D(){return translated(vecAdjacent());}
    public static ArrayList<Loc> vecInRadius(int radius,boolean euclidean){return new Loc().inRadius(radius,euclidean);}
    public static ArrayList<Loc> vecInRadius2D(int radius,boolean euclidean){return new Loc().inRadius2D(radius,euclidean);}
    public ArrayList<Loc> inRadius(int radius,boolean euclidean){return radius(radius,euclidean,false);}
    public ArrayList<Loc> inRadius2D(int radius,boolean euclidean){return radius(radius,euclidean,true);}
    private ArrayList<Loc> radius(int radius,boolean euclidean,boolean flat){
        var result=new ArrayList<Loc>();if(radius<0)return result;
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)for(int dy=flat?0:-radius;dy<=(flat?0:radius);dy++){
            var point=add(dx,dy,dz);if((euclidean?distAdv(point):distSimple(point))<=radius)result.add(point);
        }return result;
    }
    public int getBlock(BlockView world){return world.getBlockId(x(),y(),z());}
    public int getMeta(BlockView world){return world.getBlockMeta(x(),y(),z());}
    public BlockEntity getTileEntity(BlockView world){return world.getBlockEntity(x(),y(),z());}
    public Loc setBlockNotify(World world,int id){world.setBlock(x(),y(),z(),id);return this;}
    public Loc setBlock(World world,int id){world.setBlockWithoutNotifyingNeighbors(x(),y(),z(),id);return this;}
    public Loc setMeta(World world,int meta){world.setBlockMetaWithoutNotifyingNeighbors(x(),y(),z(),meta);return this;}
    public Loc setMetaNotify(World world,int meta){world.setBlockMeta(x(),y(),z(),meta);return this;}
    public Loc setBlockAndMeta(World world,int id,int meta){world.setBlockWithoutNotifyingNeighbors(x(),y(),z(),id,meta);return this;}
    public Loc setBlockAndMetaNotify(World world,int id,int meta){world.setBlock(x(),y(),z(),id,meta);return this;}
    public Loc setTileEntity(World world,BlockEntity entity){world.setBlockEntity(x(),y(),z(),entity);return this;}
    public Loc removeTileEntity(World world){world.removeBlockEntity(x(),y(),z());return this;}
    public int getLight(World world){return world.getLightLevel(x(),y(),z());}
    public Loc notify(World world){world.notifyNeighbors(x(),y(),z(),getBlock(world));return this;}
    public Loc setSpawnPoint(World world){
        if(world.mcc$level() instanceof net.minecraft.server.level.ServerLevel level)level.setDefaultSpawnPos(new net.minecraft.core.BlockPos(x(),y(),z()),0);
        return this;
    }
}
