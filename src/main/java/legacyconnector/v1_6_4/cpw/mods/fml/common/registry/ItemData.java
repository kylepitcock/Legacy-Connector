package legacyconnector.v1_6_4.cpw.mods.fml.common.registry;

import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;

/**
 * FML 1.6's record of an item in the id table saved with a world: the owning mod, the item's class and its numeric id. {@code GameData.writeItemData}
 * saves them and {@code buildWorldItemData} reads them back; a record equals another of the same mod and class.
 */
public class ItemData {
    private final String modId;
    private final String itemType;
    private final int itemId;

    public ItemData(String modId, String itemType, int itemId) {
        this.modId = modId;
        this.itemType = itemType;
        this.itemId = itemId;
    }

    /** A record read from the tag {@link #toNBT} wrote. */
    public ItemData(NBTTagCompound tag) {
        this(tag.getString("ModId"), tag.getString("ItemType"), tag.getInteger("ItemId"));
    }

    public String getModId() {
        return modId;
    }

    public String getItemType() {
        return itemType;
    }

    public int getItemId() {
        return itemId;
    }

    /** Writes the record into the tag (and returns it). */
    public NBTTagCompound toNBT(NBTTagCompound tag) {
        tag.setString("ModId", modId);
        tag.setString("ItemType", itemType);
        tag.setInteger("ItemId", itemId);
        return tag;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ItemData data && data.modId.equals(modId) && data.itemType.equals(itemType);
    }

    @Override
    public int hashCode() {
        return 31 * modId.hashCode() + itemType.hashCode();
    }

    @Override
    public String toString() {
        return modId + ":" + itemType + "@" + itemId;
    }
}
