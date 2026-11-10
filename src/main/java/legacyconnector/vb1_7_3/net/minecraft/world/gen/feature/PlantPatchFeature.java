package legacyconnector.vb1_7_3.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.PlantBlock;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Original 64-attempt patch generator; each plant's survival callback owns placement rules. */
public class PlantPatchFeature extends Feature {
    private int plantBlockId;
    public PlantPatchFeature(int plantBlockId){this.plantBlockId=plantBlockId;}
    @Override public boolean generate(World world,Random random,int x,int y,int z){
        for(int attempt=0;attempt<64;attempt++){
            int px=x+random.nextInt(8)-random.nextInt(8),py=y+random.nextInt(4)-random.nextInt(4),pz=z+random.nextInt(8)-random.nextInt(8);
            if(world.isAir(px,py,pz)&&((PlantBlock)Block.BLOCKS[plantBlockId]).canGrow(world,px,py,pz))world.setBlockWithoutNotifyingNeighbors(px,py,pz,plantBlockId);
        }return true;
    }
}
