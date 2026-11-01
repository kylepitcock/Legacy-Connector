package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import legacyconnector.v1_7_10.cpw.mods.fml.common.registry.VillagerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy village buildings (Forge village creation handlers: Thaumcraft's wizard tower and banker, Witchery's shops...) added to vanilla
 * villages without changing them: the village is assembled exactly as vanilla does it, then the legacy buildings are added as extra
 * pieces of the same structure, beside its streets, front to the road, on free ground only (a plot that touches any piece is
 * skipped). Each added piece carries the village's streets and pieces, which its legacy component sees as 1.7.10's component list
 * (Witchery's town wall is built around them). Handlers that add vanilla's own 1.7.10 buildings are left out. How many: the share mod buildings had in a 1.7.10 village (their weights against vanilla's 91), counted against the
 * village's own buildings; each building at most as often per village as 1.7.10 allowed (villagePiecesLimit). Hooked from
 * {@code JigsawStructureLegacyVillageMixin}; switch: {@code villages.legacyBuildings}.
 */
public final class LegacyVillageAdditions {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Villages");
    private static final float VANILLA_1710_WEIGHT = 91.0F;
    private static final List<String> TYPES = List.of("plains", "desert", "savanna", "snowy", "taiga");
    /** Plots tried per building before giving up on it. */
    private static final int ATTEMPTS = 64;

    /** A legacy building: its handler and the pool element that draws it. */
    private record Building(VillagerRegistry.IVillageCreationHandler handler, LegacyVillagePoolElement element) {}

    /** A building's entry in one village: weight and limit as its handler gave them for that village. */
    private record Candidate(Building building, int weight, int limit) {
        LegacyVillagePoolElement element() {
            return building.element;
        }
    }

    private static final Map<String, List<Building>> BUILDINGS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Set<String> LOGGED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private LegacyVillageAdditions() {}

    /** The village type of a jigsaw structure's start pool ("village/plains/town_centers"), or null when it is no village or nothing is to add. */
    public static String villageType(Holder<StructureTemplatePool> startPool) {
        if (VillagerBridge.creationHandlers().isEmpty() || !org.kylepitcock.legacyconnector.core.LegacyConfig.villageLegacyBuildings()) return null;
        var key = startPool.unwrapKey().orElse(null);
        if (key == null || !key.location().getNamespace().equals("minecraft")) return null;
        String[] parts = key.location().getPath().split("/");
        if (parts.length < 3 || !parts[0].equals("village") || !TYPES.contains(parts[1])) return null;
        return parts[1];
    }

    /** The stub with the legacy buildings added after vanilla's pieces. */
    public static Structure.GenerationStub wrap(Structure.GenerationStub stub, Structure.GenerationContext context, String type) {
        return new Structure.GenerationStub(stub.position(), Either.left(builder -> {
            List<StructurePiece> vanilla = new ArrayList<>();
            StructurePiecesBuilder recording = new StructurePiecesBuilder() {
                @Override
                public void addPiece(StructurePiece piece) {
                    super.addPiece(piece);
                    vanilla.add(piece);
                }
            };
            stub.generator().ifLeft(c -> c.accept(recording)).ifRight(b -> b.build().pieces().forEach(recording::addPiece));
            for (StructurePiece p : vanilla) builder.addPiece(p);
            try {
                add(builder, vanilla, context, type);
            } catch (RuntimeException | LinkageError e) {
                if (LOGGED.add("failed")) LOG.warn("Legacy village buildings could not be added: {}", e.toString());
            }
        }));
    }

    private static List<Building> buildings(String type) {
        return BUILDINGS.computeIfAbsent(type, t -> {
            List<Building> out = new ArrayList<>();
            for (var handler : VillagerBridge.creationHandlers()) {
                try {
                    if (buildsVanillaPiece(handler)) continue;
                    var element = new LegacyVillagePoolElement(LegacyVillagePoolElement.key(handler), t,
                            net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection.RIGID);
                    if (element.isBuilding()) out.add(new Building(handler, element));
                } catch (RuntimeException | LinkageError e) {
                    LOG.warn("Village creation handler {} failed: {}", handler.getClass().getName(), e.toString());
                }
            }
            return List.copyOf(out);
        });
    }

    /**
     * Whether a handler adds one of vanilla's own 1.7.10 village buildings (Witchery's "town" clusters register extra houses, fields
     * and churches through its district handler). The modern village already has its own houses of each kind; adding the 1.7.10
     * ones again would change the vanilla village, so only mods' own buildings are added.
     */
    private static boolean buildsVanillaPiece(VillagerRegistry.IVillageCreationHandler handler) {
        try {
            Class<?> c = handler.getComponentClass();
            return c != null && c.getDeclaringClass() == legacyconnector.v1_7_10.net.minecraft.world.gen.structure.StructureVillagePieces.class;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    /**
     * The legacy buildings of one village, as 1.7.10's getStructureVillageWeightedPieceList made them: every handler is asked for its
     * weight with the village's random (most give a random limit, Thaumcraft's wizard tower 0 or 1), and a building whose limit is 0
     * is left out of that village.
     */
    private static List<Candidate> candidates(String type, Random rand) {
        List<Candidate> out = new ArrayList<>();
        for (Building b : buildings(type)) {
            try {
                var w = b.handler.getVillagePieceWeight(rand, 0);
                if (w != null && w.villagePieceWeight > 0 && w.villagePiecesLimit > 0) out.add(new Candidate(b, w.villagePieceWeight, w.villagePiecesLimit));
            } catch (RuntimeException | LinkageError e) {
                // left out of this village
            }
        }
        return out;
    }

    private static void add(StructurePiecesBuilder builder, List<StructurePiece> vanilla, Structure.GenerationContext context, String type) {
        List<BoundingBox> streets = new ArrayList<>();
        List<BoundingBox> others = new ArrayList<>();
        int buildings = 0;
        for (StructurePiece p : vanilla) {
            if (!(p instanceof PoolElementStructurePiece pool)) {
                others.add(p.getBoundingBox());
                continue;
            }
            String element = pool.getElement().toString();
            if (element.contains("/streets/")) {
                streets.add(p.getBoundingBox());
                continue;
            }
            others.add(p.getBoundingBox());
            if (element.contains("/houses/")) buildings++;
        }
        if (streets.isEmpty()) return;
        Random rand = new Random(context.seed() ^ context.chunkPos().toLong() * 0x9E3779B97F4A7C15L);
        List<Candidate> candidates = candidates(type, rand);
        if (candidates.isEmpty()) {
            if (VILLAGES_LOGGED.getAndIncrement() < 20) LOG.info("{} village at chunk {}: {} houses, no legacy building rolled for it", type, context.chunkPos(), buildings);
            return;
        }
        int modWeight = 0;
        for (Candidate c : candidates) modWeight += c.weight;
        // in 1.7.10 mod buildings were picked modWeight times in (91 + modWeight) against vanilla's: that share of the village's buildings
        float expected = buildings * modWeight / (VANILLA_1710_WEIGHT + modWeight);
        int target = (int) expected + (rand.nextFloat() < expected - (int) expected ? 1 : 0);
        Map<Candidate, Integer> placed = new HashMap<>();
        List<BoundingBox> added = new ArrayList<>();
        int[] rejects = new int[REJECTS.length];
        for (int n = 0; n < target; n++) {
            Candidate c = pick(candidates, placed, rand);
            if (c == null) break;
            for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
                int r = tryPlace(builder, context, c, streets, others, added, rand);
                if (r < 0) {
                    placed.merge(c, 1, Integer::sum);
                    break;
                }
                rejects[r]++;
            }
        }
        if (VILLAGES_LOGGED.getAndIncrement() < 20) {
            StringBuilder why = new StringBuilder();
            for (int i = 0; i < rejects.length; i++) if (rejects[i] > 0) why.append(why.isEmpty() ? "" : ", ").append(REJECTS[i]).append(' ').append(rejects[i]);
            StringBuilder what = new StringBuilder();
            placed.forEach((c, k) -> what.append(what.isEmpty() ? "" : ", ").append(c.element()).append(k > 1 ? " x" + k : ""));
            LOG.info("{} village at chunk {}: {} houses, {} legacy building(s) rolled, target {}, added {}{}", type, context.chunkPos(), buildings,
                    candidates.size(), target, what.isEmpty() ? "none" : what, why.isEmpty() ? "" : " (plots rejected: " + why + ")");
        }
    }

    private static final String[] REJECTS = {"no footprint", "water", "slope", "occupied"};
    private static final java.util.concurrent.atomic.AtomicInteger VILLAGES_LOGGED = new java.util.concurrent.atomic.AtomicInteger();

    /** A building by weight among those below their per-village limit, or null when none is left. */
    private static Candidate pick(List<Candidate> candidates, Map<Candidate, Integer> placed, Random rand) {
        int left = 0;
        for (Candidate c : candidates) if (placed.getOrDefault(c, 0) < c.limit) left += c.weight;
        if (left <= 0) return null;
        int roll = rand.nextInt(left);
        for (Candidate c : candidates) {
            if (placed.getOrDefault(c, 0) >= c.limit) continue;
            roll -= c.weight;
            if (roll < 0) return c;
        }
        return null;
    }

    /** -1 when placed, else the index of the reason in {@link #REJECTS}. */
    private static int tryPlace(StructurePiecesBuilder builder, Structure.GenerationContext context, Candidate c, List<BoundingBox> streets,
            List<BoundingBox> others, List<BoundingBox> added, Random rand) {
        BoundingBox s = streets.get(rand.nextInt(streets.size()));
        boolean low = rand.nextBoolean();
        boolean alongX = s.getXSpan() >= s.getZSpan();
        // the side of the street the building stands on, its front (LegacyVillagePoolElement.front) facing the street
        int mode = alongX ? (low ? 2 : 0) : (low ? 1 : 3);
        BoundingBox shape = c.element().footprint(mode);
        if (shape == null) return 0;
        // the plot corner that puts the footprint right beside the street, somewhere along it
        int cx, cz;
        if (alongX) {
            cx = s.minX() + rand.nextInt(Math.max(1, s.getXSpan())) - (shape.minX() + shape.maxX()) / 2;
            cz = low ? s.minZ() - 1 - shape.maxZ() : s.maxZ() + 1 - shape.minZ();
        } else {
            cz = s.minZ() + rand.nextInt(Math.max(1, s.getZSpan())) - (shape.minZ() + shape.maxZ()) / 2;
            cx = low ? s.minX() - 1 - shape.maxX() : s.maxX() + 1 - shape.minX();
        }
        BoundingBox flat = shape.moved(cx, 0, cz);
        int midX = (flat.minX() + flat.maxX()) / 2, midZ = (flat.minZ() + flat.maxZ()) / 2;
        int y = groundY(context, midX, midZ);
        // no building on water or far off the street's level where it stands (a cliff); the street follows the terrain, so its
        // ground is read right in front of the plot, not from the street piece's lowest point
        int floor = context.chunkGenerator().getFirstOccupiedHeight(midX, midZ, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        if (floor < y) return 1;
        int streetY = alongX ? groundY(context, Math.clamp(midX, s.minX(), s.maxX()), low ? s.minZ() : s.maxZ())
                : groundY(context, low ? s.minX() : s.maxX(), Math.clamp(midZ, s.minZ(), s.maxZ()));
        if (Math.abs(y - streetY) > 4) return 2;
        BoundingBox box = shape.moved(cx, y, cz);
        // a block of free ground around the building, except toward its own street, which it stands right beside
        BoundingBox margin = box.inflatedBy(1);
        if (box.intersects(s)) return 3;
        for (BoundingBox b : streets) if (b != s && b.intersects(margin)) return 3;
        for (BoundingBox b : others) if (b.intersects(margin)) return 3;
        for (BoundingBox b : added) if (b.intersects(margin)) return 3;
        // the piece knows its village: legacy components see its streets and buildings in their component list, as in 1.7.10
        builder.addPiece(new PoolElementStructurePiece(context.structureTemplateManager(), c.element().inVillage(streets, others), new BlockPos(cx, y, cz), 0,
                rotationOf(mode), box, LiquidSettings.IGNORE_WATERLOGGING));
        added.add(box);
        return -1;
    }

    private static int groundY(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
    }

    /** The rotation whose mode (LegacyVillagePoolElement.mode) is the given 1.7.10 coordBaseMode. */
    private static Rotation rotationOf(int mode) {
        return switch (mode) {
            case 0 -> Rotation.NONE;
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            default -> Rotation.COUNTERCLOCKWISE_90;
        };
    }

    /** Startup log: the buildings with their 1.7.10 weight and limit for one sample village. */
    static void describe() {
        Random rand = new Random(0);
        for (Building b : buildings("plains")) {
            String sample;
            try {
                var w = b.handler.getVillagePieceWeight(rand, 0);
                sample = w == null ? "none" : "weight " + w.villagePieceWeight + ", limit " + w.villagePiecesLimit;
            } catch (RuntimeException | LinkageError e) {
                sample = e.toString();
            }
            LOG.info("Legacy village building {}: footprint {}, a sample village gives it {} (vanilla buildings weigh 91; limit 0 leaves it out of the village)",
                    b.element, b.element.describeFootprint(), sample);
        }
    }

    /** For the hook: whether to wrap, then the wrapped stub. */
    public static Optional<Structure.GenerationStub> apply(Optional<Structure.GenerationStub> stub, Structure.GenerationContext context,
            Holder<StructureTemplatePool> startPool) {
        if (stub.isEmpty()) return stub;
        String type = villageType(startPool);
        return type == null ? stub : Optional.of(wrap(stub.get(), context, type));
    }
}
