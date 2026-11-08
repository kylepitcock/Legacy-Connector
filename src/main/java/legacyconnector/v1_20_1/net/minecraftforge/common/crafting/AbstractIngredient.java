package legacyconnector.v1_20_1.net.minecraftforge.common.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/**
 * Forge 1.20.1's base class of custom ingredients. {@code Ingredient} is final in 1.21.1 (the access transformer of the layer opens it), and custom
 * ingredients are an {@link ICustomIngredient} registered with a type. A subclass written for 1.20.1 is therefore an {@code Ingredient} whose custom part is
 * a {@link Bridge} that forwards to it: its own {@code test}, {@code getItems}, {@code isSimple} keep working, and the type (codec and network form) is made
 * from its 1.20.1 {@link IIngredientSerializer} by {@link CraftingHelper#typeFor}.
 */
public abstract class AbstractIngredient extends Ingredient {
    private final List<Ingredient.Value> values;

    protected AbstractIngredient() {
        this(Stream.empty());
    }

    protected AbstractIngredient(Stream<? extends Ingredient.Value> values) {
        super(new Bridge());
        ((Bridge) getCustomIngredient()).owner = this;
        List<Ingredient.Value> list = new ArrayList<>();
        values.forEach(list::add);
        this.values = list;
    }

    /** The serializer of this ingredient type (1.20.1 had it on {@code Ingredient}). */
    public abstract IIngredientSerializer<? extends Ingredient> getSerializer();

    /** The 1.20.1 JSON form; subclasses override it. */
    public JsonElement toJson() {
        return new JsonObject();
    }

    @Override
    public boolean test(ItemStack stack) {
        if (stack == null) return false;
        for (ItemStack candidate : getItems()) if (ItemStack.isSameItem(candidate, stack)) return true;
        return false;
    }

    @Override
    public ItemStack[] getItems() {
        Set<ItemStack> out = new LinkedHashSet<>();
        for (Ingredient.Value v : values) out.addAll(v.getItems());
        return out.toArray(new ItemStack[0]);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    /** The {@link ICustomIngredient} NeoForge sees: everything goes to the 1.20.1 subclass. */
    public static final class Bridge implements ICustomIngredient {
        Ingredient owner;
        IIngredientSerializer<?> serializer;

        public Bridge() {
        }

        /** For an ingredient that extends {@code Ingredient} directly (Farmers Delight's tool action ingredient) and is read by a 1.20.1 serializer. */
        public Bridge(Ingredient owner, IIngredientSerializer<?> serializer) {
            this.owner = owner;
            this.serializer = serializer;
        }

        public Ingredient owner() {
            return owner;
        }

        /** The 1.20.1 JSON form of the ingredient, if it has one. */
        public JsonElement toJson() {
            if (owner instanceof AbstractIngredient a) return a.toJson();
            try {
                return (JsonElement) owner.getClass().getMethod("toJson").invoke(owner);
            } catch (ReflectiveOperationException | ClassCastException e) {
                return new JsonObject();
            }
        }

        @Override
        public boolean test(ItemStack stack) {
            return owner.test(stack);
        }

        @Override
        public Stream<ItemStack> getItems() {
            return Arrays.stream(owner.getItems());
        }

        @Override
        public boolean isSimple() {
            return owner.isSimple();
        }

        @Override
        public IngredientType<?> getType() {
            return CraftingHelper.typeFor(serializer != null ? serializer : ((AbstractIngredient) owner).getSerializer());
        }
    }
}
