package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 generator of temples (desert and jungle pyramids, swamp huts, igloos). The modern game places these structures itself, so this one starts none of its own; the queries
 * legacy code makes ({@code getNearestStructurePos}, {@code isInsideStructure}) answer from the modern structures (see
 * {@code Structures1122}).
 */
public class MapGenScatteredFeature extends MapGenStructure {
    public MapGenScatteredFeature() {
    }

    public MapGenScatteredFeature(java.util.Map<String, String> options) {
    }

    @Override
    public String getStructureName() {
        return "Temple";
    }

    @Override
    protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
        return false;
    }

    @Override
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        return new StructureStart(chunkX, chunkZ);
    }

    @Override
    public BlockPos getNearestStructurePos(World worldIn, BlockPos pos, boolean findUnexplored) {
        this.world = worldIn;
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.nearest(worldIn, "Temple", pos, findUnexplored);
    }

    @Override
    public boolean isInsideStructure(BlockPos pos) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.inside(this.world, "Temple", pos) || super.isInsideStructure(pos);
    }

    /** The mobs of a swamp hut (witches) when the position is in one. */
    public java.util.List<legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.SpawnListEntry> getMonsters() {
        return new java.util.ArrayList<>();
    }

    public boolean isSwampHut(BlockPos pos) {
        var level = this.world == null ? null : this.world.mcc$liveLevel();
        if (!(level instanceof net.minecraft.server.level.ServerLevel s)) return false;
        var holder = s.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .getHolder(net.minecraft.world.level.levelgen.structure.BuiltinStructures.SWAMP_HUT);
        return holder.isPresent() && s.structureManager().getStructureWithPieceAt(pos.mcc$modern(), holder.get().value()).isValid();
    }
}
