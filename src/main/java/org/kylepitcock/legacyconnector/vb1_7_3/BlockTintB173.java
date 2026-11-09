package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.world.BlockView;

/** Original world and inventory block colors are separate callbacks; native tint handlers consume opaque ARGB. */
public final class BlockTintB173 {
    private BlockTintB173(){}
    public static boolean hasColor(Block block) {
        try {
            return block.getClass().getMethod("getColor",int.class).getDeclaringClass()!=Block.class
                    ||block.getClass().getMethod("getColorMultiplier",BlockView.class,int.class,int.class,int.class).getDeclaringClass()!=Block.class;
        }catch(ReflectiveOperationException e){return false;}
    }
    public static int inventoryColor(Block block,int metadata) {
        synchronized(block){return 0xFF000000|block.getColor(metadata)&0xFFFFFF;}
    }
    public static int worldColor(Block block,BlockView view,int x,int y,int z) {
        synchronized(block){return 0xFF000000|block.getColorMultiplier(view,x,y,z)&0xFFFFFF;}
    }
}
