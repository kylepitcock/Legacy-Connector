package legacyconnector.v1_12_2.net.minecraft.entity;

/**
 * One hit box of a multi-part entity. Parts are not world entities in this layer (modern parts belong to their parent's class), so a
 * part keeps its own position and size, and hits on it are passed to the parent.
 */
public class MultiPartEntityPart extends Entity {
    public final IEntityMultiPart parent;
    public final String partName;

    public MultiPartEntityPart(IEntityMultiPart parent, String partName, float width, float height) {
        super(parent.getWorld());
        this.parent = parent;
        this.partName = partName;
        setSize(width, height);
    }

    @Override
    protected boolean mcc$isView() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean attackEntityFrom(legacyconnector.v1_12_2.net.minecraft.util.DamageSource source, float amount) {
        return !isEntityInvulnerable(source) && parent.attackEntityFromPart(this, source, amount);
    }

    @Override
    public boolean isEntityEqual(Entity entity) {
        return this == entity || parent == entity;
    }
}
