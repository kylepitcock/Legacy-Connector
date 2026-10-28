package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A player takes the result out of an anvil (change the chance the anvil gets damaged). */
public class AnvilRepairEvent extends PlayerEvent {
    private final ItemStack left;
    private final ItemStack right;
    private final ItemStack output;
    private float breakChance = 0.12F;

    public AnvilRepairEvent(EntityPlayer player, ItemStack left, ItemStack right, ItemStack output) {
        super(player);
        this.left = left;
        this.right = right;
        this.output = output;
    }

    /** 1.12's names are crossed: the result is the "item result", the left input the "item input". */
    public ItemStack getItemResult() {
        return output;
    }

    public ItemStack getItemInput() {
        return left;
    }

    public ItemStack getIngredientInput() {
        return right;
    }

    public float getBreakChance() {
        return breakChance;
    }

    public void setBreakChance(float breakChance) {
        this.breakChance = breakChance;
    }
}
