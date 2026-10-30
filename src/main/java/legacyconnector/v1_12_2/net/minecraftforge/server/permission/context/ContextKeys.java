package legacyconnector.v1_12_2.net.minecraftforge.server.permission.context;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** The keys Forge's own contexts fill. */
public class ContextKeys {
    public static final ContextKey<BlockPos> POS = ContextKey.create("pos", BlockPos.class);
    public static final ContextKey<Entity> TARGET = ContextKey.create("target", Entity.class);
    public static final ContextKey<EnumFacing> FACING = ContextKey.create("facing", EnumFacing.class);
    public static final ContextKey<AxisAlignedBB> AREA = ContextKey.create("area", AxisAlignedBB.class);
    public static final ContextKey<IBlockState> BLOCK_STATE = ContextKey.create("blockstate", IBlockState.class);
}
