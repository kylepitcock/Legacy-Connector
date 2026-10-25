package legacyconnector.v1_12_2.net.minecraftforge.common.crafting;

import com.google.gson.JsonObject;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient;

public interface IIngredientFactory {
    Ingredient parse(JsonContext context, JsonObject json);
}
