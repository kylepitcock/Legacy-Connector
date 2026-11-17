package legacyconnector.v1_12_2.net.minecraftforge.event.brewing;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerEvent;

/** A player took a brewed potion out of a brewing stand (fed from the modern PlayerBrewedPotionEvent). */
public class PlayerBrewedPotionEvent extends PlayerEvent {
    private final ItemStack stack;

    public PlayerBrewedPotionEvent(EntityPlayer player, ItemStack stack) {
        super(player);
        this.stack = stack;
    }

    public ItemStack getStack() {
        return stack;
    }
}
