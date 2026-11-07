package legacyconnector.v1_20_1.net.minecraftforge.common.crafting;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Forge 1.20.1's custom ingredient serializer. NeoForge 1.21.1 reads ingredients through codecs ({@code ICustomIngredient}); the interface exists so mods that
 * declare serializers link, {@code CraftingHelper.register} remembers them, and a recipe naming such an ingredient type is not understood by 1.21.1.
 */
public interface IIngredientSerializer<T extends Ingredient> {
    T parse(FriendlyByteBuf buffer);

    T parse(JsonObject json);

    void write(FriendlyByteBuf buffer, T ingredient);
}
