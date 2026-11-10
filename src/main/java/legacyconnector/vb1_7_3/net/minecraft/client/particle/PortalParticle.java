package legacyconnector.vb1_7_3.net.minecraft.client.particle;

import legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta portal trajectory, growth and brightness; original subclasses may recolor the same particle. */
public class PortalParticle extends Particle {
    private float initialScale;
    private double initialX,initialY,initialZ;
    public PortalParticle(World world,double x,double y,double z,double vx,double vy,double vz){
        super(world,x,y,z,vx,vy,vz);
        velocityX=vx;velocityY=vy;velocityZ=vz;
        initialX=this.x=x;initialY=this.y=y;initialZ=this.z=z;
        float color=random.nextFloat()*.6F+.4F;
        initialScale=scale=random.nextFloat()*.2F+.5F;
        red=green=blue=1F*color;green*=.3F;red*=.9F;
        maxParticleAge=(int)(Math.random()*10)+40;noClip=true;textureId=(int)(Math.random()*8);
    }
    @Override public void render(Tessellator t,float partial,float rx,float rxz,float rz,float ryz,float rxy){
        float age=(particleAge+partial)/maxParticleAge;
        age=1-age;age*=age;age=1-age;scale=initialScale*age;
        super.render(t,partial,rx,rxz,rz,ryz,rxy);
    }
    @Override public float getBrightnessAtEyes(float partial){
        float ambient=super.getBrightnessAtEyes(partial),age=(float)particleAge/maxParticleAge;
        age*=age;age*=age;return ambient*(1-age)+age;
    }
    @Override public void tick(){
        prevX=x;prevY=y;prevZ=z;
        float age=(float)particleAge/maxParticleAge;
        float curve=-age+age*age*2F,factor=1F-curve;
        x=initialX+velocityX*factor;y=initialY+velocityY*factor+(1F-age);z=initialZ+velocityZ*factor;
        if(particleAge++>=maxParticleAge)markDead();
    }
}
