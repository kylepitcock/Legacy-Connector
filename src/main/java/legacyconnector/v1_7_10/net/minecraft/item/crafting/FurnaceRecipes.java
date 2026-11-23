package legacyconnector.v1_7_10.net.minecraft.item.crafting;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.RecipeBridge;

/** Legacy smelting registry; registrations become modern smelting recipes. Lookups see legacy registrations. */
public class FurnaceRecipes {
    private static final FurnaceRecipes smeltingBase = new FurnaceRecipes();
    @SuppressWarnings("rawtypes")
    private final Map smeltingList = new HashMap();
    @SuppressWarnings("rawtypes")
    private final Map experienceList = new HashMap();
    /** Forge 3.x's {@code FurnaceRecipes.metaSmeltingList}: (item id, damage) to the result, for the registrations of one damage value (read by name by mods that merge recipes). */
    @SuppressWarnings("rawtypes")
    public final Map metaSmeltingList = new HashMap();

    public static FurnaceRecipes smelting() {
        return smeltingBase;
    }

    public void addSmeltingRecipeForBlock(Block input, ItemStack output, float xp) {
        addSmelting(Item.getItemFromBlock(input), output, xp);
    }

    public void addSmelting(Item input, ItemStack output, float xp) {
        addSmeltingRecipe(new ItemStack(input, 1, 32767), output, xp);
    }

    @SuppressWarnings("unchecked")
    public void addSmeltingRecipe(ItemStack input, ItemStack output, float xp) {
        smeltingList.put(input, output);
        if (input.getItem() != null && input.getItemDamage() != 32767) {
            metaSmeltingList.put(java.util.Arrays.asList(legacyconnector.v1_6_4.net.minecraft.util.Ids.itemId(input.getItem()), input.getItemDamage()), output);
        }
        experienceList.put(output, xp);
        RecipeBridge.addSmelting(input, output, xp);
    }

    public ItemStack getSmeltingResult(ItemStack input) {
        if (input == null) return null;
        getSmeltingList();
        for (Object o : smeltingList.entrySet()) {
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            if (!func_151397_a(input, (ItemStack) e.getKey())) continue;
            // a vanilla entry: once a server runs the recipe manager decides (a data pack may have removed or changed it)
            if (vanilla.contains(e.getKey()) && net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer() != null) return mcc$modernResult(input);
            // a data pack or KubeJS changed or removed the translated recipe: the recipe manager decides
            if (RecipeBridge.replacementOf(e.getKey()) != null) return mcc$modernResult(input);
            return (ItemStack) e.getValue();
        }
        return mcc$modernResult(input);
    }

