package legacyconnector.vb1_7_3.net.minecraft.entity;

import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import legacyconnector.vb1_7_3.net.minecraft.util.math.MathHelper;

/** Beta primed TNT callbacks; native TNT receives this typed view without a second lifecycle. */
public class TntEntity extends Entity {
    public int fuse;
    public TntEntity(World world){super(world);blocksSameBlockSpawning=true;setBoundingBoxSpacing(.98F,.98F);standingEyeHeight=height/2;}
    public TntEntity(World world,double x,double y,double z){
        this(world);setPosition(x,y,z);float angle=(float)(Math.random()*(float)Math.PI*2);
        velocityX=-MathHelper.sin(angle*(float)Math.PI/180)*.02F;velocityY=.20000000298023224D;
        velocityZ=-MathHelper.cos(angle*(float)Math.PI/180)*.02F;fuse=80;prevX=x;prevY=y;prevZ=z;
    }
    @Override protected boolean bypassesSteppingEffects(){return false;}
    @Override public boolean isCollidable(){return !dead;}
    @Override public void tick(){
        prevX=x;prevY=y;prevZ=z;velocityY-=.03999999910593033D;move(velocityX,velocityY,velocityZ);
        velocityX*=.9800000190734863D;velocityY*=.9800000190734863D;velocityZ*=.9800000190734863D;
        if(onGround){velocityX*=.699999988079071D;velocityZ*=.699999988079071D;velocityY*= -.5D;}
        if(fuse--<=0){markDead();if(!world.isRemote)world.createExplosion(null,x,y,z,4);}
        else world.addParticle("smoke",x,y+.5D,z,0,0,0);
    }
    @Override public void writeNbt(NbtCompound tag){tag.putByte("Fuse",(byte)fuse);}
    @Override public void readNbt(NbtCompound tag){fuse=tag.getByte("Fuse");}
    @Override public float getShadowRadius(){return 0;}
}
