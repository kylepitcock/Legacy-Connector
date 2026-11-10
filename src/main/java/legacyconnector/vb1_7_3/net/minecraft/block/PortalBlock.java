package legacyconnector.vb1_7_3.net.minecraft.block;

import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Box;
import legacyconnector.vb1_7_3.net.minecraft.world.BlockView;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.vb1_7_3.DimensionsB173;

/** Shared original portal block behavior; subclasses supply their own frame and destination. */
public class PortalBlock extends Block {
    public PortalBlock(int id,int texture){super(id,texture,Material.NETHER_PORTAL);}
    @Override public Box getCollisionShape(World world,int x,int y,int z){return null;}
    @Override public boolean isOpaque(){return false;}
    @Override public boolean isFullCube(){return false;}
    @Override public int getDroppedItemCount(Random random){return 0;}
    @Override public int getRenderLayer(){return 1;}
    @Override public void updateBoundingBox(BlockView world,int x,int y,int z){
        boolean acrossX=world.getBlockId(x-1,y,z)==id||world.getBlockId(x+1,y,z)==id;
        if(acrossX)setBoundingBox(0,0,.375F,1,1,.625F);else setBoundingBox(.375F,0,0,.625F,1,1);
    }
    @Override public boolean isSideVisible(BlockView world,int x,int y,int z,int side){
        if(world.getBlockId(x,y,z)==id)return false;
        boolean acrossX=world.getBlockId(x-1,y,z)==id&&world.getBlockId(x-2,y,z)!=id
                ||world.getBlockId(x+1,y,z)==id&&world.getBlockId(x+2,y,z)!=id;
        boolean acrossZ=world.getBlockId(x,y,z-1)==id&&world.getBlockId(x,y,z-2)!=id
                ||world.getBlockId(x,y,z+1)==id&&world.getBlockId(x,y,z+2)!=id;
        // Coordinates are the neighbour's: a portal beside it along X shows its west/east face, along Z its north/south face.
        return acrossX&&(side==4||side==5)||acrossZ&&(side==2||side==3);
    }
    public boolean create(World world,int x,int y,int z){
        boolean acrossX=world.getBlockId(x-1,y,z)==49||world.getBlockId(x+1,y,z)==49;
        boolean acrossZ=world.getBlockId(x,y,z-1)==49||world.getBlockId(x,y,z+1)==49;
        if(acrossX==acrossZ)return false;
        int dx=acrossX?1:0,dz=acrossZ?1:0;
        if(world.getBlockId(x-dx,y,z-dz)==0){x-=dx;z-=dz;}
        for(int column=-1;column<=2;column++)for(int row=-1;row<=3;row++){
            boolean edge=column==-1||column==2||row==-1||row==3;
            if((column==-1||column==2)&&(row==-1||row==3))continue;
            int block=world.getBlockId(x+dx*column,y+row,z+dz*column);
            if(edge?block!=49:block!=0&&block!=51)return false;
        }
        boolean previous=world.pauseTicking;world.pauseTicking=true;
        try{for(int column=0;column<2;column++)for(int row=0;row<3;row++)world.setBlock(x+dx*column,y+row,z+dz*column,id);}
        finally{world.pauseTicking=previous;}
        return true;
    }
    @Override public void neighborUpdate(World world,int x,int y,int z,int changedId){
        boolean acrossX=world.getBlockId(x-1,y,z)==id||world.getBlockId(x+1,y,z)==id;
        int dx=acrossX?1:0,dz=acrossX?0:1;
        int bottom=y;while(world.getBlockId(x,bottom-1,z)==id)bottom--;
        if(world.getBlockId(x,bottom-1,z)!=49){world.setBlock(x,y,z,0);return;}
        int height=0;while(height<4&&world.getBlockId(x,bottom+height,z)==id)height++;
        boolean frame=height==3&&world.getBlockId(x,bottom+height,z)==49;
        boolean left=world.getBlockId(x-dx,y,z-dz)==49,right=world.getBlockId(x+dx,y,z+dz)==49;
        boolean other=left?world.getBlockId(x+dx,y,z+dz)==id:right&&world.getBlockId(x-dx,y,z-dz)==id;
        if(!frame||!other)world.setBlock(x,y,z,0);
    }
    public int getDimNumber(){return -1;}
    @Override public void onEntityCollision(World world,int x,int y,int z,Entity entity){
        if(!entity.hasVehicle()&&entity.passenger==null)DimensionsB173.portalContact(world,entity,getDimNumber());
    }
}