    private static volatile Object mcc$cacheOwner;
    private static final Map<net.minecraft.world.item.Item, net.minecraft.world.item.ItemStack> MCC$MODERN = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * The vanilla smelting recipes 1.7.10's table held live in the modern recipe manager: once a server runs, an input no legacy
     * recipe takes is looked up there (cached per item, as smelting inputs are matched by item), so mod furnaces and machines
     * built on {@code getSmeltingResult} smelt ores and food as before.
     */
    private static ItemStack mcc$modernResult(ItemStack input) {
        if (input == null || input.getItem() == null) return null;
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        var level = server.overworld();
        if (level == null) return null;
        var manager = server.getRecipeManager();
        if (mcc$cacheOwner != manager) {
            MCC$MODERN.clear();
            mcc$cacheOwner = manager;
        }
        net.minecraft.world.item.ItemStack modern = ItemStack.mcc$toModern(input);
        if (modern.isEmpty()) return null;
        net.minecraft.world.item.ItemStack result = MCC$MODERN.computeIfAbsent(modern.getItem(), item -> {
            try {
                var in = new net.minecraft.world.item.crafting.SingleRecipeInput(new net.minecraft.world.item.ItemStack(item));
                return manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, in, level)
                        .map(h -> h.value().getResultItem(level.registryAccess()).copy()).orElse(net.minecraft.world.item.ItemStack.EMPTY);
            } catch (RuntimeException e) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
        });
        return result.isEmpty() ? null : ItemStack.mcc$wrap(result.copy());
    }

    public boolean func_151397_a(ItemStack a, ItemStack b) {
        return b.getItem() == a.getItem() && (b.getItemDamage() == 32767 || b.getItemDamage() == a.getItemDamage());
    }

    /** The input keys of the vanilla entries (modern recipes already, so never translated again). */
    private final java.util.Set<Object> vanilla = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private final java.util.concurrent.atomic.AtomicBoolean seeded = new java.util.concurrent.atomic.AtomicBoolean();

    /**
     * 1.7.10's list held vanilla's smelting recipes, and mods copy it into their own machines (a powered furnace) while loading, when
     * the modern recipe manager does not exist yet: so it is seeded once with the 1.7.10 set (the map only: no recipes are translated,
     * vanilla has them). Claimed without a lock because resolving the init fields may wait for another thread's class initialisation.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map getSmeltingList() {
        if (seeded.compareAndSet(false, true)) {
            for (String[] r : new String[][] {
                {"B", "sand", "32767", "B", "glass", "0", "0.1"}, {"B", "iron_ore", "32767", "I", "iron_ingot", "0", "0.7"},
                {"B", "gold_ore", "32767", "I", "gold_ingot", "0", "1.0"}, {"B", "diamond_ore", "32767", "I", "diamond", "0", "1.0"},
                {"B", "emerald_ore", "32767", "I", "emerald", "0", "1.0"}, {"B", "lapis_ore", "32767", "I", "dye", "4", "0.2"},
                {"B", "coal_ore", "32767", "I", "coal", "0", "0.1"}, {"B", "redstone_ore", "32767", "I", "redstone", "0", "0.7"},
                {"B", "quartz_ore", "32767", "I", "quartz", "0", "0.2"}, {"B", "log", "32767", "I", "coal", "1", "0.15"},
                {"B", "log2", "32767", "I", "coal", "1", "0.15"}, {"B", "netherrack", "32767", "I", "netherbrick", "0", "0.1"},
                {"B", "clay", "32767", "B", "hardened_clay", "0", "0.35"}, {"B", "cactus", "32767", "I", "dye", "2", "0.2"},
                {"B", "cobblestone", "32767", "B", "stone", "0", "0.1"}, {"B", "stonebrick", "0", "B", "stonebrick", "2", "0.1"},
                {"I", "clay_ball", "32767", "I", "brick", "0", "0.3"}, {"I", "porkchop", "32767", "I", "cooked_porkchop", "0", "0.35"},
                {"I", "beef", "32767", "I", "cooked_beef", "0", "0.35"}, {"I", "chicken", "32767", "I", "cooked_chicken", "0", "0.35"},
                {"I", "fish", "0", "I", "cooked_fished", "0", "0.35"}, {"I", "fish", "1", "I", "cooked_fished", "1", "0.35"},
                {"I", "potato", "32767", "I", "baked_potato", "0", "0.35"},
            }) {
                try {
                    ItemStack in = mcc$stack(r[0], r[1], Integer.parseInt(r[2]));
                    ItemStack out = mcc$stack(r[3], r[4], Integer.parseInt(r[5]));
                    if (in.getItem() == null || out.getItem() == null) continue;
                    smeltingList.put(in, out);
                    experienceList.put(out, Float.parseFloat(r[6]));
                    vanilla.add(in);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    // an entry the host cannot resolve is left out
                }
            }
            mcc$seedModern();
        }
        return smeltingList;
    }

    /**
     * What the game smelts that 1.7.10 did not (raw ores, wood and stripped logs, glazed terracotta, ancient debris...): a mod machine that
     * copies the list at load would never smelt it. Read from the vanilla pack, tags expanded; inputs an entry already takes are skipped.
     */
    @SuppressWarnings("unchecked")
    private void mcc$seedModern() {
        try {
            for (var e : org.kylepitcock.legacyconnector.core.ModernSmelting.entries()) {
                ItemStack in = ItemStack.mcc$wrap(new net.minecraft.world.item.ItemStack(e.input()));
                if (in == null || in.getItem() == null) continue;
                if (!in.getItem().getHasSubtypes()) in = new ItemStack(in.getItem(), 1, 32767);
                boolean listed = false;
                for (Object k : smeltingList.keySet()) {
                    if (func_151397_a(in, (ItemStack) k)) {
                        listed = true;
                        break;
                    }
                }
                if (listed) continue;
                ItemStack out = ItemStack.mcc$wrap(e.result().copy());
                if (out == null) continue;
                smeltingList.put(in, out);
                experienceList.put(out, e.experience());
                vanilla.add(in);
            }
        } catch (RuntimeException | LinkageError e) {
            // without the vanilla pack the 1.7.10 entries stay
        }
    }

    private static ItemStack mcc$stack(String kind, String field, int damage) throws ReflectiveOperationException {
        if (kind.equals("B")) {
            return new ItemStack((Block) legacyconnector.v1_7_10.net.minecraft.init.Blocks.class.getField(field).get(null), 1, damage);
        }
        return new ItemStack((Item) legacyconnector.v1_7_10.net.minecraft.init.Items.class.getField(field).get(null), 1, damage);
    }

    public float getSmeltingExperience(ItemStack stack) {
        for (Object o : experienceList.entrySet()) {
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            if (func_151397_a(stack, (ItemStack) e.getKey())) return (Float) e.getValue();
        }
        return 0.0F;
    }
}
