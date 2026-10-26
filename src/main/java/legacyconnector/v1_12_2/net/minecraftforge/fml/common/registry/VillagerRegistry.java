package legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge's villager registry. Professions and careers are registry entries (the {@code VillagerProfession} registry event); village
 * creation handlers are recorded here, for the village bridge to build their components once 1.12 structure pieces are modelled.
 */
public class VillagerRegistry {
    private static final VillagerRegistry INSTANCE = new VillagerRegistry();
    private final List<IVillageCreationHandler> villageCreationHandlers = new ArrayList<>();

    /**
     * Builds a village component; its methods ({@code getVillagePieceWeight}, {@code getComponentClass}, {@code buildComponent}) take
     * 1.12 structure types the layer does not model yet, so this only marks implementations.
     */
    public interface IVillageCreationHandler {
    }

    public static VillagerRegistry instance() {
        return INSTANCE;
    }

    public synchronized void registerVillageCreationHandler(IVillageCreationHandler handler) {
        if (handler == null || villageCreationHandlers.contains(handler)) return;
        villageCreationHandlers.add(handler);
    }

    public synchronized List<IVillageCreationHandler> mcc$villageCreationHandlers() {
        return new ArrayList<>(villageCreationHandlers);
    }
}
