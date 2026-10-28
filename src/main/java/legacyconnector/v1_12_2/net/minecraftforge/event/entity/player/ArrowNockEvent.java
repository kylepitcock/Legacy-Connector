package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A player starts drawing a bow (set an action to replace what happens). */
public class ArrowNockEvent extends PlayerEvent {
    private final ItemStack bow;
    private final EnumHand hand;
    private final World world;
    private final boolean hasAmmo;
    private legacyconnector.v1_12_2.net.minecraft.util.ActionResult<ItemStack> action;

    public ArrowNockEvent(EntityPlayer player, ItemStack bow, EnumHand hand, World world, boolean hasAmmo) {
        super(player);
        this.bow = bow;
        this.hand = hand;
        this.world = world;
        this.hasAmmo = hasAmmo;
    }

    public ItemStack getBow() {
        return bow;
    }

    public World getWorld() {
        return world;
    }

    public EnumHand getHand() {
        return hand;
    }

    public boolean hasAmmo() {
        return hasAmmo;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.ActionResult<ItemStack> getAction() {
        return action;
    }

    public void setAction(legacyconnector.v1_12_2.net.minecraft.util.ActionResult<ItemStack> action) {
        this.action = action;
    }
}
