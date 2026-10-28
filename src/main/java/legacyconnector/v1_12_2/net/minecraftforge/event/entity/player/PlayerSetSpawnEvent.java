package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A player's spawn point is set (cancel to keep the old one). */
@Cancelable
public class PlayerSetSpawnEvent extends PlayerEvent {
    private final boolean forced;
    private final BlockPos newSpawn;

    public PlayerSetSpawnEvent(EntityPlayer player, BlockPos newSpawn, boolean forced) {
        super(player);
        this.newSpawn = newSpawn;
        this.forced = forced;
    }

    public boolean isForced() {
        return forced;
    }

    public BlockPos getNewSpawn() {
        return newSpawn;
    }
}
