package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Bone meal used on a block: a handler that grows the block itself sets the result to ALLOW. */
@Cancelable
@Event.HasResult
public class BonemealEvent extends PlayerEvent {
    public final World world;
    public final Block block;
    public final int x;
    public final int y;
    public final int z;

    public BonemealEvent(EntityPlayer player, World world, Block block, int x, int y, int z) {
        super(player);
        this.world = world;
        this.block = block;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
