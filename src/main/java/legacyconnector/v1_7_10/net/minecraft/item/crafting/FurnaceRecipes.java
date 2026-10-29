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
        experienceList.put(output, xp);
        RecipeBridge.addSmelting(input, output, xp);
    }

    public ItemStack getSmeltingResult(ItemStack input) {
        for (Object o : smeltingList.entrySet()) {
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            if (func_151397_a(input, (ItemStack) e.getKey()) && !RecipeBridge.removedByDataPack(e.getKey())) return (ItemStack) e.getValue();
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

    @SuppressWarnings("rawtypes")
    public Map getSmeltingList() {
        return smeltingList;
    }

    public float getSmeltingExperience(ItemStack stack) {
        for (Object o : experienceList.entrySet()) {
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            if (func_151397_a(stack, (ItemStack) e.getKey())) return (Float) e.getValue();
        }
        return 0.0F;
    }
}
