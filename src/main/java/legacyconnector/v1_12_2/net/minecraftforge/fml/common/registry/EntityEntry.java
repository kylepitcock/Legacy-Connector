package legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

/** A registered entity type: its class and name (spawning legacy entities needs the entity bridge). */
public class EntityEntry extends IForgeRegistryEntry.Impl<EntityEntry> {
    private final Class<? extends Entity> cls;
    private final String name;
    private legacyconnector.v1_12_2.net.minecraft.entity.EntityList.EntityEggInfo egg;
    /** The builder's factory ({@code EntityEntryBuilder.factory}); null makes instances through the World constructor. */
    public java.util.function.Function<World, ? extends Entity> mcc$factory;
    /** Tracking settings given when the entry was built. */
    public int trackingRange;
    public int updateFrequency;
    public boolean sendsVelocityUpdates;
    /** The mod's own network id given to the builder (EntityRegistry.lookupModSpawn finds entries by it). */
    public int mcc$networkId = -1;

    public EntityEntry(Class<? extends Entity> cls, String name) {
        this.cls = cls;
        this.name = name;
    }

    public Class<? extends Entity> getEntityClass() {
        return cls;
    }

    public String getName() {
        return name;
    }

    public void setEgg(legacyconnector.v1_12_2.net.minecraft.entity.EntityList.EntityEggInfo egg) {
        this.egg = egg;
        if (egg != null && egg.spawnedID != null) legacyconnector.v1_12_2.net.minecraft.entity.EntityList.ENTITY_EGGS.put(egg.spawnedID, egg);
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.EntityList.EntityEggInfo getEgg() {
        return egg;
    }

    public boolean hasEgg() {
        return egg != null;
    }

    public Entity newInstance(World world) {
        if (mcc$factory != null) return mcc$factory.apply(world);
        try {
            var ctor = cls.getConstructor(World.class);
            return ctor.newInstance(world);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
