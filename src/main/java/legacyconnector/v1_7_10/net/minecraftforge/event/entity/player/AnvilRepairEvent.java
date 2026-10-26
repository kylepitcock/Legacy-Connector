package legacyconnector.v1_7_10.net.minecraftforge.event.entity.player;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** A player took the result out of an anvil; {@link #breakChance} is the chance the anvil is damaged (0.12). */
public class AnvilRepairEvent extends PlayerEvent {
    public final ItemStack left;
    public final ItemStack right;
    public final ItemStack output;
    public float breakChance;

    public AnvilRepairEvent(EntityPlayer player, ItemStack output, ItemStack left, ItemStack right) {
        super(player);
        this.output = output;
        this.left = left;
        this.right = right;
        this.breakChance = 0.12F;
    }
}
