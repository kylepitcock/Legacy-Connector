package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A player tries to sleep in a bed; a handler's result replaces the vanilla checks (OK lets it sleep). */
public class PlayerSleepInBedEvent extends PlayerEvent {
    private EntityPlayer.SleepResult result;
    private final BlockPos pos;

    public PlayerSleepInBedEvent(EntityPlayer player, BlockPos pos) {
        super(player);
        this.pos = pos;
    }

    public EntityPlayer.SleepResult getResultStatus() {
        return result;
    }

    public void setResult(EntityPlayer.SleepResult result) {
        this.result = result;
    }

    public BlockPos getPos() {
        return pos;
    }
}
