package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider;
import legacyconnector.v1_7_10.net.minecraft.world.gen.structure.MapGenStructure;
import legacyconnector.v1_7_10.net.minecraft.world.gen.structure.MapGenStructureIO;
import legacyconnector.v1_7_10.net.minecraft.world.gen.structure.StructureBoundingBox;
import legacyconnector.v1_7_10.net.minecraft.world.gen.structure.StructureComponent;
import legacyconnector.v1_7_10.net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.kylepitcock.legacyconnector.core.LegacyLifecycle;
import org.kylepitcock.legacyconnector.core.LegacyModLocks;
import org.kylepitcock.legacyconnector.core.worldgen.LegacyStructureRegistry;
import org.kylepitcock.legacyconnector.core.worldgen.LegacyStructureSource;

/**
 * 1.7.10 {@code MapGenStructure}s that mods drive from their own world generators (an {@code IWorldGenerator} or a
 * {@code PopulateChunkEvent} handler calling {@code generate} and {@code generateStructuresInChunk}, as Anti-Gravity's star generator
 * does) as modern structures ({@link LegacyStructureRegistry}).
 *
 * <p>Which generators: those constructed while legacy mods load (kept in static fields or by the mod's world generator), except the
 * layer's own vanilla stand-ins and generators that override {@code generate} or {@code generateStructuresInChunk} (their placement
 * is their own: CustomOreGen's ore distributions, HBM's NBT structures). Generators a mod dimension's chunk provider creates when the
 * dimension loads keep the 1.7.10 path, as does every call made in a mod dimension, the deep pass, or a level whose structure sets do
 * not include the generator's set (superflat): there the legacy code runs as before.
 *
 * <p>The starts are the legacy ones, laid out by the game with the legacy seeding ({@link MapGenStructure#mcc$startAt}) in every
 * chunk the structure set offers (spacing 1), so they are exactly as frequent as in 1.7.10. The pieces are placed in a chunk only
 * when the mod's own call asks for this generator's structures there ({@link LegacyStructureRegistry#activate}): its dimension checks
 * and config switches still decide.
 */
public final class MapGenStructures1710 {
    private record Tracked(MapGenStructure generator, String namespace) {}

    private static final List<Tracked> TRACKED = new ArrayList<>();
    private static final Map<MapGenStructure, Source> BY_GENERATOR = new IdentityHashMap<>();
    private static boolean providing;

    private MapGenStructures1710() {}

    /** A structure generator was constructed (LegacyStructures.track). Only those of the loading phase may become modern. */
    public static void tracked(MapGenStructure generator) {
        if (!LegacyLifecycle.isRegistrationOpen() || !LegacyStructureRegistry.enabled()) return;
        if (generator.getClass().getName().startsWith("legacyconnector.")) return; // the layer's vanilla stand-ins
        String ns = "legacyconnector";
        try {
            var active = org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710.active();
            if (active != null && active.record() != null && active.record().modernId != null) ns = active.record().modernId;
        } catch (RuntimeException | LinkageError e) {
            // the layer's namespace
        }
        synchronized (MapGenStructures1710.class) {
            TRACKED.add(new Tracked(generator, sanitize(ns)));
            if (!providing) {
                providing = true;
                LegacyStructureRegistry.ensureRegistered();
                LegacyStructureRegistry.addProvider(MapGenStructures1710::sources);
            }
        }
    }

    private static synchronized Collection<? extends LegacyStructureSource> sources() {
        List<Source> out = new ArrayList<>();
        Set<String> keys = new java.util.HashSet<>();
        Set<ResourceLocation> ids = new java.util.HashSet<>();
        for (Tracked t : TRACKED) {
            MapGenStructure g = t.generator;
            if (BY_GENERATOR.containsKey(g)) {
                out.add(BY_GENERATOR.get(g));
                continue;
            }
            if (ownPlacement(g.getClass())) continue;
            String name;
            try {
                name = g.getStructureName();
            } catch (RuntimeException | LinkageError e) {
                name = null;
            }
            if (name == null || name.isBlank()) continue;
            String key = g.getClass().getName() + "#" + name;
            for (int n = 2; !keys.add(key); n++) key = g.getClass().getName() + "#" + name + "#" + n;
            String path = "legacy/" + sanitize(name);
            ResourceLocation id = ResourceLocation.tryBuild(t.namespace, path);
            if (id == null) continue;
            for (int n = 2; !ids.add(id); n++) id = ResourceLocation.fromNamespaceAndPath(t.namespace, path + "_" + n);
            Source s = new Source(g, key, id, biomes(g));
            BY_GENERATOR.put(g, s);
            out.add(s);
        }
        return out;
    }

