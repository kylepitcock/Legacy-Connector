package legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityVillager;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122;

/**
 * Forge's villager registry. Professions are registry entries (the {@code VillagerProfession} registry event), each with careers whose
 * trades mods add per level; the vanilla professions and careers are there for mods to add trades to, and those trades reach the modern
 * villagers of the matching profession (see {@code VillagerTrades1122}). Village creation handlers are recorded for the village bridge.
 */
public class VillagerRegistry {
    private static final VillagerRegistry INSTANCE = new VillagerRegistry();
    private final List<IVillageCreationHandler> villageCreationHandlers = new ArrayList<>();
    private static boolean mcc$vanillaDone;

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

    // ------------------------------------------------------------------ professions and careers

    /** Adds the vanilla professions and careers (1.12's, in its id order) so mods find them by name. */
    @SuppressWarnings("unchecked")
    public static synchronized void mcc$registerVanilla() {
        if (mcc$vanillaDone) return;
        mcc$vanillaDone = true;
        String[][] professions = {
                {"farmer", "farmer", "fisherman", "shepherd", "fletcher"},
                {"librarian", "librarian", "cartographer"},
                {"priest", "cleric"},
                {"smith", "armor", "weapon", "tool"},
                {"butcher", "butcher", "leather"},
                {"nitwit", "nitwit"}};
        for (String[] p : professions) {
            VillagerProfession prof = new VillagerProfession("minecraft:" + p[0], "minecraft:textures/entity/villager/" + p[0] + ".png",
                    "minecraft:textures/entity/zombie_villager/zombie_" + p[0] + ".png");
            for (int i = 1; i < p.length; i++) new VillagerCareer(prof, p[i]);
            Registries1122.VILLAGER_PROFESSIONS.registerVanilla(prof);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<VillagerProfession> mcc$professions() {
        mcc$registerVanilla();
        return (List<VillagerProfession>) (List<?>) Registries1122.VILLAGER_PROFESSIONS.getValues();
    }

    public static VillagerProfession getById(int id) {
        mcc$registerVanilla();
        Object p = Registries1122.VILLAGER_PROFESSIONS.getValue(id);
        if (p instanceof VillagerProfession v) return v;
        // 1.12's vanilla ids 0-5
        List<VillagerProfession> all = mcc$professions();
        return id >= 0 && id < all.size() ? all.get(id) : null;
    }

    @SuppressWarnings("unchecked")
    public static int getId(VillagerProfession profession) {
        mcc$registerVanilla();
        int vanilla = mcc$professions().indexOf(profession);
        if (vanilla >= 0 && vanilla < 6) return vanilla;
        return profession == null ? -1 : Registries1122.VILLAGER_PROFESSIONS.getID(profession);
    }

    /** Gives a villager a random profession among the registered ones. */
    public static void setRandomProfession(EntityVillager entity, Random rand) {
        List<VillagerProfession> all = mcc$professions();
        if (entity != null && !all.isEmpty()) entity.setProfession(all.get(rand.nextInt(all.size())));
    }

    /** A villager profession: skins and careers. */
    public static class VillagerProfession extends legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry.Impl<VillagerProfession> {
        private final ResourceLocation name;
        private final ResourceLocation texture;
        private final ResourceLocation zombie;
        private final List<VillagerCareer> careers = new ArrayList<>();

        public VillagerProfession(String name, String texture, String zombie) {
            this.name = new ResourceLocation(name);
            this.texture = new ResourceLocation(texture);
            this.zombie = new ResourceLocation(zombie);
            setRegistryName(this.name);
        }

        void register(VillagerCareer career) {
            career.id = careers.size();
            careers.add(career);
        }

        public ResourceLocation getSkin() {
            return texture;
        }

        public ResourceLocation getZombieSkin() {
            return zombie;
        }

        public VillagerCareer getCareer(int id) {
            for (VillagerCareer c : careers) if (c.id == id) return c;
            return careers.isEmpty() ? null : careers.get(0);
        }

        public int getRandomCareer(Random rand) {
            return careers.isEmpty() ? 0 : rand.nextInt(careers.size());
        }

        public List<VillagerCareer> mcc$careers() {
            return careers;
        }

        /** The modern profession of a vanilla one (1.12 id order), or null for a mod's. */
        public net.minecraft.world.entity.npc.VillagerProfession mcc$modern() {
            int id = mcc$professions().indexOf(this);
            return id >= 0 && id < 6 ? EntityVillager.mcc$modernProfession(id) : null;
        }
    }

    /** A career of a profession: its trades, by level from 1. */
    public static class VillagerCareer {
        private final VillagerProfession profession;
        private final String name;
        int id;
        private final List<List<EntityVillager.ITradeList>> trades = new ArrayList<>();

        public VillagerCareer(VillagerProfession parent, String name) {
            this.profession = parent;
            this.name = name;
            parent.register(this);
        }

        public String getName() {
            return name;
        }

        public int getId() {
            return id;
        }

        public VillagerProfession getProfession() {
            return profession;
        }

        public VillagerCareer addTrade(int level, EntityVillager.ITradeList... tradeList) {
            if (level <= 0) throw new IllegalArgumentException("Levels start at 1");
            synchronized (trades) {
                while (trades.size() < level) trades.add(new ArrayList<>());
                for (EntityVillager.ITradeList t : tradeList) if (t != null) trades.get(level - 1).add(t);
            }
            return this;
        }

        /** The trades unlocked at a level (index from 0), or null past the last. */
        public List<EntityVillager.ITradeList> getTrades(int level) {
            synchronized (trades) {
                return level >= 0 && level < trades.size() ? java.util.Collections.unmodifiableList(new ArrayList<>(trades.get(level))) : null;
            }
        }

        public int mcc$levels() {
            synchronized (trades) {
                return trades.size();
            }
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof VillagerCareer c && c.profession == profession && c.name.equals(name);
        }

        @Override
        public int hashCode() {
            return name.hashCode() * 31 + System.identityHashCode(profession);
        }
    }
}
