package legacyconnector.vb1_7_3.net.minecraft.entity;

import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import legacyconnector.vb1_7_3.net.minecraft.util.math.MathHelper;

/** Beta falling block state, also used for native falling-block callback views. */
public class FallingBlockEntity extends Entity {
    public int blockId,timeFalling;
    public FallingBlockEntity(World world){super(world);}
    public FallingBlockEntity(World world,double x,double y,double z,int blockId){
        this(world);this.blockId=blockId;blocksSameBlockSpawning=true;setBoundingBoxSpacing(.98F,.98F);standingEyeHeight=height/2;
        setPosition(x,y,z);prevX=x;prevY=y;prevZ=z;
    }
    @Override protected boolean bypassesSteppingEffects(){return false;}
    @Override public boolean isCollidable(){return !dead;}
    @Override public void tick(){
        if(blockId==0){markDead();return;}
        prevX=x;prevY=y;prevZ=z;timeFalling++;velocityY-=.03999999910593033D;move(velocityX,velocityY,velocityZ);
        velocityX*=.9800000190734863D;velocityY*=.9800000190734863D;velocityZ*=.9800000190734863D;
        int bx=MathHelper.floor(x),by=MathHelper.floor(y),bz=MathHelper.floor(z);
        if(world.getBlockId(bx,by,bz)==blockId)world.setBlock(bx,by,bz,0);
        if(onGround){
            velocityX*=.699999988079071D;velocityZ*=.699999988079071D;velocityY*= -.5D;markDead();
            int below=world.getBlockId(bx,by-1,bz);
            boolean fallsThrough=below==0||below==51||world.getMaterial(bx,by-1,bz).isFluid();
            if(!(world.canPlace(blockId,bx,by,bz,true,1)&&!fallsThrough&&world.setBlock(bx,by,bz,blockId))&&!world.isRemote)dropItem(blockId,1);
        }else if(timeFalling>100&&!world.isRemote){dropItem(blockId,1);markDead();}
    }
    @Override public void writeNbt(NbtCompound tag){tag.putByte("Tile",(byte)blockId);}
    @Override public void readNbt(NbtCompound tag){blockId=tag.getByte("Tile")&255;}
    @Override public float getShadowRadius(){return 0;}
    public World getWorld(){return world;}
}
