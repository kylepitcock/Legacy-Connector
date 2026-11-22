package legacyconnector.v1_12_2.net.minecraft.item.crafting;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.RecipeBridge1122;

/** Legacy smelting registry: registrations become modern smelting recipes; one removed from {@link #getSmeltingList()} is left out. */
public class FurnaceRecipes {
    private static final FurnaceRecipes INSTANCE = new FurnaceRecipes();
    /** Input -> output of the registrations, vanilla's included (keys compare by identity, as legacy ItemStacks do). */
    private final java.util.Map<ItemStack, ItemStack> smeltingList = java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<>());

    /** Output -> experience of the mod registrations (the private vanilla map some mods reach by reflection or access transformer). */
    public final java.util.Map<ItemStack, Float> experienceList = java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<>());

    /** The input keys of the vanilla entries: modern recipes already, so never translated again. */
    private final java.util.Set<ItemStack> vanilla = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private final java.util.concurrent.atomic.AtomicBoolean seeded = new java.util.concurrent.atomic.AtomicBoolean();

    public static FurnaceRecipes instance() {
        return INSTANCE;
    }

    public void addSmeltingRecipe(ItemStack input, ItemStack stack, float experience) {
        smeltingList.put(input, stack);
        experienceList.put(stack, experience);
        RecipeBridge1122.addSmelting(input, stack, experience);
    }

    public void addSmeltingRecipeForBlock(Block input, ItemStack stack, float experience) {
        addSmeltingRecipe(new ItemStack(input, 1, 32767), stack, experience);
    }

    public void addSmelting(Item input, ItemStack stack, float experience) {
        addSmeltingRecipe(new ItemStack(input, 1, 32767), stack, experience);
    }

    public ItemStack getSmeltingResult(ItemStack stack) {
        Object[] s = RecipeBridge1122.smeltingFor(stack);
        if (s != null) return (ItemStack) s[1];
        seedVanilla();
        synchronized (smeltingList) {
            for (var e : smeltingList.entrySet()) {
                ItemStack in = e.getKey();
                if (vanilla.contains(in) && in.getItem() == stack.getItem() && (in.getMetadata() == 32767 || in.getMetadata() == stack.getMetadata())) {
                    return e.getValue();
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public float getSmeltingExperience(ItemStack stack) {
        Object[] s = RecipeBridge1122.smeltingFor(stack);
        if (s != null) return (Float) s[2];
        ItemStack r = getSmeltingResult(stack);
        Float xp = r.isEmpty() ? null : experienceList.get(r);
        return xp == null ? 0.0F : xp;
    }

    public java.util.Map<ItemStack, ItemStack> getSmeltingList() {
        seedVanilla();
        return smeltingList;
    }

    /**
     * 1.12.2's table held the vanilla smelting recipes and mods copy it into their own machines (a powered furnace). Here those live in
     * the modern recipe manager, which does not exist while mods load, so the list is seeded with the 1.12.2 set on first use. The
     * entries only go into the map; they are not translated to recipes (vanilla has them).
     */
    private void seedVanilla() {
        // claimed without a lock: resolving the Blocks and Items fields may wait for another thread's class initialisation, which may
        // itself be reading this list (a lock here deadlocked chunk generation)
        if (!seeded.compareAndSet(false, true)) return;
        for (String[] r : new String[][] {
            // input kind, input field, input meta, output kind, output field, output meta, experience
            {"B", "SAND", "32767", "B", "GLASS", "0", "0.1"}, {"B", "IRON_ORE", "32767", "I", "IRON_INGOT", "0", "0.7"},
            {"B", "GOLD_ORE", "32767", "I", "GOLD_INGOT", "0", "1.0"}, {"B", "DIAMOND_ORE", "32767", "I", "DIAMOND", "0", "1.0"},
            {"B", "EMERALD_ORE", "32767", "I", "EMERALD", "0", "1.0"}, {"B", "LAPIS_ORE", "32767", "I", "DYE", "4", "0.2"},
            {"B", "COAL_ORE", "32767", "I", "COAL", "0", "0.1"}, {"B", "REDSTONE_ORE", "32767", "I", "REDSTONE", "0", "0.7"},
            {"B", "QUARTZ_ORE", "32767", "I", "QUARTZ", "0", "0.2"}, {"B", "LOG", "32767", "I", "COAL", "1", "0.15"},
            {"B", "LOG2", "32767", "I", "COAL", "1", "0.15"}, {"B", "NETHERRACK", "32767", "I", "NETHERBRICK", "0", "0.1"},
            {"B", "CLAY", "32767", "B", "HARDENED_CLAY", "0", "0.35"}, {"B", "CACTUS", "32767", "I", "DYE", "2", "0.2"},
            {"B", "COBBLESTONE", "32767", "B", "STONE", "0", "0.1"}, {"B", "STONEBRICK", "0", "B", "STONEBRICK", "2", "0.1"},
            {"B", "SPONGE", "1", "B", "SPONGE", "0", "0.15"}, {"I", "CLAY_BALL", "32767", "I", "BRICK", "0", "0.3"},
            {"I", "PORKCHOP", "32767", "I", "COOKED_PORKCHOP", "0", "0.35"}, {"I", "BEEF", "32767", "I", "COOKED_BEEF", "0", "0.35"},
            {"I", "CHICKEN", "32767", "I", "COOKED_CHICKEN", "0", "0.35"}, {"I", "MUTTON", "32767", "I", "COOKED_MUTTON", "0", "0.35"},
            {"I", "RABBIT", "32767", "I", "COOKED_RABBIT", "0", "0.35"}, {"I", "FISH", "0", "I", "COOKED_FISH", "0", "0.35"},
            {"I", "FISH", "1", "I", "COOKED_FISH", "1", "0.35"}, {"I", "POTATO", "32767", "I", "BAKED_POTATO", "0", "0.35"},
            {"I", "CHORUS_FRUIT", "32767", "I", "CHORUS_FRUIT_POPPED", "0", "0.1"},
        }) {
            try {
                ItemStack in = stack(r[0], r[1], Integer.parseInt(r[2]));
                ItemStack out = stack(r[3], r[4], Integer.parseInt(r[5]));
                if (in.isEmpty() || out.isEmpty()) continue;
                smeltingList.put(in, out);
                experienceList.put(out, Float.parseFloat(r[6]));
                vanilla.add(in);
            } catch (ReflectiveOperationException | RuntimeException e) {
                // an entry the host cannot resolve is left out
            }
        }
    }

    private static ItemStack stack(String kind, String field, int meta) throws ReflectiveOperationException {
        if (kind.equals("B")) {
            return new ItemStack((Block) legacyconnector.v1_12_2.net.minecraft.init.Blocks.class.getField(field).get(null), 1, meta);
        }
        return new ItemStack((Item) legacyconnector.v1_12_2.net.minecraft.init.Items.class.getField(field).get(null), 1, meta);
    }
}
