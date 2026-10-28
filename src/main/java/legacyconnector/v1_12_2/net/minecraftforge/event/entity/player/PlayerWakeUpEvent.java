package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A sleeping player wakes up. */
public class PlayerWakeUpEvent extends PlayerEvent {
    private final boolean wakeImmediately;
    private final boolean updateWorld;
    private final boolean setSpawn;

    public PlayerWakeUpEvent(EntityPlayer player, boolean wakeImmediately, boolean updateWorld, boolean setSpawn) {
        super(player);
        this.wakeImmediately = wakeImmediately;
        this.updateWorld = updateWorld;
        this.setSpawn = setSpawn;
    }

    public boolean wakeImmediately() {
        return wakeImmediately;
    }

    public boolean updateWorld() {
        return updateWorld;
    }

    public boolean shouldSetSpawn() {
        return setSpawn;
    }
}
