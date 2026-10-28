package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2's flower pot tile entity: the item planted and its metadata (mods' pots extend it). */
public class TileEntityFlowerPot extends TileEntity {
    private Item flowerPotItem;
    private int flowerPotData;

    public TileEntityFlowerPot() {
    }

    public TileEntityFlowerPot(Item potItem, int potData) {
        this.flowerPotItem = potItem;
        this.flowerPotData = potData;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        ResourceLocation name = flowerPotItem == null ? null : Item.REGISTRY.getNameForObject(flowerPotItem);
        compound.setString("Item", name == null ? "" : name.toString());
        compound.setInteger("Data", flowerPotData);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Item", 8)) flowerPotItem = Item.getByNameOrId(compound.getString("Item"));
        else flowerPotItem = Item.getItemById(compound.getInteger("Item"));
        flowerPotData = compound.getInteger("Data");
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    public void setItemStack(ItemStack stack) {
        flowerPotItem = stack == null || stack.isEmpty() ? null : stack.getItem();
        flowerPotData = stack == null || stack.isEmpty() ? 0 : stack.getMetadata();
    }

    public ItemStack getFlowerItemStack() {
        return flowerPotItem == null ? ItemStack.EMPTY : new ItemStack(flowerPotItem, 1, flowerPotData);
    }

    public Item getFlowerPotItem() {
        return flowerPotItem;
    }

    public int getFlowerPotData() {
        return flowerPotData;
    }
}
