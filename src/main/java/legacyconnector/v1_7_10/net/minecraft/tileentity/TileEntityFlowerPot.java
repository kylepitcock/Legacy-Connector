package legacyconnector.v1_7_10.net.minecraft.tileentity;

import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;

/** A flower pot's plant (item and metadata), saved as "Item" (id) and "Data". */
public class TileEntityFlowerPot extends TileEntity {
    private Item flowerPotItem;
    private int flowerPotData;

    public TileEntityFlowerPot() {
    }

    public TileEntityFlowerPot(Item item, int data) {
        this.flowerPotItem = item;
        this.flowerPotData = data;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Item", flowerPotItem == null ? 0 : Item.getIdFromItem(flowerPotItem));
        tag.setInteger("Data", flowerPotData);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.flowerPotItem = Item.getItemById(tag.getInteger("Item"));
        this.flowerPotData = tag.getInteger("Data");
    }

    public void func_145964_a(Item item, int data) {
        this.flowerPotItem = item;
        this.flowerPotData = data;
    }

    public Item getFlowerPotItem() {
        return flowerPotItem;
    }

    public int getFlowerPotData() {
        return flowerPotData;
    }
}
