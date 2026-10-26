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
        return this;
    }

    public EntityEntryBuilder<E> spawn(EnumCreatureType type, int weight, int min, int max, java.util.Collection<Biome> biomes) {
        return this;
    }

    public EntityEntry build() {
        EntityEntry entry = new EntityEntry(cls, name);
        if (id != null) entry.setRegistryName(id);
        entry.trackingRange = trackingRange;
        entry.updateFrequency = updateFrequency;
        entry.sendsVelocityUpdates = sendsVelocityUpdates;
        entry.mcc$factory = factory;
        if (eggPrimary >= 0) entry.setEgg(new legacyconnector.v1_12_2.net.minecraft.entity.EntityList.EntityEggInfo(id, eggPrimary, eggSecondary));
        return entry;
    }
}
