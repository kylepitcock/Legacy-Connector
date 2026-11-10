package legacyconnector.vb1_7_3.net.minecraft.client.particle;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta explosion puff; animation uses the shared generic particle sheet. */
public class ExplosionParticle extends Particle {
    public ExplosionParticle(World world,double x,double y,double z,double vx,double vy,double vz){
        super(world,x,y,z,vx,vy,vz);
        velocityX=vx+(double)((float)(Math.random()*2-1)*.05F);
        velocityY=vy+(double)((float)(Math.random()*2-1)*.05F);
        velocityZ=vz+(double)((float)(Math.random()*2-1)*.05F);
        red=green=blue=random.nextFloat()*.3F+.7F;
        scale=random.nextFloat()*random.nextFloat()*6F+1F;
        maxParticleAge=(int)(16/((double)random.nextFloat()*.8+.2))+2;
    }
    @Override public void tick(){
        prevX=x;prevY=y;prevZ=z;if(particleAge++>=maxParticleAge)markDead();
        textureId=7-particleAge*8/maxParticleAge;velocityY+=.004;move(velocityX,velocityY,velocityZ);
        velocityX*=.9F;velocityY*=.9F;velocityZ*=.9F;
        if(onGround){velocityX*=.7F;velocityZ*=.7F;}
    }
}
