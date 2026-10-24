package legacyconnector.v1_7_10.net.minecraft.entity.boss;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.IEntityMultiPart;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;

/** One hit box of a multi-part boss: damage to it is forwarded to the boss. */
public class EntityDragonPart extends Entity {
    public final IEntityMultiPart entityDragonObj;
    public final String field_146032_b;

    public EntityDragonPart(IEntityMultiPart owner, String name, float width, float height) {
        super(owner.func_82194_d());
        setSize(width, height);
        this.entityDragonObj = owner;
        this.field_146032_b = name;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return !isEntityInvulnerable() && entityDragonObj.attackEntityFromPart(this, source, amount);
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
    }
}
