package legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;

/** Builds an {@link EntityEntry}. */
public final class EntityEntryBuilder<E extends Entity> {
    private Class<? extends E> cls;
    private ResourceLocation id;
    private String name;
    private int network;
    private int trackingRange = 64;
    private int updateFrequency = 3;
    private boolean sendsVelocityUpdates = true;
    private int eggPrimary = -1;
    private int eggSecondary = -1;
    private java.util.function.Function<legacyconnector.v1_12_2.net.minecraft.world.World, ? extends E> factory;
    /** The natural spawns {@link #spawn} asked for, added when the entry is registered. */
    private final java.util.List<Object[]> spawns = new java.util.ArrayList<>();

    private EntityEntryBuilder() {
    }

    public static <E extends Entity> EntityEntryBuilder<E> create() {
        return new EntityEntryBuilder<>();
    }

    @SuppressWarnings("unchecked")
    public <E2 extends E> EntityEntryBuilder<E2> entity(Class<? extends E2> cls) {
        EntityEntryBuilder<E2> self = (EntityEntryBuilder<E2>) this;
        self.cls = cls;
        return self;
    }

    public EntityEntryBuilder<E> id(ResourceLocation id, int network) {
        this.id = id;
        this.network = network;
        return this;
    }

    public EntityEntryBuilder<E> id(String id, int network) {
        return id(new ResourceLocation(id), network);
    }

    /** Makes the entities with this function instead of the class's World constructor. */
    public EntityEntryBuilder<E> factory(java.util.function.Function<legacyconnector.v1_12_2.net.minecraft.world.World, E> factory) {
        this.factory = factory;
        return this;
    }

    public EntityEntryBuilder<E> name(String name) {
        this.name = name;
        return this;
    }

    public EntityEntryBuilder<E> tracker(int range, int updateFrequency, boolean sendVelocityUpdates) {
        this.trackingRange = range;
        this.updateFrequency = updateFrequency;
        this.sendsVelocityUpdates = sendVelocityUpdates;
        return this;
    }

    public EntityEntryBuilder<E> egg(int primary, int secondary) {
        this.eggPrimary = primary;
        this.eggSecondary = secondary;
        return this;
    }

    public EntityEntryBuilder<E> spawn(EnumCreatureType type, int weight, int min, int max, Biome... biomes) {
        spawns.add(new Object[] {type, weight, min, max, biomes.clone()});
        return this;
    }

    public EntityEntryBuilder<E> spawn(EnumCreatureType type, int weight, int min, int max, java.util.Collection<Biome> biomes) {
        return spawn(type, weight, min, max, biomes.toArray(new Biome[0]));
    }

    public EntityEntry build() {
        BuiltEntityEntry entry = new BuiltEntityEntry(cls, name, new java.util.ArrayList<>(spawns));
        if (id != null) entry.setRegistryName(id);
        entry.trackingRange = trackingRange;
        entry.updateFrequency = updateFrequency;
        entry.sendsVelocityUpdates = sendsVelocityUpdates;
        entry.mcc$factory = factory;
        entry.mcc$networkId = network;
        if (eggPrimary >= 0) entry.setEgg(new legacyconnector.v1_12_2.net.minecraft.entity.EntityList.EntityEggInfo(id, eggPrimary, eggSecondary));
        return entry;
    }

    /** The built entry: its natural spawns are added once it is registered. */
    public final class BuiltEntityEntry extends EntityEntry {
        private final java.util.List<Object[]> mcc$spawns;
        private boolean mcc$added;

        BuiltEntityEntry(Class<? extends Entity> cls, String name, java.util.List<Object[]> spawns) {
            super(cls, name);
            this.mcc$spawns = spawns;
        }

        /** Adds the entity's natural spawns (once). */
        public void addedToRegistry() {
            if (mcc$added) return;
            mcc$added = true;
            for (Object[] s : mcc$spawns) {
                EntityRegistry.addSpawn(getEntityClass(), (Integer) s[1], (Integer) s[2], (Integer) s[3], (EnumCreatureType) s[0], (Biome[]) s[4]);
            }
        }

        @Override
        public void mcc$registered() {
            addedToRegistry();
        }
    }
}
