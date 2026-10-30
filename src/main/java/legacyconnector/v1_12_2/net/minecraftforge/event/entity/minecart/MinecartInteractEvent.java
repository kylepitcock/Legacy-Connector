package legacyconnector.v1_12_2.net.minecraftforge.event.entity.minecart;

import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityMinecart;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;

/** A player right-clicks a minecart (cancel to stop the minecart's own interaction). */
@Cancelable
public class MinecartInteractEvent extends MinecartEvent {
    private final EntityPlayer player;
    private final EnumHand hand;

    public MinecartInteractEvent(EntityMinecart minecart, EntityPlayer player, EnumHand hand) {
        super(minecart);
        this.player = player;
        this.hand = hand;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public ItemStack getItem() {
        return player.getHeldItem(hand);
    }

    public EnumHand getHand() {
        return hand;
    }
}
