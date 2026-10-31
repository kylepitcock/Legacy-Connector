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
 * One legacy structure component as a modern piece ({@code legacyconnector:legacy_piece}): its box is the component's box and
 * placing it runs the component's {@code addComponentParts} for the chunk being decorated. A component that returns false is
 * placed in no later chunk (1.7.10 removed it from its start). Saved with the component's own legacy NBT when the mod registered
 * it with MapGenStructureIO; otherwise the start is laid out again from the seed and the piece found by its index.
 */
public final class LegacyStructurePiece extends StructurePiece {
    public static StructurePieceType TYPE;

    private final String generator;
    private final int startX;
    private final int startZ;
    private final int index;
    /** The legacy component (or start), once laid out or read back. */
    private volatile Object handle;
    /** The legacy NBT read from a save, until the piece is needed. */
    private CompoundTag saved;
    private volatile boolean off;

    LegacyStructurePiece(String generator, int startX, int startZ, int index, Object handle, BoundingBox box, int depth) {
        super(TYPE, depth, box);
        this.generator = generator;
        this.startX = startX;
        this.startZ = startZ;
        this.index = index;
        this.handle = handle;
    }

    public LegacyStructurePiece(CompoundTag tag) {
        super(TYPE, tag);
        this.generator = tag.getString("Src");
        this.startX = tag.getInt("SX");
        this.startZ = tag.getInt("SZ");
        this.index = tag.getInt("I");
        this.off = tag.getBoolean("Off");
        this.saved = tag.contains("L", 10) ? tag.getCompound("L") : null;
    }

    String generator() {
        return generator;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Src", generator);
        tag.putInt("SX", startX);
        tag.putInt("SZ", startZ);
        tag.putInt("I", index);
        if (off) tag.putBoolean("Off", true);
        CompoundTag legacy = saved;
        Object h = handle;
        LegacyStructureSource source = LegacyStructureRegistry.source(generator);
        if (h != null && source != null) {
            try {
                CompoundTag now = source.save(h, startX, startZ);
                if (now != null) legacy = now;
            } catch (RuntimeException | LinkageError e) {
                // keep what was read (or nothing: laid out again from the seed)
            }
        }
        if (legacy != null) tag.put("L", legacy);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox box,
            ChunkPos chunkPos, BlockPos pos) {
        if (off) return;
        LegacyStructureRegistry.place(this, level, box, random.nextLong());
    }

    /** Places the piece now (the mod asked for its structures in this chunk). */
    void placeNow(WorldGenLevel level, BoundingBox box, long seed) {
        if (off) return;
        LegacyStructureSource source = LegacyStructureRegistry.source(generator);
        if (source == null) return;
        Object h = resolve(source, level);
        if (h == null) {
            off = true;
            return;
        }
        boolean keep;
        try {
            // checked against the other modern structures like an in-place legacy generator (undone in this chunk when it builds into one)
            keep = InPlaceStructureGuard.guarded(level, generator, () -> source.place(h, level, box, seed));
        } catch (RuntimeException | LinkageError e) {
            LegacyStructureRegistry.failed(generator, "placing a piece", e);
            keep = false;
        }
        if (!keep) off = true;
    }

    private Object resolve(LegacyStructureSource source, WorldGenLevel level) {
        Object h = handle;
        if (h != null) return h;
        synchronized (this) {
            if (handle != null) return handle;
            CompoundTag legacy = saved;
            if (legacy != null) {
                try {
                    h = source.load(level, legacy);
                } catch (RuntimeException | LinkageError e) {
                    h = null;
                }
            }
            if (h == null) h = LegacyStructureRegistry.relayout(source, level, startX, startZ, index);
            if (h != null) {
                handle = h;
                saved = null;
            }
            return h;
        }
    }
}
