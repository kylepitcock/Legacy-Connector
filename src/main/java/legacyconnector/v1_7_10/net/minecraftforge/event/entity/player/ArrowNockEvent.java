package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** A player starts drawing a bow; cancelling keeps the vanilla draw from starting (mods handle it themselves). */
@Cancelable
public class ArrowNockEvent extends PlayerEvent {
    public ItemStack result;

    public ArrowNockEvent(EntityPlayer player, ItemStack result) {
        super(player);
        this.result = result;
    }
}
