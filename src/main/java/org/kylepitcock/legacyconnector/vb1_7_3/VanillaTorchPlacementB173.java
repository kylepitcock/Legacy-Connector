package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.function.IntPredicate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Original onBlockAdded support selection, scoped to native blocks placed through the Beta World API. */
public final class VanillaTorchPlacementB173 {
    private VanillaTorchPlacementB173(){}

    /** Null retains the requested state; AIR means the original placement has no valid support. */
    public static BlockState resolve(int id,int meta,BlockState requested,IntPredicate supported){
        if(!VanillaBlockStatesB173.isTorch(id,requested)||id!=50&&(meta&15)!=0)return null;
        // Original TorchBlock normalizes every initial placement; redstone only initializes metadata zero.
        for(int direction=1;direction<=5;direction++)if(supported.test(direction))
            return VanillaBlockStatesB173.modernState(id,direction,requested);
        return Blocks.AIR.defaultBlockState();
    }
}
