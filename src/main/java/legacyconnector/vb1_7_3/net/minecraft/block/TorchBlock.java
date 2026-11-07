package legacyconnector.vb1_7_3.net.minecraft.block;

import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;

/** Beta 1.7.3 {@code BlockTorch}: render type 2, no collision. */
public class TorchBlock extends Block {
    protected TorchBlock(int id, int textureId) {
        super(id, textureId, Material.PISTON_BREAKABLE);
        setTickRandomly(true);
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
        return 2;
    }
    @Override public legacyconnector.vb1_7_3.net.minecraft.util.math.Box getCollisionShape(legacyconnector.vb1_7_3.net.minecraft.world.World world,int x,int y,int z){return null;}
    private boolean supported(legacyconnector.vb1_7_3.net.minecraft.world.World w,int x,int y,int z,int meta) {
        return switch(meta) { case 1 -> w.method_1783(x-1,y,z);case 2 -> w.method_1783(x+1,y,z);case 3 -> w.method_1783(x,y,z-1);case 4 -> w.method_1783(x,y,z+1);case 5 -> w.method_1783(x,y-1,z);default -> false;};
    }
    @Override public boolean canPlaceAt(legacyconnector.vb1_7_3.net.minecraft.world.World w,int x,int y,int z){for(int m=1;m<=5;m++)if(supported(w,x,y,z,m))return true;return false;}
    @Override public void onPlaced(legacyconnector.vb1_7_3.net.minecraft.world.World w,int x,int y,int z) {
        for(int m:new int[]{5,1,2,3,4})if(supported(w,x,y,z,m)){w.setBlockMeta(x,y,z,m);return;}
    }
    @Override public void onPlaced(legacyconnector.vb1_7_3.net.minecraft.world.World w,int x,int y,int z,int side) {
        int m=switch(side){case 1->5;case 2->4;case 3->3;case 4->2;case 5->1;default->0;};
        if(supported(w,x,y,z,m))w.setBlockMeta(x,y,z,m);
    }
    @Override public void neighborUpdate(legacyconnector.vb1_7_3.net.minecraft.world.World w,int x,int y,int z,int other) {
        int meta=w.getBlockMeta(x,y,z);if(!supported(w,x,y,z,meta)){dropStacks(w,x,y,z,meta);w.setBlock(x,y,z,0);}
    }
    @Override public void updateBoundingBox(legacyconnector.vb1_7_3.net.minecraft.world.BlockView w,int x,int y,int z) {
        switch(w.getBlockMeta(x,y,z)) {
            case 1 -> setBoundingBox(0,0.2F,0.35F,0.3F,0.8F,0.65F);
            case 2 -> setBoundingBox(0.7F,0.2F,0.35F,1,0.8F,0.65F);
            case 3 -> setBoundingBox(0.35F,0.2F,0,0.65F,0.8F,0.3F);
            case 4 -> setBoundingBox(0.35F,0.2F,0.7F,0.65F,0.8F,1);
            default -> setBoundingBox(0.4F,0,0.4F,0.6F,0.6F,0.6F);
        }
    }
}
