package legacyconnector.v1_7_10.net.minecraftforge.oredict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.OreDictBridge;

/**
 * The Forge ore dictionary. Legacy registrations are kept here and exported as modern item tags
 * ({@code ingotCopper} -> {@code c:ingots/copper}) by {@link OreDictBridge}; lookups also see modern
 * items carrying the equivalent tag once tags are loaded, so legacy machines accept modern copper.
 */
public class OreDictionary {
    public static final int WILDCARD_VALUE = Short.MAX_VALUE;
    private static final List<String> idToName = new ArrayList<>();
    private static final Map<String, Integer> nameToId = new HashMap<>();
    private static final List<ArrayList<ItemStack>> idToStack = new ArrayList<>();

    static {
        OreDictBridge.registerVanillaDefaults();
    }

    public static int getOreID(String name) {
        Integer id = nameToId.get(name);
        if (id == null) {
            idToName.add(name);
            id = idToName.size() - 1;
            nameToId.put(name, id);
            idToStack.add(new ArrayList<>());
        }
        return id;
    }

    public static String getOreName(int id) {
        return id >= 0 && id < idToName.size() ? idToName.get(id) : "Unknown";
    }

    public static String[] getOreNames() {
        return idToName.toArray(new String[0]);
    }

    public static int[] getOreIDs(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return new int[0];
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < idToStack.size(); i++) {
            for (ItemStack s : idToStack.get(i)) {
                if (itemMatches(s, stack, false)) {
                    out.add(i);
                    break;
                }
            }
        }
        for (String name : OreDictBridge.modernNamesFor(stack)) {
            int id = getOreID(name);
            if (!out.contains(id)) out.add(id);
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    @Deprecated
    public static int getOreID(ItemStack stack) {
        int[] ids = getOreIDs(stack);
        return ids.length == 0 ? -1 : ids[0];
    }

    /** All stacks registered under {@code name}, plus modern items in the equivalent tag. */
    public static ArrayList<ItemStack> getOres(String name) {
        ArrayList<ItemStack> backing = idToStack.get(getOreID(name));
        ArrayList<ItemStack> out = new UnmodifiableArrayList<>(backing);
        for (ItemStack modern : OreDictBridge.modernItemsFor(name)) {
            boolean dupe = false;
            for (ItemStack s : out) if (itemMatches(s, modern, false)) dupe = true;
            if (!dupe) out.add(modern);
        }
        return out;
    }

    /**
     * Forge returns this view of a registration list; DragonAPI reaches the mutable backing list through the private
     * {@code list} field, so the field and the class name are part of the contract.
     */
    private static class UnmodifiableArrayList<E> extends ArrayList<E> {
        private final ArrayList<E> list;

        UnmodifiableArrayList(ArrayList<E> list) {
            super(list);
            this.list = list;
        }
    }

    /** The shared empty result for unknown names ({@link #getOres(String, boolean)} without creating the entry). */
    public static final ArrayList<ItemStack> EMPTY_LIST = new UnmodifiableArrayList<>(new ArrayList<>());

    /**
     * 1.7.10.2+: {@code alwaysCreateEntry} registers the name when it is unknown; otherwise an unknown name gives
     * {@link #EMPTY_LIST} (unless modern items carry the equivalent tag) and is not registered.
     */
    public static ArrayList<ItemStack> getOres(String name, boolean alwaysCreateEntry) {
        if (alwaysCreateEntry || name == null || nameToId.containsKey(name)) return getOres(name);
        List<ItemStack> modern = OreDictBridge.modernItemsFor(name);
        if (modern == null || modern.isEmpty()) return EMPTY_LIST;
        return new UnmodifiableArrayList<>(new ArrayList<>(modern));
    }

    public static void rebakeMap() {
    }

    public static ArrayList<ItemStack> getOres(Integer id) {
        return getOres(getOreName(id));
    }

    public static boolean doesOreNameExist(String name) {
        return nameToId.containsKey(name);
    }

    public static boolean containsMatch(boolean strict, ItemStack[] inputs, ItemStack... targets) {
        for (ItemStack input : inputs) for (ItemStack target : targets) if (itemMatches(target, input, strict)) return true;
        return false;
    }

    public static boolean containsMatch(boolean strict, List<ItemStack> inputs, ItemStack... targets) {
        return containsMatch(strict, inputs.toArray(new ItemStack[0]), targets);
    }

    public static boolean itemMatches(ItemStack target, ItemStack input, boolean strict) {
        if (input == null && target != null || input != null && target == null) return false;
        if (input == null) return true;
        return target.getItem() == input.getItem()
                && (target.getItemDamage() == WILDCARD_VALUE && !strict || target.getItemDamage() == input.getItemDamage());
    }

    public static void registerOre(String name, Item ore) {
        registerOre(name, new ItemStack(ore));
    }

    public static void registerOre(String name, Block ore) {
        registerOre(name, new ItemStack(ore));
    }

    public static void registerOre(String name, ItemStack ore) {
        if (name == null || name.isEmpty() || ore == null || ore.getItem() == null) return;
        int id = getOreID(name);
        ItemStack copy = ore.copy();
        idToStack.get(id).add(copy);
        OreDictBridge.onRegister(name, copy);
        MinecraftForge.EVENT_BUS.post(new OreRegisterEvent(name, copy));
    }

    /** Snapshot of every legacy registration, for tag generation. */
    public static Map<String, List<ItemStack>> mcc$registrations() {
        Map<String, List<ItemStack>> out = new HashMap<>();
        for (int i = 0; i < idToName.size(); i++) if (!idToStack.get(i).isEmpty()) out.put(idToName.get(i), idToStack.get(i));
        return out;
    }

    public static class OreRegisterEvent extends Event {
        public final String Name;
        public final ItemStack Ore;

        public OreRegisterEvent(String name, ItemStack ore) {
            this.Name = name;
            this.Ore = ore;
        }
    }
}
