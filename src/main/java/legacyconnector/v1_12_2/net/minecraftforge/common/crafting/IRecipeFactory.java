package legacyconnector.v1_12_2.net.minecraftforge.common.crafting;

import com.google.gson.JsonObject;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe;

public interface IRecipeFactory {
    IRecipe parse(JsonContext context, JsonObject json);
}
