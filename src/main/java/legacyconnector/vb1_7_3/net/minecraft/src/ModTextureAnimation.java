package legacyconnector.vb1_7_3.net.minecraft.src;

import java.awt.image.BufferedImage;
import legacyconnector.vb1_7_3.net.minecraft.client.render.texture.DynamicTexture;

/** ModLoader's vertical strip animation, preserving its frame order and tick-rate semantics. */
public class ModTextureAnimation extends DynamicTexture {
    private final byte[][] images;
    private final int tickRate;
    private int index,ticks;
    public ModTextureAnimation(int sprite,int atlas,BufferedImage image,int tickRate){this(sprite,1,atlas,image,tickRate);}
    public ModTextureAnimation(int sprite,int repeat,int atlas,BufferedImage image,int tickRate){
        super(sprite);this.replicate=repeat;this.atlas=atlas;this.tickRate=tickRate;this.ticks=tickRate;
        int size=image.getWidth(),frames=image.getHeight()/size;if(frames<1)throw new IllegalArgumentException("source has no complete images");
        images=new byte[frames][size*size*4];
        for(int frame=0;frame<frames;frame++)for(int y=0;y<size;y++)for(int x=0;x<size;x++){
            int argb=image.getRGB(x,y+frame*size),i=(y*size+x)*4;
            images[frame][i]=(byte)(argb>>16);images[frame][i+1]=(byte)(argb>>8);images[frame][i+2]=(byte)argb;images[frame][i+3]=(byte)(argb>>24);
        }
        pixels=images[0];
    }
    @Override public void tick(){if(ticks>=tickRate){index=(index+1)%images.length;pixels=images[index];ticks=0;}ticks++;}
}
