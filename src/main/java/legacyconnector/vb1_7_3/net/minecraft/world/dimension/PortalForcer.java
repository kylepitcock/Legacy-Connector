package legacyconnector.vb1_7_3.net.minecraft.world.dimension;

import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;

/** Default Beta portal placement; installed subclasses retain virtual search/create dispatch. */
public class PortalForcer {
    private final java.util.Random random=new java.util.Random();
    private static final int PORTAL=90,OBSIDIAN=49;
    public void moveToPortal(World world,Entity entity){
        if(!teleportToValidPortal(world,entity)){createPortal(world,entity);teleportToValidPortal(world,entity);}
    }
    public boolean teleportToValidPortal(World world,Entity entity){
        int centerX=(int)Math.floor(entity.x),centerZ=(int)Math.floor(entity.z);
        int bestX=0,bestY=0,bestZ=0;double best=-1;
        for(int x=centerX-128;x<=centerX+128;x++)for(int z=centerZ-128;z<=centerZ+128;z++)for(int y=127;y>=0;y--){
            if(world.getBlockId(x,y,z)!=PORTAL)continue;
            while(world.getBlockId(x,y-1,z)==PORTAL)y--;
            double distance=distance(entity,x,y,z);
            if(best<0||distance<best){best=distance;bestX=x;bestY=y;bestZ=z;}
        }
        if(best<0)return false;
        double x=bestX+.5,z=bestZ+.5;
        if(world.getBlockId(bestX-1,bestY,bestZ)==PORTAL)x-=.5;
        if(world.getBlockId(bestX+1,bestY,bestZ)==PORTAL)x+=.5;
        if(world.getBlockId(bestX,bestY,bestZ-1)==PORTAL)z-=.5;
        if(world.getBlockId(bestX,bestY,bestZ+1)==PORTAL)z+=.5;
        entity.setPositionAndAnglesKeepPrevAngles(x,bestY+.5,z,entity.yaw,0);
        entity.setVelocityClient(0,0,0);return true;
    }
    private static double distance(Entity entity,int x,int y,int z){
        double dx=x+.5-entity.x,dy=y+.5-entity.y,dz=z+.5-entity.z;return dx*dx+dy*dy+dz*dz;
    }
    private static boolean clear(World world,int x,int y,int z,int dx,int dz,int depth){
        for(int across=0;across<depth;across++)for(int column=0;column<4;column++)for(int row=-1;row<4;row++){
            int px=x+(column-1)*dx+across*dz,pz=z+(column-1)*dz-across*dx;
            if(row<0?!world.getMaterial(px,y+row,pz).isSolid():!world.isAir(px,y+row,pz))return false;
        }return true;
    }
    public boolean createPortal(World world,Entity entity){
        int centerX=(int)Math.floor(entity.x),centerY=(int)Math.floor(entity.y),centerZ=(int)Math.floor(entity.z);
        int bestX=centerX,bestY=centerY,bestZ=centerZ,orientation=0,start=random.nextInt(4);double best=-1;
        // First prefer a three-block-deep clearance, then accept a single clear portal plane.
        for(int pass=0;pass<2;pass++){
            if(pass==1&&best>=0)break;
            for(int x=centerX-16;x<=centerX+16;x++)for(int z=centerZ-16;z<=centerZ+16;z++){
                candidate:for(int y=127;y>=0;y--){
                    if(!world.isAir(x,y,z))continue;
                    while(y>0&&world.isAir(x,y-1,z))y--;
                    for(int direction=start;direction<start+(pass==0?4:2);direction++){
                        int dx=direction%2,dz=1-dx;
                        if(pass==0&&direction%4>=2){dx=-dx;dz=-dz;}
                        if(!clear(world,x,y,z,dx,dz,pass==0?3:1))continue candidate;
                        double distance=distance(entity,x,y,z);
                        if(best<0||distance<best){best=distance;bestX=x;bestY=y;bestZ=z;orientation=direction%(pass==0?4:2);}
                    }
                }
            }
        }
        int dx=orientation%2,dz=1-dx;if(orientation%4>=2){dx=-dx;dz=-dz;}
        if(best<0){
            bestY=Math.max(70,Math.min(118,bestY));
            for(int across=-1;across<=1;across++)for(int column=0;column<2;column++)for(int row=-1;row<3;row++)
                world.setBlock(bestX+column*dx+across*dz,bestY+row,bestZ+column*dz-across*dx,row<0?OBSIDIAN:0);
        }
        for(int repeat=0;repeat<4;repeat++){
            boolean previous=world.pauseTicking;world.pauseTicking=true;
            try{
                for(int column=0;column<4;column++)for(int row=-1;row<4;row++)
                    world.setBlock(bestX+(column-1)*dx,bestY+row,bestZ+(column-1)*dz,column==0||column==3||row==-1||row==3?OBSIDIAN:PORTAL);
            }finally{world.pauseTicking=previous;}
            for(int column=0;column<4;column++)for(int row=-1;row<4;row++){
                int x=bestX+(column-1)*dx,y=bestY+row,z=bestZ+(column-1)*dz;
                world.notifyNeighbors(x,y,z,world.getBlockId(x,y,z));
            }
        }
        return true;
    }
}
