package org.kylepitcock.legacyconnector.core.worldgen;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A legacy structure generator (a 1.7.10 {@code MapGenStructure}) as seen by {@link LegacyStructure}: the version bridge lays out
 * the legacy start of a chunk and places its pieces; the modern side handles placement, biomes, saving and locating.
 */
public interface LegacyStructureSource {
    /** Stable key, written into the generated structure JSON (generator class and structure name). */
    String key();

    /** Registry id of the structure and of its structure set, under the legacy mod's namespace. */
    ResourceLocation id();

    /** Modern biomes the legacy generator lists as its spawn biomes; empty: no list found (the dimension's biomes). */
    List<ResourceLocation> biomes();

    /** The mod's generation lock (legacy generators are not thread safe). */
    ReentrantLock lock();

    /** The legacy start of a chunk, or null for none. Runs on a generation worker; never throws. */
    List<Part> start(ServerLevel level, long seed, int chunkX, int chunkZ);

    /** Places the part of a piece that lies in {@code box} (a chunk); false when the legacy piece gave up (placed in no later chunk). */
    boolean place(Object handle, WorldGenLevel level, BoundingBox box, long seed);

    /** The legacy NBT of a piece, or null when the mod did not register it with MapGenStructureIO (it is laid out again from the seed). */
    CompoundTag save(Object handle, int startX, int startZ);

    /** A piece read back from its legacy NBT, or null. */
    Object load(WorldGenLevel level, CompoundTag legacy);

    /** One piece of a legacy start: the legacy object, its box and generation depth. */
    record Part(Object handle, BoundingBox box, int depth) {}
}
