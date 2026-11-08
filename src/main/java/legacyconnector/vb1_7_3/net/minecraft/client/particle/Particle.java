package legacyconnector.vb1_7_3.net.minecraft.client.particle;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator;

/** Original Beta particle lifecycle and camera-facing quad; subclasses keep their own tick/render callbacks. */
public class Particle extends Entity {
    public int textureId,particleAge,maxParticleAge;
    public float prevU,prevV,scale,gravityStrength,red=1,green=1,blue=1;
    public static double xOffset,yOffset,zOffset;
    public transient org.kylepitcock.legacyconnector.vb1_7_3.ParticleHostB173 mcc$particleHost;
    public Particle(World world,double x,double y,double z,double vx,double vy,double vz){
        super(world);setBoundingBoxSpacing(.2F,.2F);standingEyeHeight=height/2;setPosition(x,y,z);prevX=x;prevY=y;prevZ=z;
        velocityX=vx+(Math.random()*2-1)*.4;velocityY=vy+(Math.random()*2-1)*.4;velocityZ=vz+(Math.random()*2-1)*.4;
        float speed=(float)(Math.random()+Math.random()+1)*.15F;double length=Math.sqrt(velocityX*velocityX+velocityY*velocityY+velocityZ*velocityZ);
        if(length>0){velocityX=velocityX/length*speed*.4;velocityY=velocityY/length*speed*.4+.1;velocityZ=velocityZ/length*speed*.4;}
        prevU=random.nextFloat()*3;prevV=random.nextFloat()*3;scale=(random.nextFloat()*.5F+.5F)*2;maxParticleAge=(int)(4/(random.nextFloat()*.9F+.1F));
    }
    public Particle multiplyVelocity(float amount){velocityX*=amount;velocityY=(velocityY-.1)*amount+.1;velocityZ*=amount;return this;}
    public Particle setScale(float amount){setBoundingBoxSpacing(.2F*amount,.2F*amount);scale*=amount;return this;}
    protected boolean bypassesSteppingEffects(){return false;}
    @Override protected void initDataTracker(){}
    @Override public void tick(){prevX=x;prevY=y;prevZ=z;if(particleAge++>=maxParticleAge)markDead();velocityY-=.04*gravityStrength;move(velocityX,velocityY,velocityZ);velocityX*=.9800000190734863;velocityY*=.9800000190734863;velocityZ*=.9800000190734863;if(onGround){velocityX*=.699999988079071;velocityZ*=.699999988079071;}}
    @Override public void move(double x,double y,double z){if(mcc$particleHost==null)super.move(x,y,z);else mcc$particleHost.moveOriginal(this,x,y,z);}
    public int getGroup(){return 0;}
    @Override public boolean isCollidable(){return false;}
    public void mcc$initializeRandom(long seed){random=new java.util.Random(seed);}
    public void render(Tessellator t,float partial,float rx,float rxz,float rz,float ryz,float rxy){
        float u=(textureId%16)/16F,v=(textureId/16)/16F,size=.1F*scale;
        float x=(float)(prevX+(this.x-prevX)*partial-xOffset),y=(float)(prevY+(this.y-prevY)*partial-yOffset),z=(float)(prevZ+(this.z-prevZ)*partial-zOffset);
        float brightness=getBrightnessAtEyes(partial);t.color(red*brightness,green*brightness,blue*brightness);
        t.vertex(x-rx*size-ryz*size,y-rxz*size,z-rz*size-rxy*size,u+.0624375F,v+.0624375F);
        t.vertex(x-rx*size+ryz*size,y+rxz*size,z-rz*size+rxy*size,u+.0624375F,v);
        t.vertex(x+rx*size+ryz*size,y+rxz*size,z+rz*size+rxy*size,u,v);
        t.vertex(x+rx*size-ryz*size,y-rxz*size,z+rz*size-rxy*size,u,v+.0624375F);
    }
    @Override public void writeNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound tag){}
    @Override public void readNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound tag){}
}
