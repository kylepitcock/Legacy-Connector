package legacyconnector.v1_7_10.codechicken.nei;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** NEI's list of every item (a stack per item and subtype); filled by the NEI bridge before the mods' NEI plugins run. */
public class ItemList {
    public static List<ItemStack> items = new ArrayList<>();
}
