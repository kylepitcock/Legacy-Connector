package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A player releases a drawn bow (change the charge, or cancel to not shoot). */
@Cancelable
public class ArrowLooseEvent extends PlayerEvent {
    private final ItemStack bow;
    private final World world;
    private final boolean hasAmmo;
    private int charge;

    public ArrowLooseEvent(EntityPlayer player, ItemStack bow, World world, int charge, boolean hasAmmo) {
        super(player);
        this.bow = bow;
        this.world = world;
        this.charge = charge;
        this.hasAmmo = hasAmmo;
    }

    public ItemStack getBow() {
        return bow;
    }

    public World getWorld() {
        return world;
    }

    public boolean hasAmmo() {
        return hasAmmo;
    }

    public int getCharge() {
        return charge;
    }

    public void setCharge(int charge) {
        this.charge = charge;
    }
}
