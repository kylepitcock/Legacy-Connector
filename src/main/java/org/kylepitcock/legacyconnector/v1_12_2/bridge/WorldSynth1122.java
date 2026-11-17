package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.core.synth.Synth;

/**
 * Area W (world, chunks, world generation, structures) methods whose legacy signatures name classes only the synthesizer provides
 * (spawn placement types, door and plant enums...), answered with their real behaviour through {@link Synth#addStaticMethod} /
 * {@link Synth#addInstanceMethod}.
 */
public final class WorldSynth1122 {
    private WorldSynth1122() {}

    private static String enumName(Object o) {
        return o instanceof Enum<?> e ? e.name() : o == null ? null : String.valueOf(o);
    }

    public static void register() {
        Synth.addStaticMethod("net.minecraft.world.WorldEntitySpawner.canCreatureTypeSpawnAtLocation", args -> {
            if (args.length < 3 || !(args[1] instanceof World world) || !(args[2] instanceof BlockPos pos)) return Synth.NONE;
            return canCreatureTypeSpawnAtLocation(args[0], world, pos);
        });
        Synth.addInstanceMethod("net.minecraft.world.gen.feature.WorldGenDoublePlant.setPlantType", (self, args) -> {
            if (!(self instanceof legacyconnector.v1_12_2.net.minecraft.world.gen.feature.WorldGenDoublePlant gen) || args.length < 1) return Synth.NONE;
            gen.mcc$setPlantType(enumName(args[0]));
            return null;
        });
    }

    /**
     * 1.12's spawn placement check: inside the world border; a mod placement type (EnumHelper.addSpawnPlacementType) asks its
     * predicate; IN_WATER needs water here and below and no full block above; ON_GROUND (and the rest) a sturdy top below (not
     * bedrock or barrier) and room for the body here and above.
     */
    public static boolean canCreatureTypeSpawnAtLocation(Object type, World world, BlockPos pos) {
        var level = world.mcc$access();
        if (level == null || pos == null) return false;
        try {
            var p = pos.mcc$modern();
            if (!level.getWorldBorder().isWithinBounds(p)) return false;
            if (type instanceof legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving.SpawnPlacementType t && t.mcc$hasCustomRule()) return t.canSpawnAt(world, pos);
            var state = level.getBlockState(p);
            if ("IN_WATER".equals(enumName(type))) {
                return state.getFluidState().is(net.minecraft.tags.FluidTags.WATER) && level.getFluidState(p.below()).is(net.minecraft.tags.FluidTags.WATER)
                        && !level.getBlockState(p.above()).isRedstoneConductor(level, p.above());
            }
            var down = p.below();
            var downState = level.getBlockState(down);
            if (!downState.isFaceSturdy(level, down, net.minecraft.core.Direction.UP)) return false;
            if (downState.is(net.minecraft.world.level.block.Blocks.BEDROCK) || downState.is(net.minecraft.world.level.block.Blocks.BARRIER)) return false;
            return legacyconnector.v1_12_2.net.minecraft.world.WorldEntitySpawner.isValidEmptySpawnBlock(StatesBridge1122.toLegacy(state))
                    && legacyconnector.v1_12_2.net.minecraft.world.WorldEntitySpawner.isValidEmptySpawnBlock(StatesBridge1122.toLegacy(level.getBlockState(p.above())));
        } catch (Throwable t) {
            return false;
        }
    }
}
