package legacyconnector.v1_20_1.net.minecraftforge.common.crafting;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.crafting.Ingredient;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.MigrateContext;

/**
 * Forge 1.20.1's serializer of plain (vanilla) ingredients, kept as a singleton ({@code INSTANCE}); NeoForge 1.21.1 dropped the class. Reads and writes go
 * through the codecs of {@link Ingredient}; the network form needs registry access, taken from the running game.
 */
public class VanillaIngredientSerializer implements IIngredientSerializer<Ingredient> {
    public static final VanillaIngredientSerializer INSTANCE = new VanillaIngredientSerializer();

    @Override
    public Ingredient parse(FriendlyByteBuf buffer) {
        return Ingredient.CONTENTS_STREAM_CODEC.decode(new RegistryFriendlyByteBuf(buffer, MigrateContext.registries()));
    }

    @Override
    public Ingredient parse(JsonObject json) {
        return CraftingHelper.getIngredient(json, false);
    }

    @Override
    public void write(FriendlyByteBuf buffer, Ingredient ingredient) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(new RegistryFriendlyByteBuf(buffer, MigrateContext.registries()), ingredient);
    }
}
