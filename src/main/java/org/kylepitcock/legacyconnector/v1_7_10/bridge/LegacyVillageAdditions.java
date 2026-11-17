package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import legacyconnector.v1_7_10.cpw.mods.fml.common.registry.VillagerRegistry;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import org.kylepitcock.legacyconnector.core.worldgen.LegacyVillageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.7.10's source of legacy village buildings (Forge village creation handlers: Thaumcraft's wizard tower and banker, Witchery's
 * shops...) for {@link org.kylepitcock.legacyconnector.core.worldgen.VillageAdditions}, which adds them to vanilla villages. Handlers
 * that add vanilla's own 1.7.10 buildings are left out.
 */
public final class LegacyVillageAdditions implements LegacyVillageSource {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Villages");
    static final LegacyVillageAdditions INSTANCE = new LegacyVillageAdditions();

    /** A legacy building: its handler and the pool element that draws it. */
    private record Building(VillagerRegistry.IVillageCreationHandler handler, LegacyVillagePoolElement element) implements LegacyVillageSource.Building {
        @Override
        public Weight weigh(Random rand) {
            var w = handler.getVillagePieceWeight(rand, 0);
            return w == null ? null : new Weight(w.villagePieceWeight, w.villagePiecesLimit);
        }

        @Override
        public BoundingBox footprint(int mode) {
            return element.footprint(mode);
        }

        @Override
        public StructurePoolElement inVillage(List<BoundingBox> streets, List<BoundingBox> others) {
            return element.inVillage(streets, others);
        }

        @Override
        public String toString() {
            return element.toString();
        }
    }

    private static final Map<String, List<LegacyVillageSource.Building>> BUILDINGS = new java.util.concurrent.ConcurrentHashMap<>();

    private LegacyVillageAdditions() {}

    @Override
    public boolean active() {
        return !VillagerBridge.creationHandlers().isEmpty();
    }

    @Override
    public List<LegacyVillageSource.Building> buildings(String type) {
        return BUILDINGS.computeIfAbsent(type, t -> {
            List<LegacyVillageSource.Building> out = new ArrayList<>();
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

    /** Startup log: the buildings with their 1.7.10 weight and limit for one sample village. */
    @Override
    public void describe() {
        Random rand = new Random(0);
        for (LegacyVillageSource.Building lb : buildings("plains")) {
            Building b = (Building) lb;
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
}
