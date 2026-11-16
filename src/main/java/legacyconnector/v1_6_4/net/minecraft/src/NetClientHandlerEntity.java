package legacyconnector.v1_6_4.net.minecraft.src;

/** ModLoaderMP's client table row ({@code registerNetClientHandlerEntity}): the entity class of a spawn type and whether the spawn packet names an owner. */
public class NetClientHandlerEntity {
    public Class<?> entityClass;
    public boolean entityHasOwner;

    public NetClientHandlerEntity(Class<?> entityClass, boolean entityHasOwner) {
        this.entityClass = entityClass;
        this.entityHasOwner = entityHasOwner;
    }
}
