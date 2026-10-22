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
            if (func_151397_a(input, (ItemStack) e.getKey())) return (ItemStack) e.getValue();
        }
        return null;
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
