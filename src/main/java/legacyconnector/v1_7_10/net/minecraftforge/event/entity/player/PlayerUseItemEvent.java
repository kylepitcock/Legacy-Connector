package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** A player uses a held item over time (eating, drinking, drawing a bow). */
public class PlayerUseItemEvent extends PlayerEvent {
    public final ItemStack item;
    public int duration;

    private PlayerUseItemEvent(EntityPlayer player, ItemStack item, int duration) {
        super(player);
        this.item = item;
        this.duration = duration;
    }

    @Cancelable
    public static class Start extends PlayerUseItemEvent {
        public Start(EntityPlayer player, ItemStack item, int duration) {
            super(player, item, duration);
        }
    }

    @Cancelable
    public static class Tick extends PlayerUseItemEvent {
        public Tick(EntityPlayer player, ItemStack item, int duration) {
            super(player, item, duration);
        }
    }

    @Cancelable
    public static class Stop extends PlayerUseItemEvent {
        public Stop(EntityPlayer player, ItemStack item, int duration) {
            super(player, item, duration);
        }
    }

    public static class Finish extends PlayerUseItemEvent {
        public Finish(EntityPlayer player, ItemStack item, int duration) {
            super(player, item, duration);
        }
    }
}
