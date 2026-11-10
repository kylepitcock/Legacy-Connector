package legacyconnector.vb1_7_3.net.minecraft.client.particle;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta item fragments sample a quarter of the original selected item sprite. */
public class ItemParticle extends Particle {
    public ItemParticle(World world,double x,double y,double z,Item item){
        super(world,x,y,z,0,0,0);textureId=item.getTextureId(0);red=green=blue=1;
        // Original ItemParticle takes its gravity from vanilla snow's shared block field.
        Block snow=Block.BLOCKS[80];gravityStrength=snow==null?1F:snow.particleFallSpeedModifier;scale/=2F;
    }
    @Override public int getGroup(){return 2;}
    @Override public void render(Tessellator t,float partial,float rx,float rxz,float rz,float ryz,float rxy){
        float u=(textureId%16+prevU/4F)/16F,v=(textureId/16+prevV/4F)/16F,size=.1F*scale;
        float x=(float)(prevX+(this.x-prevX)*partial-xOffset),y=(float)(prevY+(this.y-prevY)*partial-yOffset),z=(float)(prevZ+(this.z-prevZ)*partial-zOffset);
        float brightness=getBrightnessAtEyes(partial);t.color(brightness*red,brightness*green,brightness*blue);
        t.vertex(x-rx*size-ryz*size,y-rxz*size,z-rz*size-rxy*size,u,v+.015609375F);
        t.vertex(x-rx*size+ryz*size,y+rxz*size,z-rz*size+rxy*size,u,v);
        t.vertex(x+rx*size+ryz*size,y+rxz*size,z+rz*size+rxy*size,u+.015609375F,v);
        t.vertex(x+rx*size-ryz*size,y-rxz*size,z+rz*size-rxy*size,u+.015609375F,v+.015609375F);
    }
}
