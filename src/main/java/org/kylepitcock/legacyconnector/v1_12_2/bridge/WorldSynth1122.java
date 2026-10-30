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
        Synth.InstanceMethod door = (self, args) -> {
            if (!(self instanceof legacyconnector.v1_12_2.net.minecraft.world.gen.structure.StructureComponent piece) || args.length < 8) return Synth.NONE;
            piece.mcc$generateDoor((World) args[0], (legacyconnector.v1_12_2.net.minecraft.world.gen.structure.StructureBoundingBox) args[1],
                    (java.util.Random) args[2], Synth.toInt(args[3]), Synth.toInt(args[4]), Synth.toInt(args[5]),
                    (legacyconnector.v1_12_2.net.minecraft.util.EnumFacing) args[6],
                    args[7] instanceof legacyconnector.v1_12_2.net.minecraft.block.Block b ? b : null);
            return null;
        };
        Synth.addInstanceMethod("net.minecraft.world.gen.structure.StructureComponent.generateDoor", door);
        Synth.addInstanceMethod("net.minecraft.world.gen.structure.StructureVillagePieces$Village.generateDoor", door);
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
            Object custom = Synth.callInstance("net.minecraft.entity.EntityLiving$SpawnPlacementType.canSpawnAt", type, new Object[] {world, pos});
            if (custom instanceof Boolean b) return b;
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
