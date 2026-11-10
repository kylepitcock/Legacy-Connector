package legacyconnector.vb1_7_3.net.minecraft.client.particle;

import legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta smoke expansion, animation and collision response. */
public class FireSmokeParticle extends Particle {
    float initialScale;
    public FireSmokeParticle(World world,double x,double y,double z,double vx,double vy,double vz){this(world,x,y,z,vx,vy,vz,1F);}
    public FireSmokeParticle(World world,double x,double y,double z,double vx,double vy,double vz,float scale){
        super(world,x,y,z,0,0,0);
        velocityX*=.1F;velocityY*=.1F;velocityZ*=.1F;
        velocityX+=vx;velocityY+=vy;velocityZ+=vz;
        red=green=blue=(float)(Math.random()*.3F);
        this.scale*=.75F;this.scale*=scale;initialScale=this.scale;
        maxParticleAge=(int)(8/(Math.random()*.8+.2));maxParticleAge=(int)(maxParticleAge*scale);noClip=false;
    }
    @Override public void render(Tessellator t,float partial,float rx,float rxz,float rz,float ryz,float rxy){
        float growth=(particleAge+partial)/maxParticleAge*32F;
        if(growth<0)growth=0;if(growth>1)growth=1;
        scale=initialScale*growth;super.render(t,partial,rx,rxz,rz,ryz,rxy);
    }
    @Override public void tick(){
        prevX=x;prevY=y;prevZ=z;if(particleAge++>=maxParticleAge)markDead();
        textureId=7-particleAge*8/maxParticleAge;velocityY+=.004;move(velocityX,velocityY,velocityZ);
        if(y==prevY){velocityX*=1.1;velocityZ*=1.1;}
        velocityX*=.96F;velocityY*=.96F;velocityZ*=.96F;
        if(onGround){velocityX*=.7F;velocityZ*=.7F;}
    }
}
