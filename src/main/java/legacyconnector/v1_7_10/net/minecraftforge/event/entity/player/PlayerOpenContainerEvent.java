package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.Container;

/** Asked every tick while a player has a container open: ALLOW keeps it open, DENY closes it. */
@legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event.HasResult
public class PlayerOpenContainerEvent extends PlayerEvent {
    public final boolean canInteractWith;

    public PlayerOpenContainerEvent(EntityPlayer player, Container openContainer) {
        super(player);
        this.canInteractWith = openContainer.canInteractWith(player);
    }
}
