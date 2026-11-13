package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.world.BlockView;

/** Original face-culling callbacks use neighbour coordinates, with the block's current state bounds. */
public final class BlockFacesB173 {
    private static final int[][] OFFSETS={{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
    private BlockFacesB173(){}
    public static boolean hasCallback(Block block) {
        try{return block.getClass().getMethod("isSideVisible",BlockView.class,int.class,int.class,int.class,int.class).getDeclaringClass()!=Block.class;}
        catch(ReflectiveOperationException e){return false;}
    }
    public static int visibleFaces(Block block,BlockView view,int x,int y,int z) {
        synchronized(block) {
            double[] bounds={block.minX,block.minY,block.minZ,block.maxX,block.maxY,block.maxZ};
            try {
                block.updateBoundingBox(view,x,y,z);
                int visible=0;
                for(int side=0;side<6;side++) {
                    int[] d=OFFSETS[side];
                    int nx=x+d[0],ny=y+d[1],nz=z+d[2];
                    if(!block.isSideVisible(view,nx,ny,nz,side))continue;
                    // Stacked translucent faces of one block read as a grid of cells (a 65% alpha cloud); like Beta's glass and ice,
                    // a translucent block draws no face against the same block, whatever its own callback answers.
                    if(block.getRenderLayer()==1&&!block.isOpaque()&&view.getBlockId(nx,ny,nz)==block.id
                            &&view.getBlockMeta(nx,ny,nz)==view.getBlockMeta(x,y,z))continue;
                    visible|=1<<side;
                }
                return visible;
            } finally {
                block.minX=bounds[0];block.minY=bounds[1];block.minZ=bounds[2];
                block.maxX=bounds[3];block.maxY=bounds[4];block.maxZ=bounds[5];
            }
        }
    }
}
