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

/** A living entity using an item over time (eating, drawing a bow). */
public class LivingEntityUseItemEvent extends LivingEvent {
    private final ItemStack item;
    private int duration;

    private LivingEntityUseItemEvent(EntityLivingBase entity, ItemStack item, int duration) {
        super(entity);
        this.item = item;
        this.duration = duration;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    /** Starts using an item (cancel to not use it; the duration can be changed). */
    @Cancelable
    public static class Start extends LivingEntityUseItemEvent {
        public Start(EntityLivingBase entity, ItemStack item, int duration) {
            super(entity, item, duration);
        }
    }

    /** One tick of using (cancel to stop). */
    @Cancelable
    public static class Tick extends LivingEntityUseItemEvent {
        public Tick(EntityLivingBase entity, ItemStack item, int duration) {
            super(entity, item, duration);
        }
    }

    /** Stops using before finishing (cancel to not stop). */
    @Cancelable
    public static class Stop extends LivingEntityUseItemEvent {
        public Stop(EntityLivingBase entity, ItemStack item, int duration) {
            super(entity, item, duration);
        }
    }

    /** Finished using: the stack left in the hand. */
    public static class Finish extends LivingEntityUseItemEvent {
        private ItemStack result;

        public Finish(EntityLivingBase entity, ItemStack item, int duration, ItemStack result) {
            super(entity, item, duration);
            this.result = result;
        }

        public ItemStack getResultStack() {
            return result;
        }

        public void setResultStack(ItemStack result) {
            this.result = result;
        }
    }
}
