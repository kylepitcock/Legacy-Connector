package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** An empty bucket is about to fill from the targeted block; a handler that fills it sets {@link #result} and ALLOW. */
@Cancelable
@Event.HasResult
public class FillBucketEvent extends PlayerEvent {
    public final ItemStack current;
    public final World world;
    public final MovingObjectPosition target;
    public ItemStack result;

    public FillBucketEvent(EntityPlayer player, ItemStack current, World world, MovingObjectPosition target) {
        super(player);
        this.current = current;
        this.world = world;
        this.target = target;
    }
}
