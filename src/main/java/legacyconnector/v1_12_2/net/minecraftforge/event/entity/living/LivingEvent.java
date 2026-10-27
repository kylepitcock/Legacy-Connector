package legacyconnector.v1_12_2.net.minecraftforge.event.entity.living;

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

/** An event about a living entity. */
public class LivingEvent extends legacyconnector.v1_12_2.net.minecraftforge.event.entity.EntityEvent {
    private final EntityLivingBase entityLiving;

    public LivingEvent(EntityLivingBase entity) {
        super(entity);
        this.entityLiving = entity;
    }

    public EntityLivingBase getEntityLiving() {
        return entityLiving;
    }

    /** Every tick of a living entity (cancel to skip the tick). */
    @Cancelable
    public static class LivingUpdateEvent extends LivingEvent {
        public LivingUpdateEvent(EntityLivingBase e) {
            super(e);
        }
    }

    /** A living entity jumps. */
    public static class LivingJumpEvent extends LivingEvent {
        public LivingJumpEvent(EntityLivingBase e) {
            super(e);
        }
    }
}
