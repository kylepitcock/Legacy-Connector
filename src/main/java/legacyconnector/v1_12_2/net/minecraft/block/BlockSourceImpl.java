package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's place of the dispensing block source (the shim's implementation lives in the dispenser package). */
public class BlockSourceImpl extends legacyconnector.v1_12_2.net.minecraft.dispenser.BlockSourceImpl {
    public BlockSourceImpl(World worldIn, BlockPos posIn) {
        super(worldIn, posIn);
    }
}
