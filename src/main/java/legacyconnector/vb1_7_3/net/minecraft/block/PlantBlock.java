package legacyconnector.vb1_7_3.net.minecraft.block;

import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;

/** Beta 1.7.3 {@code BlockFlower}: a small crossed-squares plant (render type 1) without collision. */
public class PlantBlock extends Block {
    protected PlantBlock(int id, int textureId) {
        super(id, Material.PLANT);
        this.textureId = textureId;
        setTickRandomly(true);
        float f = 0.2F;
        setBoundingBox(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, f * 3.0F, 0.5F + f);
    }

    @Override
    public boolean isOpaque() {
        return false;
    }

    @Override
    public boolean isFullCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 1;
    }

    protected boolean canPlantOnTop(int blockId){return blockId==2||blockId==3||blockId==60;}

    @Override public boolean canGrow(legacyconnector.vb1_7_3.net.minecraft.world.World world,int x,int y,int z){
        // Native vanilla plant views retain their specialized survival rules during ordinary gameplay.
        if(mcc$owner==null&&mcc$modern!=null&&world.mcc$level()!=null
                &&!legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$generationActive())
            return mcc$modern.defaultBlockState().canSurvive(world.mcc$access(),new net.minecraft.core.BlockPos(x,y,z));
        // Generation may read detached Beta terrain outside the native region, through the World view.
        if(mcc$owner==null&&(id==39||id==40)){
            if(y<0||y>=128)return false;
            int support=world.getBlockId(x,y-1,z);
            return world.getBrightness(x,y,z)<13&&support>=0&&support<BLOCKS_OPAQUE.length&&BLOCKS_OPAQUE[support];
        }
        return (world.getBrightness(x,y,z)>=8||world.hasSkyLight(x,y,z))&&canPlantOnTop(world.getBlockId(x,y-1,z));
    }
}
