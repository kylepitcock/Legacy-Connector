package legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

/** A registered entity type: its class and name (spawning legacy entities needs the entity bridge). */
public class EntityEntry extends IForgeRegistryEntry.Impl<EntityEntry> {
    private final Class<? extends Entity> cls;
    private final String name;
    private int eggPrimary = -1;
    private int eggSecondary = -1;
    /** Tracking settings given when the entry was built. */
    public int trackingRange;
    public int updateFrequency;
    public boolean sendsVelocityUpdates;

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

    public EntityEntry setEgg(int primary, int secondary) {
        this.eggPrimary = primary;
        this.eggSecondary = secondary;
        return this;
    }

    public boolean hasEgg() {
        return eggPrimary >= 0;
    }

    public Entity newInstance(World world) {
        try {
            var ctor = cls.getConstructor(World.class);
            return ctor.newInstance(world);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
