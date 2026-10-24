package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** The item a player was using broke; {@link #original} is the stack as it was. */
public class PlayerDestroyItemEvent extends PlayerEvent {
    public final ItemStack original;

    public PlayerDestroyItemEvent(EntityPlayer player, ItemStack original) {
        super(player);
        this.original = original;
    }
}
