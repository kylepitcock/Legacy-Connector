package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

@Cancelable
public class PlayerInteractEvent extends PlayerEvent {
    public enum Action {
        RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK, LEFT_CLICK_BLOCK
    }

    public final Action action;
    public final int x;
    public final int y;
    public final int z;
    public final int face;
    public final World world;
    public Result useBlock = Result.DEFAULT;
    public Result useItem = Result.DEFAULT;

    public PlayerInteractEvent(EntityPlayer player, Action action, int x, int y, int z, int face, World world) {
        super(player);
        this.action = action;
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
        this.world = world;
        if (face == -1) useBlock = Result.DENY;
    }

    @Override
    public void setCanceled(boolean cancel) {
        super.setCanceled(cancel);
        useBlock = cancel ? Result.DENY : useBlock == Result.DENY ? Result.DEFAULT : useBlock;
        useItem = cancel ? Result.DENY : useItem == Result.DENY ? Result.DEFAULT : useItem;
    }
}
