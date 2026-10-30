package legacyconnector.v1_12_2.net.minecraftforge.oredict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** The 1.12.2 ore dictionary: names to stacks (metadata {@link #WILDCARD_VALUE} matches any). */
public class OreDictionary {
    public static final int WILDCARD_VALUE = Short.MAX_VALUE;
    /** The list returned for names nobody registered (shared; mods compare against it). */
    public static final NonNullList<ItemStack> EMPTY_LIST = NonNullList.create();

    private static final List<String> ID_TO_NAME = new ArrayList<>();
    private static final Map<String, Integer> NAME_TO_ID = new HashMap<>();
    private static final Map<Integer, List<ItemStack>> ORES = new LinkedHashMap<>();

    static {
        VanillaOres.register();
    }

    public static synchronized int getOreID(String name) {
        Integer id = NAME_TO_ID.get(name);
        if (id == null) {
            id = ID_TO_NAME.size();
            ID_TO_NAME.add(name);
            NAME_TO_ID.put(name, id);
            ORES.put(id, new ArrayList<>());
        }
        return id;
    }

    /** Forge rebuilt its stack-to-ids cache after registry changes; ids here are looked up from the live lists, so there is nothing to rebuild. */
    public static void rebakeMap() {
    }

    public static synchronized String getOreName(int id) {
        return id >= 0 && id < ID_TO_NAME.size() ? ID_TO_NAME.get(id) : "Unknown";
    }

    public static synchronized String[] getOreNames() {
        return ID_TO_NAME.toArray(new String[0]);
    }

    public static synchronized boolean doesOreNameExist(String name) {
        return NAME_TO_ID.containsKey(name);
    }

    public static synchronized int[] getOreIDs(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return new int[0];
        List<Integer> ids = new ArrayList<>();
        for (var e : ORES.entrySet()) {
            for (ItemStack ore : e.getValue()) {
                if (itemMatches(ore, stack, false)) {
                    ids.add(e.getKey());
                    break;
                }
            }
        }
        int[] out = new int[ids.size()];
        for (int i = 0; i < out.length; i++) out[i] = ids.get(i);
        return out;
    }

    public static synchronized NonNullList<ItemStack> getOres(String name) {
        return getOres(name, true);
    }

    public static synchronized NonNullList<ItemStack> getOres(String name, boolean alwaysCreateEntry) {
        NonNullList<ItemStack> out = NonNullList.create();
        Integer id = alwaysCreateEntry ? Integer.valueOf(getOreID(name)) : NAME_TO_ID.get(name);
        if (id != null) for (ItemStack s : ORES.get(id)) out.add(s);
        return out;
    }

    public static synchronized NonNullList<ItemStack> getOres(Integer id) {
        NonNullList<ItemStack> out = NonNullList.create();
        List<ItemStack> list = ORES.get(id);
        if (list != null) for (ItemStack s : list) out.add(s);
        return out;
    }

    public static boolean itemMatches(ItemStack target, ItemStack input, boolean strict) {
        if (input.isEmpty() && !target.isEmpty() || !input.isEmpty() && target.isEmpty()) return false;
        return target.getItem() == input.getItem()
                && (target.getMetadata() == WILDCARD_VALUE || target.getMetadata() == input.getMetadata() || (input.getMetadata() == WILDCARD_VALUE && !strict));
    }

    public static boolean containsMatch(boolean strict, List<ItemStack> inputs, ItemStack... targets) {
        for (ItemStack input : inputs) for (ItemStack target : targets) if (itemMatches(target, input, strict)) return true;
        return false;
    }

    public static void registerOre(String name, Item ore) {
        registerOre(name, new ItemStack(ore));
    }

    public static void registerOre(String name, Block ore) {
        registerOre(name, new ItemStack(ore));
    }

    public static void registerOre(String name, ItemStack ore) {
        if (ore == null || ore.isEmpty()) return;
        int id = getOreID(name);
        synchronized (OreDictionary.class) {
            for (ItemStack have : ORES.get(id)) if (ItemStack.areItemsEqual(have, ore)) return;
            ORES.get(id).add(ore.copy());
        }
        MinecraftForge.EVENT_BUS.post(new OreRegisterEvent(name, ore));
    }

    public static synchronized Map<String, List<ItemStack>> mcc$registrations() {
        Map<String, List<ItemStack>> out = new LinkedHashMap<>();
        for (var e : ORES.entrySet()) out.put(ID_TO_NAME.get(e.getKey()), new ArrayList<>(e.getValue()));
        return out;
    }

    public static class OreRegisterEvent extends Event {
        private final String name;
        private final ItemStack ore;

        public OreRegisterEvent(String name, ItemStack ore) {
            this.name = name;
            this.ore = ore;
        }

        public String getName() {
            return name;
        }

        public ItemStack getOre() {
            return ore;
        }
    }

    /** Forge's NonNullList form of {@link #containsMatch(boolean, List, ItemStack...)}. */
    public static boolean containsMatch(boolean strict, legacyconnector.v1_12_2.net.minecraft.util.NonNullList<ItemStack> inputs, ItemStack... targets) {
        return containsMatch(strict, (List<ItemStack>) inputs, targets);
    }
}
