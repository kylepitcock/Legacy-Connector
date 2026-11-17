package legacyconnector.v1_6_4.net.minecraft.src;

/** ModLoaderMP's server table row ({@code registerEntityTrackerEntry}): the spawn type id of an entity class and whether its spawn packet names an owner. */
public class EntityTrackerEntry2 {
    public int entityId;
    public boolean entityHasOwner;

    public EntityTrackerEntry2(int entityId, boolean entityHasOwner) {
        this.entityId = entityId;
        this.entityHasOwner = entityHasOwner;
    }
}
