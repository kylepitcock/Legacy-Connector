package legacyconnector.vb1_7_3.net.minecraft.entity.decoration.painting;

import java.util.ArrayList;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.vb1_7_3.PaintingsB173;

/** Original Beta attachment coordinates, motive selection and validity; spawned paintings use the native entity. */
public class PaintingEntity extends Entity {
    public int facing,attachmentX,attachmentY,attachmentZ;
    public PaintingVariants variant=PaintingVariants.KEBAB;
    public PaintingEntity(World world){super(world);setBoundingBoxSpacing(.5F,.5F);}
    public PaintingEntity(World world,int x,int y,int z,int facing){
        this(world);attachmentX=x;attachmentY=y;attachmentZ=z;
        var fitting=new ArrayList<PaintingVariants>();
        for(var art:PaintingVariants.values()){variant=art;setFacing(facing);if(canStayAttached())fitting.add(art);}
        if(!fitting.isEmpty())variant=fitting.get(random.nextInt(fitting.size()));setFacing(facing);
    }
    public PaintingEntity(World world,int x,int y,int z,int facing,String motive){
        this(world);attachmentX=x;attachmentY=y;attachmentZ=z;
        for(var art:PaintingVariants.values())if(art.id.equals(motive)){variant=art;break;}setFacing(facing);
    }
    private static float offset(int pixels){return pixels==32||pixels==64?.5F:0;}
    public void setFacing(int facing){
        this.facing=facing&3;yaw=prevYaw=this.facing*90;
        // Keep Beta's geometry without rounding its thin box to a plane at modern world coordinates.
        double cx=attachmentX+.5,cy=attachmentY+.5,cz=attachmentZ+.5;
        switch(this.facing){case 0->{cz-=.5625F;cx-=offset(variant.width);}case 1->{cx-=.5625F;cz+=offset(variant.width);}case 2->{cz+=.5625F;cx+=offset(variant.width);}case 3->{cx+=.5625F;cz-=offset(variant.width);}}
        cy+=offset(variant.height);setPosition(cx,cy,cz);
        double hx=(this.facing==1||this.facing==3?.5:variant.width)/32,hy=variant.height/32D,hz=(this.facing==0||this.facing==2?.5:variant.width)/32;
        boundingBox.set(cx-hx+.00625,cy-hy+.00625,cz-hz+.00625,cx+hx-.00625,cy+hy-.00625,cz+hz-.00625);
    }
    public boolean canStayAttached(){
        if(world==null||!PaintingsB173.clear(this))return false;
        int bx=(int)Math.floor(x-variant.width/32F),by=(int)Math.floor(y-variant.height/32F),bz=(int)Math.floor(z-variant.width/32F);
        for(int i=0;i<variant.width/16;i++)for(int j=0;j<variant.height/16;j++)
            if(!(facing==0||facing==2?world.getMaterial(bx+i,by+j,attachmentZ):world.getMaterial(attachmentX,by+j,bz+i)).isSolid())return false;
        return true;
    }
    @Override public boolean isCollidable(){return true;}
    @Override public void writeNbt(NbtCompound nbt){nbt.putByte("Dir",(byte)facing);nbt.putString("Motive",variant.id);nbt.putInt("TileX",attachmentX);nbt.putInt("TileY",attachmentY);nbt.putInt("TileZ",attachmentZ);}
    @Override public void readNbt(NbtCompound nbt){attachmentX=nbt.getInt("TileX");attachmentY=nbt.getInt("TileY");attachmentZ=nbt.getInt("TileZ");variant=PaintingVariants.KEBAB;for(var art:PaintingVariants.values())if(art.id.equals(nbt.getString("Motive"))){variant=art;break;}setFacing(nbt.getByte("Dir"));}
}
