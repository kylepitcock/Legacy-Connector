package org.kylepitcock.legacyconnector.core.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * {@code legacyconnector:legacy_marker}: the one piece of a {@link LegacyGeneratorStructure} start. Its box is the area a legacy world
 * generator built in; placing it does nothing (the legacy generator itself built the structure, during decoration). It only makes the
 * build a modern structure start: {@code /locate}, structure-aware mods and saving see it.
 */
public final class LegacyMarkerPiece extends StructurePiece {
    /** Set when the piece type is registered (LegacyConnector). */
    public static StructurePieceType TYPE;

    LegacyMarkerPiece(BoundingBox box) {
        super(TYPE, 0, box);
    }

    public LegacyMarkerPiece(CompoundTag tag) {
        super(TYPE, tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        // the box (saved by StructurePiece) is all there is
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox box,
            ChunkPos chunkPos, BlockPos pos) {
        // built by the legacy generator itself
    }
}
