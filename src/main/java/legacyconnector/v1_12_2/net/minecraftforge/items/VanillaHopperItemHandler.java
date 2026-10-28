package legacyconnector.v1_12_2.net.minecraftforge.items;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityHopper;
import legacyconnector.v1_12_2.net.minecraftforge.items.wrapper.InvWrapper;

/** Forge: a hopper's item handler; an item put into an empty hopper waits for the hopper's next transfer, as vanilla insertion does. */
public class VanillaHopperItemHandler extends InvWrapper {
    private final TileEntityHopper hopper;

    public VanillaHopperItemHandler(TileEntityHopper hopper) {
        super(hopper);
        this.hopper = hopper;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (simulate) return super.insertItem(slot, stack, true);
        boolean wasEmpty = getInv().isEmpty();
        int originalStackSize = stack.getCount();
        stack = super.insertItem(slot, stack, false);
        if (wasEmpty && originalStackSize > stack.getCount() && !hopper.mayTransfer()) hopper.setTransferCooldown(8);
        return stack;
    }
}