    private static String sanitize(String s) {
        String out = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        return out.isEmpty() ? "unnamed" : out;
    }

    /** True when the generator class places its structures its own way (overrides the per-chunk generation or placement). */
    private static boolean ownPlacement(Class<?> type) {
        try {
            var generate = type.getMethod("generate", IChunkProvider.class, World.class, int.class, int.class, Block[].class);
            var place = type.getMethod("generateStructuresInChunk", World.class, Random.class, int.class, int.class);
            return generate.getDeclaringClass() != MapGenStructure.class || place.getDeclaringClass() != MapGenStructure.class;
        } catch (NoSuchMethodException | RuntimeException | LinkageError e) {
            return true;
        }
    }

    /** The modern biomes of the legacy spawn biome list the generator keeps in a field (1.7.10 vanilla's pattern), or none. */
    private static List<ResourceLocation> biomes(MapGenStructure g) {
        Set<BiomeGenBase> legacy = new LinkedHashSet<>();
        for (Class<?> c = g.getClass(); c != null && c != MapGenStructure.class && legacy.isEmpty(); c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!Collection.class.isAssignableFrom(f.getType()) && f.getType() != BiomeGenBase[].class) continue;
                try {
                    if (!f.trySetAccessible()) continue;
                    Object v = f.get(Modifier.isStatic(f.getModifiers()) ? null : g);
                    Iterable<?> items = v instanceof Collection<?> col ? col : v instanceof Object[] arr ? java.util.Arrays.asList(arr) : null;
                    if (items == null) continue;
                    Set<BiomeGenBase> found = new LinkedHashSet<>();
                    boolean other = false;
                    for (Object o : items) {
                        if (o instanceof BiomeGenBase b) found.add(b);
                        else if (o != null) other = true;
                    }
                    if (!other && !found.isEmpty()) {
                        legacy.addAll(found);
                        break;
                    }
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    // not readable: no list from this field
                }
            }
        }
        if (legacy.isEmpty()) return List.of();
        Set<ResourceLocation> out = new LinkedHashSet<>();
        for (BiomeGenBase b : legacy) {
            ResourceLocation id = LegacyEntitySpawns.modernBiome(b);
            if (id != null) out.add(id);
        }
        // every vanilla biome the legacy world reads as one of the listed biomes (meadow is legacy plains)
        for (Field f : net.minecraft.world.level.biome.Biomes.class.getFields()) {
            try {
                if (Modifier.isStatic(f.getModifiers()) && f.get(null) instanceof net.minecraft.resources.ResourceKey<?> k
                        && legacy.contains(LegacyEntitySpawns.legacyBiome(k.location()))) {
                    out.add(k.location());
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                // skip
            }
        }
        return new ArrayList<>(out);
    }

    /**
     * Whether a legacy call of {@code generate} / {@code generateStructuresInChunk} is handled by the modern structure (the legacy code
     * then does nothing). {@code placing}: the call asks for the structures of the chunk being decorated.
     */
    public static boolean modern(MapGenStructure generator, World world, boolean placing) {
        Source s;
        synchronized (MapGenStructures1710.class) {
            s = BY_GENERATOR.get(generator);
        }
        if (s == null || world == null || !(world.mcc$level() instanceof WorldGenRegion region)) return false;
        if (!LegacyStructureRegistry.generatesIn(region.getLevel(), s.id)) return false;
        // the deep pass runs the generators again below y=0: the structure is placed once, by the game
        if (World.mcc$inDeepPass()) return true;
        if (placing) LegacyStructureRegistry.activate(region, s.key);
        return true;
    }

    /** True when a start class places its components itself (an override of generateStructure): one piece covers the start. */
    private static final ClassValue<Boolean> OWN_START_PLACEMENT = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("generateStructure", World.class, Random.class, StructureBoundingBox.class).getDeclaringClass() != StructureStart.class;
            } catch (NoSuchMethodException | RuntimeException | LinkageError e) {
                return true;
            }
        }
    };

    private static BoundingBox modern(StructureBoundingBox b) {
        return new BoundingBox(Math.min(b.minX, b.maxX), Math.min(b.minY, b.maxY), Math.min(b.minZ, b.maxZ),
                Math.max(b.minX, b.maxX), Math.max(b.minY, b.maxY), Math.max(b.minZ, b.maxZ));
    }

    private static final class Source implements LegacyStructureSource {
        final MapGenStructure generator;
        final String key;
        final ResourceLocation id;
        final List<ResourceLocation> biomes;
        final ReentrantLock lock;

        Source(MapGenStructure generator, String key, ResourceLocation id, List<ResourceLocation> biomes) {
            this.generator = generator;
            this.key = key;
            this.id = id;
            this.biomes = biomes;
            this.lock = LegacyModLocks.of(generator);
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public ResourceLocation id() {
            return id;
        }

        @Override
        public List<ResourceLocation> biomes() {
            return biomes;
        }

        @Override
        public ReentrantLock lock() {
            return lock;
        }

        @Override
        public List<Part> start(ServerLevel level, long seed, int chunkX, int chunkZ) {
            World world = World.mcc$wrap(level);
            StructureStart[] out = new StructureStart[1];
            RuntimeException[] failure = new RuntimeException[1];
            LegacyModLocks.generation(lock, () -> {
                try {
                    out[0] = generator.mcc$startAt(world, chunkX, chunkZ);
                } catch (RuntimeException e) {
                    failure[0] = e;
                } catch (LinkageError e) {
                    failure[0] = new IllegalStateException(e);
                }
            });
            if (failure[0] != null) {
                LegacyStructureRegistry.failed(key, "laying out a start", failure[0]);
                return null;
            }
            StructureStart start = out[0];
            if (start == null || !start.isSizeableStructure() || start.getComponents().isEmpty()) return null;
            List<Part> parts = new ArrayList<>();
            if (OWN_START_PLACEMENT.get(start.getClass())) {
                if (start.getBoundingBox() == null) return null;
                parts.add(new Part(start, modern(start.getBoundingBox()), 0));
                return parts;
            }
            for (Object o : start.getComponents()) {
                if (o instanceof StructureComponent c && c.getBoundingBox() != null) parts.add(new Part(c, modern(c.getBoundingBox()), c.getComponentType()));
            }
            return parts.isEmpty() ? null : parts;
        }

        @Override
        public boolean place(Object handle, WorldGenLevel level, BoundingBox box, long seed) {
            World world = World.mcc$wrap(level);
            StructureBoundingBox chunk = new StructureBoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
            boolean[] keep = {true};
            LegacyModLocks.generation(lock, () -> {
                try {
                    if (handle instanceof StructureComponent c) {
                        if (c.getBoundingBox().intersectsWith(chunk) && !c.addComponentParts(world, new Random(seed), chunk)) keep[0] = false;
                    } else if (handle instanceof StructureStart s) {
                        s.generateStructure(world, new Random(seed), chunk);
                    }
                } catch (RuntimeException | LinkageError e) {
                    LegacyStructureRegistry.failed(key, "placing a piece", e);
                }
            });
            return keep[0];
        }

        @Override
        public CompoundTag save(Object handle, int startX, int startZ) {
            NBTTagCompound tag;
            if (handle instanceof StructureComponent c) {
                if (MapGenStructureIO.func_143036_a(c) == null) return null;
                tag = c.func_143010_b();
            } else if (handle instanceof StructureStart s) {
                if (MapGenStructureIO.func_143033_a(s) == null) return null;
                tag = s.func_143021_a(startX, startZ);
                tag.setBoolean("mcc:start", true);
            } else {
                return null;
            }
            return tag.mcc$modern().copy();
        }

        @Override
        public Object load(WorldGenLevel level, CompoundTag legacy) {
            World world = World.mcc$wrap(level);
            NBTTagCompound tag = new NBTTagCompound(legacy.copy());
            return legacy.getBoolean("mcc:start") ? MapGenStructureIO.func_143035_a(tag, world) : MapGenStructureIO.func_143032_b(tag, world);
        }
    }
}
