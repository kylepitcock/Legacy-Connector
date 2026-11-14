package legacyconnector.vb1_7_3.net.minecraft.client.particle;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** ModLoader's actual particle admission, with server requests forwarded to tracking clients. */
public class ParticleManager {
    private World world;
    public ParticleManager(){}
    public ParticleManager(World world,legacyconnector.vb1_7_3.net.minecraft.client.texture.TextureManager textures){this.world=world;}
    public void addParticle(Particle particle){if(particle!=null){if(particle.world==null)particle.world=world;org.kylepitcock.legacyconnector.vb1_7_3.ParticlesB173.add(particle);}}
    /** The final two original arguments are the explicit block ID and metadata, not a side or current world lookup. */
    public void addBlockBreakParticles(int x,int y,int z,int blockId,int metadata){
        World active=world!=null?world:legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance().world;
        org.kylepitcock.legacyconnector.vb1_7_3.BlockBreakParticlesB173.request(active,x,y,z,blockId,metadata);
    }
    /** Beta's block-hit particles (the block being mined, one side): the block's break particles at the position. */
    public void addBlockBreakingParticles(int x,int y,int z,int side){
        World active=world!=null?world:legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance().world;
        if(active==null)return;
        int id=active.getBlockId(x,y,z);
        if(id>0)addBlockBreakParticles(x,y,z,id,active.getBlockMeta(x,y,z));
    }
    public void setWorld(World world){this.world=world;}
    public void removeDeadParticles(){}
    public void render(legacyconnector.vb1_7_3.net.minecraft.entity.Entity camera,float partial){}
    public void renderLit(legacyconnector.vb1_7_3.net.minecraft.entity.Entity camera,float partial){}
}
