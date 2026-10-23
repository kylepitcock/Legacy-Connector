package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/** A player right-clicked an entity. */
@Cancelable
public class EntityInteractEvent extends PlayerEvent {
    public final Entity target;

    public EntityInteractEvent(EntityPlayer player, Entity target) {
        super(player);
        this.target = target;
    }
}
