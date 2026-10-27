package legacyconnector.v1_12_2.net.minecraftforge.event.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** An entity starts or stops riding another (cancelable). */
@Cancelable
public class EntityMountEvent extends EntityEvent {
    private final Entity entityMounting;
    private final Entity entityBeingMounted;
    private final World worldObj;
    private final boolean isMounting;

    public EntityMountEvent(Entity entityMounting, Entity entityBeingMounted, World world, boolean isMounting) {
        super(entityMounting);
        this.entityMounting = entityMounting;
        this.entityBeingMounted = entityBeingMounted;
        this.worldObj = world;
        this.isMounting = isMounting;
    }

    public boolean isMounting() {
        return isMounting;
    }

    public boolean isDismounting() {
        return !isMounting;
    }

    public Entity getEntityMounting() {
        return entityMounting;
    }

    public Entity getEntityBeingMounted() {
        return entityBeingMounted;
    }

    public World getWorldObj() {
        return worldObj;
    }
}
