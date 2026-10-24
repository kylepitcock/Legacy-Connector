package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A hoe is used on a block; a handler that tills it itself sets the result to ALLOW. */
@Cancelable
@Event.HasResult
public class UseHoeEvent extends PlayerEvent {
    public final ItemStack current;
    public final World world;
    public final int x;
    public final int y;
    public final int z;

    public UseHoeEvent(EntityPlayer player, ItemStack current, World world, int x, int y, int z) {
        super(player);
        this.current = current;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
