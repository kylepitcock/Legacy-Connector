package legacyconnector.v1_12_2.baubles.api.cap;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandlerModifiable;

/** A player's seven Baubles slots (amulet, ring, ring, belt, head, body, charm). */
public interface IBaublesItemHandler extends IItemHandlerModifiable {
    boolean isItemValidForSlot(int slot, ItemStack stack, EntityLivingBase player);

    boolean isEventBlocked();

    void setEventBlock(boolean blockEvents);

    boolean isChanged(int slot);

    void setChanged(int slot, boolean changed);

    void setPlayer(EntityLivingBase player);
}
