package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** A player releases a drawn bow; {@code charge} is the number of ticks it was drawn. */
@Cancelable
public class ArrowLooseEvent extends PlayerEvent {
    public final ItemStack bow;
    public int charge;

    public ArrowLooseEvent(EntityPlayer player, ItemStack bow, int charge) {
        super(player);
        this.bow = bow;
        this.charge = charge;
    }
}
