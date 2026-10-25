package legacyconnector.v1_12_2.net.minecraftforge.items;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTBase;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.CapabilityInject;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.CapabilityManager;

public class CapabilityItemHandler {
    @CapabilityInject(IItemHandler.class)
    public static Capability<IItemHandler> ITEM_HANDLER_CAPABILITY = null;

    static {
        CapabilityManager.INSTANCE.register(IItemHandler.class, new Capability.IStorage<IItemHandler>() {
            @Override
            public NBTBase writeNBT(Capability<IItemHandler> capability, IItemHandler instance, EnumFacing side) {
                NBTTagList list = new NBTTagList();
                for (int i = 0; i < instance.getSlots(); i++) {
                    legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack = instance.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        NBTTagCompound item = new NBTTagCompound();
                        item.setInteger("Slot", i);
                        stack.writeToNBT(item);
                        list.appendTag(item);
                    }
                }
                return list;
            }

            @Override
            public void readNBT(Capability<IItemHandler> capability, IItemHandler instance, EnumFacing side, NBTBase base) {
                if (!(instance instanceof IItemHandlerModifiable modifiable)) throw new RuntimeException("IItemHandler instance does not implement IItemHandlerModifiable");
                NBTTagList list = (NBTTagList) base;
                for (int i = 0; i < list.tagCount(); i++) {
                    NBTTagCompound item = list.getCompoundTagAt(i);
                    int slot = item.getInteger("Slot");
                    if (slot >= 0 && slot < instance.getSlots()) modifiable.setStackInSlot(slot, new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(item));
                }
            }
        }, () -> new ItemStackHandler());
        ITEM_HANDLER_CAPABILITY = CapabilityManager.INSTANCE.mcc$capability(IItemHandler.class);
    }

    public static void register() {
    }
}
