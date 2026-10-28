package legacyconnector.v1_12_2.net.minecraftforge.common.crafting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient;

/** Forge: an ingredient matching what any of its children matches. */
public class CompoundIngredient extends Ingredient {
    private final Collection<Ingredient> children;
    private ItemStack[] stacks;

    public CompoundIngredient(Collection<Ingredient> children) {
        super(0);
        this.children = children;
    }

    @Override
    public ItemStack[] getMatchingStacks() {
        if (stacks == null) {
            List<ItemStack> all = new ArrayList<>();
            for (Ingredient child : children) for (ItemStack s : child.getMatchingStacks()) all.add(s);
            stacks = all.toArray(new ItemStack[0]);
        }
        return stacks;
    }

    @Override
    public boolean apply(ItemStack target) {
        if (target == null) return false;
        for (Ingredient child : children) if (child.apply(target)) return true;
        return false;
    }

    @Override
    public boolean isSimple() {
        for (Ingredient child : children) if (!child.isSimple()) return false;
        return true;
    }

    public Collection<Ingredient> getChildren() {
        return children;
    }
}
