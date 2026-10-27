package legacyconnector.v1_12_2.net.minecraft.entity;

/** An entity that belongs to another one (tamed animals, thrown items). */
public interface IEntityOwnable {
    java.util.UUID getOwnerId();

    Entity getOwner();
}
