package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import org.kylepitcock.legacyconnector.core.synth.Synth;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block;

/**
 * The per-state face queries of 1.12.2's {@code IBlockState} whose signatures name synthesized types ({@code IBlockAccess},
 * {@code BlockFaceShape}): like vanilla's StateImplementation they ask the block ({@code Block.isSideSolid(state, world, pos, side)},
 * {@code Block.getBlockFaceShape(world, state, pos, face)}), whose mod override is called when it has one. Without one, a vanilla
 * block answers from its modern state's face shape and a mod block gets Forge's default (a normal cube is solid on every side;
 * every face shape is SOLID).
 */
public final class BlockFaces1122 {
    private static final String STATE = "Llegacyconnector/v1_12_2/net/minecraft/block/state/IBlockState;";
    private static final String ACCESS = "Llegacyconnector/v1_12_2/net/minecraft/world/IBlockAccess;";
    private static final String POS = "Llegacyconnector/v1_12_2/net/minecraft/util/math/BlockPos;";
    private static final String FACING = "Llegacyconnector/v1_12_2/net/minecraft/util/EnumFacing;";
    private static final String SHAPE_CLASS = "net.minecraft.block.state.BlockFaceShape";
    private static final String SHAPE = "Llegacyconnector_synth/v1_12_2/net/minecraft/block/state/BlockFaceShape;";

    private BlockFaces1122() {}

    public static void register() {
        // the state interfaces mods call them through
        for (String owner : new String[] {"net.minecraft.block.state.IBlockState", "net.minecraftforge.common.property.IExtendedBlockState",
                "net.minecraft.block.state.IBlockProperties", "net.minecraft.block.state.BlockStateContainer$StateImplementation"}) {
            register(owner);
        }
    }

    private static void register(String owner) {
        Synth.addInstanceMethod(owner + ".isSideSolid", (self, args) -> {
            if (!(self instanceof IBlockState state) || args.length < 3 || !(args[1] instanceof BlockPos pos) || !(args[2] instanceof EnumFacing side)) {
                return Synth.NONE;
            }
            Block block = state.getBlock();
            if (block == null) return false;
            Object own = Synth.dispatch(block, "isSideSolid", "(" + STATE + ACCESS + POS + FACING + ")Z", new Object[] {state, args[0], pos, side});
            if (own != Synth.NONE) return own;
            Boolean vanilla = vanillaSturdy(state, args[0], pos, side);
            return vanilla != null ? vanilla : state.isNormalCube();
        });
        Synth.addInstanceMethod(owner + ".getBlockFaceShape", (self, args) -> {
            if (!(self instanceof IBlockState state) || args.length < 3 || !(args[1] instanceof BlockPos pos) || !(args[2] instanceof EnumFacing face)) {
                return Synth.NONE;
            }
            Block block = state.getBlock();
            if (block != null) {
                Object own = Synth.dispatch(block, "getBlockFaceShape", "(" + ACCESS + STATE + POS + FACING + ")" + SHAPE,
                        new Object[] {args[0], state, pos, face});
                if (own != Synth.NONE) return own;
            }
            Boolean vanilla = block == null ? Boolean.FALSE : vanillaSturdy(state, args[0], pos, face);
            Class<?> type = RuntimeApis1122.synthClass(SHAPE_CLASS);
            if (type == null) return Synth.NONE;
            return Vanilla1122.enumConstant(type, vanilla == null || vanilla ? "SOLID" : "UNDEFINED");
        });
    }

    /** Whether a vanilla block's face is a full sturdy face (in the world when it is a modern level's view), or null for a mod block. */
    private static Boolean vanillaSturdy(IBlockState state, Object world, BlockPos pos, EnumFacing side) {
        Object modern = state.getBlock().mcc$modern;
        if (!(modern instanceof net.minecraft.world.level.block.Block) || modern instanceof Legacy1122Block) return null;
        try {
            var s = StatesBridge1122.toModern(state);
            var p = new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ());
            Direction d = Direction.byName(side.getName());
            if (d == null) return false;
            if (world instanceof World w && w.mcc$level() != null) return s.isFaceSturdy(w.mcc$level(), p, d);
            return s.isFaceSturdy(EmptyBlockGetter.INSTANCE, p, d);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }
}
