package legacyconnector.v1_6_4.facade.cpw.mods.fml.common.registry;

import java.util.LinkedHashSet;
import java.util.Set;
import legacyconnector.v1_6_4.cpw.mods.fml.common.registry.ItemData;
import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;

/** Facade of the 1.7.10 shim's {@code GameData} for 1.6.4 mods: the numeric item id table saved with a world. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class GameData {
    private GameData() {}

    /** The id table as records: every item with a numeric id, with the mod that registered it (the namespace of its name) and its class. */
    private static Set<ItemData> current() {
        Set<ItemData> out = new LinkedHashSet<>();
        legacyconnector.v1_7_10.cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry<Item> registry =
                legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameData.getItemRegistry();
        Item[] items = Ids.itemsList();
        for (int id = 0; id < items.length; id++) {
            Item item = items[id];
            if (item == null) continue;
            String name = registry.getNameForObject(item);
            int colon = name == null ? -1 : name.indexOf(':');
            out.add(new ItemData(colon > 0 ? name.substring(0, colon) : "minecraft", item.getClass().getName(), id));
        }
        return out;
    }

    /** 1.6.4 {@code GameData.writeItemData(list)}: appends one tag per item of the id table (mod, class, numeric id). */
    public static void writeItemData(NBTTagList list) {
        for (ItemData data : current()) list.appendTag(data.toNBT(new NBTTagCompound()));
    }

    /** 1.6.4 {@code GameData.buildWorldItemData(list)}: the records a world saved with {@link #writeItemData}. */
    public static Set<ItemData> buildWorldItemData(NBTTagList list) {
        Set<ItemData> out = new LinkedHashSet<>();
        for (int i = 0; i < list.tagCount(); i++) out.add(new ItemData(list.getCompoundTagAt(i)));
        return out;
    }
}
