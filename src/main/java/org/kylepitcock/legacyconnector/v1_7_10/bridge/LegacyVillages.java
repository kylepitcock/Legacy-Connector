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
 * modern, jigsaw-based villages. Each chunk finds the village starts that reach it, plans the legacy components inside
 * the village's bounding box from the seed (the same plan every time, so nothing is persisted) and lets the components
 * that touch the chunk build themselves there, as 1.7.10's village generator did.
 */
public final class LegacyVillages {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Villages");
    private static boolean registered;
    private static final java.util.Set<Long> PLANNED = java.util.concurrent.ConcurrentHashMap.newKeySet();

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
            Random rand = new Random(seed ^ (origin.x * 341873128712L + origin.z * 132897987541L));
            List<StructureComponent> planned = plan(start, box, rand, desert, world.getBiomeGenForCoords((box.minX() + box.maxX()) / 2, (box.minZ() + box.maxZ()) / 2), handlers);
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
        for (var piece : start.getPieces()) {
            BoundingBox b = piece.getBoundingBox();
            StructureBoundingBox box1710 = new StructureBoundingBox(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ());
            // The village paths are what legacy components follow (Witchery's walls run along them).
            boolean street = piece instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece pool && pool.getElement().toString().contains("street");
            pieces.add(street ? new StructureVillagePieces.Path(null, 0, rand, box1710, 0) : new Occupied(box1710));
        }
        int existing = pieces.size();
        StructureVillagePieces.Start startPiece = new StructureVillagePieces.Start(null, 0, rand, box.minX(), box.minZ(), new ArrayList(), 0);
        startPiece.inDesert = desert;
        startPiece.biome = biome;
        for (var handler : handlers) {
            try {
                StructureVillagePieces.PieceWeight weight = handler.getVillagePieceWeight(rand, 0);
                if (weight == null) continue;
                int count = Math.max(weight.villagePiecesLimit, weight.villagePiecesLimit == 0 ? 1 : 0);
                for (int n = 0; n < count; n++) {
                    for (int attempt = 0; attempt < 24; attempt++) {
                        int x = box.minX() + 3 + rand.nextInt(Math.max(1, box.getXSpan() - 6));
                        int z = box.minZ() + 3 + rand.nextInt(Math.max(1, box.getZSpan() - 6));
                        Object built = handler.buildComponent(weight, startPiece, pieces, rand, x, box.minY(), z, rand.nextInt(4), 1);
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
        List<StructureComponent> out = new ArrayList<>();
        for (int i = existing; i < pieces.size(); i++) out.add((StructureComponent) pieces.get(i));
        if (PLANNED.add(start.getChunkPos().toLong())) {
            LOG.info("Village at chunk {}: {} legacy component(s) planned: {}", start.getChunkPos(), out.size(), out.stream().map(c -> c.getClass().getSimpleName()).toList());
        }
        return out;
    }
}
