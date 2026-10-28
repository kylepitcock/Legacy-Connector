package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.gen.structure.StructureBoundingBox;
import legacyconnector.v1_7_10.net.minecraft.world.gen.structure.StructureComponent;
import legacyconnector.v1_7_10.net.minecraft.world.gen.structure.StructureVillagePieces;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.kylepitcock.legacyconnector.core.worldgen.LegacyWorldGenFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy village components (Forge village creation handlers: Witchery's walls and shops, AgriCraft's greenhouse) in the
 * modern, jigsaw-based villages. Each chunk finds the village starts that reach it, plans the legacy components from the
 * seed (the same plan every time, so nothing is persisted) and lets the components that touch the chunk build themselves
 * there, as 1.7.10's village generator did. Components are placed as 1.7.10 placed them from its paths: beside one of the
 * village's streets, facing it, so their doors open onto the road; components that continue the layout from their own
 * paths get the next building as 1.7.10 chose it. A village's plan is kept for a while, so the chunks it crosses build from
 * the same components (one ground height per building).
 */
public final class LegacyVillages {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Villages");
    private static boolean registered;
    private static final java.util.Set<Long> PLANNED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** Recent plans by seed and village start chunk. */
    private static final java.util.Map<String, List<StructureComponent>> PLANS = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<String, List<StructureComponent>>(64, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, List<StructureComponent>> eldest) {
                    return size() > 64;
                }
            });

    private LegacyVillages() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        LegacyWorldGenFeature.addDecorator(LegacyVillages::decorate);
    }

    /** A piece that is already there (a modern village piece): occupies space, builds nothing. */
    private static final class Occupied extends StructureVillagePieces.Village {
        Occupied(StructureBoundingBox box) {
            this.boundingBox = box;
        }

        @Override
        protected void writeStructureToNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        }

        @Override
        protected void readStructureFromNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        }

        @Override
        public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
            return true;
        }
    }

    private static void decorate(WorldGenLevel level, int chunkX, int chunkZ, long seed) {
        var handlers = VillagerBridge.creationHandlers();
        if (handlers.isEmpty() || !(level instanceof WorldGenRegion region)) return;
        StructureManager manager = level.getLevel().structureManager().forWorldGenRegion(region);
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<StructureStart> starts = manager.startsForStructure(new ChunkPos(chunkX, chunkZ), structure -> {
            var key = registry.getKey(structure);
            return key != null && key.getPath().startsWith("village_");
        });
        if (starts.isEmpty()) return;
        World world = World.mcc$wrap(level);
        StructureBoundingBox chunkBox = new StructureBoundingBox(chunkX << 4, 0, chunkZ << 4, (chunkX << 4) + 15, 255, (chunkZ << 4) + 15);
        for (StructureStart start : starts) {
            if (!start.isValid()) continue;
            BoundingBox box = start.getBoundingBox();
            ChunkPos origin = start.getChunkPos();
            var key = registry.getKey(start.getStructure());
            boolean desert = key != null && key.getPath().contains("desert");
            String planKey = seed + ":" + origin.toLong();
            List<StructureComponent> planned = PLANS.get(planKey);
            if (planned == null) {
                Random rand = new Random(seed ^ (origin.x * 341873128712L + origin.z * 132897987541L));
                planned = plan(start, box, rand, desert, world.getBiomeGenForCoords((box.minX() + box.maxX()) / 2, (box.minZ() + box.maxZ()) / 2), handlers);
                PLANS.put(planKey, planned);
            }
            Random buildRand = new Random(seed ^ ((long) chunkX * 3418731287L + (long) chunkZ * 1328979875L));
            for (StructureComponent component : planned) {
                // a mod's component that never set its box (its constructor failed or was cancelled) has nothing to build
                if (component == null || component.getBoundingBox() == null || !component.getBoundingBox().intersectsWith(chunkBox)) continue;
                try {
                    component.addComponentParts(world, buildRand, chunkBox);
                } catch (RuntimeException | LinkageError e) {
                    LOG.warn("Legacy village component {} failed in chunk {},{}", component.getClass().getName(), chunkX, chunkZ, e);
                }
            }
        }
    }

    private static List<StructureComponent> plan(StructureStart start, BoundingBox box, Random rand, boolean desert, legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase biome,
            List<legacyconnector.v1_7_10.cpw.mods.fml.common.registry.VillagerRegistry.IVillageCreationHandler> handlers) {
        List<Object> pieces = new ArrayList<>();
        List<BoundingBox> streets = new ArrayList<>();
        for (var piece : start.getPieces()) {
            BoundingBox b = piece.getBoundingBox();
            StructureBoundingBox box1710 = new StructureBoundingBox(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ());
            // The village paths are what legacy components follow (Witchery's walls run along them).
            boolean street = piece instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece pool && pool.getElement().toString().contains("street");
            pieces.add(street ? new StructureVillagePieces.Path(null, 0, rand, box1710, b.getXSpan() >= b.getZSpan() ? 1 : 0) : new Occupied(box1710));
            if (street) streets.add(b);
        }
        int existing = pieces.size();
        StructureVillagePieces.Start startPiece = new StructureVillagePieces.Start(null, 0, rand, box.minX(), box.minZ(), new ArrayList(), 0);
        startPiece.inDesert = desert;
        startPiece.biome = biome;
        // what a legacy layout continues with (getNextComponentNN/PP of a mod's own path): the vanilla buildings and the mods'
        List<StructureVillagePieces.PieceWeight> weights = StructureVillagePieces.mcc$vanillaWeights();
        startPiece.structureVillageWeightedPieceList = weights;
        java.util.Map<legacyconnector.v1_7_10.cpw.mods.fml.common.registry.VillagerRegistry.IVillageCreationHandler, StructureVillagePieces.PieceWeight> handlerWeights =
                new java.util.LinkedHashMap<>();
        for (var handler : handlers) {
            try {
                StructureVillagePieces.PieceWeight weight = handler.getVillagePieceWeight(rand, 0);
                if (weight == null) continue;
                handlerWeights.put(handler, weight);
                weights.add(weight);
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Village creation handler {} failed: {}", handler.getClass().getName(), e.toString());
            }
        }
        for (var entry : handlerWeights.entrySet()) {
            var handler = entry.getKey();
            StructureVillagePieces.PieceWeight weight = entry.getValue();
            try {
                int count = Math.max(weight.villagePiecesLimit, weight.villagePiecesLimit == 0 ? 1 : 0);
                for (int n = 0; n < count && weight.canSpawnMoreVillagePieces(); n++) {
                    for (int attempt = 0; attempt < 24; attempt++) {
                        int[] plot = plot(streets, box, rand);
                        Object built = handler.buildComponent(weight, startPiece, pieces, rand, plot[0], plot[1], plot[2], plot[3], 1);
                        if (!(built instanceof StructureComponent component)) continue;
                        pieces.add(component);
                        weight.villagePiecesSpawned++;
                        component.buildComponent(startPiece, pieces, rand);
                        break;
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Village creation handler {} failed: {}", handler.getClass().getName(), e.toString());
            }
        }
        // buildings added by components continuing the layout get their own turn, as in 1.7.10's village start
        for (int guard = 0; !startPiece.field_74932_i.isEmpty() && guard < 256; guard++) {
            Object next = startPiece.field_74932_i.remove(rand.nextInt(startPiece.field_74932_i.size()));
            try {
                ((StructureComponent) next).buildComponent(startPiece, pieces, rand);
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Legacy village component {} failed to extend the layout: {}", next.getClass().getName(), e.toString());
            }
        }
        List<StructureComponent> out = new ArrayList<>();
        for (int i = existing; i < pieces.size(); i++) out.add((StructureComponent) pieces.get(i));
        if (PLANNED.add(start.getChunkPos().toLong())) {
            LOG.info("Village at chunk {}: {} legacy component(s) planned: {}", start.getChunkPos(), out.size(), out.stream().map(c -> c.getClass().getSimpleName()).toList());
        }
        return out;
    }

    /**
     * A plot beside a street as 1.7.10's paths offered them ({@code getNextComponentNN/PP}): x, y, z of the corner at the
     * street and the facing that turns the building's front to it. Without streets, a random spot in the village.
     */
    private static int[] plot(List<BoundingBox> streets, BoundingBox village, Random rand) {
        if (streets.isEmpty()) {
            int x = village.minX() + 3 + rand.nextInt(Math.max(1, village.getXSpan() - 6));
            int z = village.minZ() + 3 + rand.nextInt(Math.max(1, village.getZSpan() - 6));
            return new int[] {x, village.minY(), z, rand.nextInt(4)};
        }
        BoundingBox s = streets.get(rand.nextInt(streets.size()));
        boolean low = rand.nextBoolean();
        if (s.getXSpan() >= s.getZSpan()) {
            // the street runs along x: plots north (facing 2) or south (facing 0) of it
            int x = s.minX() + rand.nextInt(Math.max(1, s.getXSpan()));
            return low ? new int[] {x, s.minY(), s.minZ() - 1, 2} : new int[] {x, s.minY(), s.maxZ() + 1, 0};
        }
        int z = s.minZ() + rand.nextInt(Math.max(1, s.getZSpan()));
        return low ? new int[] {s.minX() - 1, s.minY(), z, 1} : new int[] {s.maxX() + 1, s.minY(), z, 3};
    }
}
