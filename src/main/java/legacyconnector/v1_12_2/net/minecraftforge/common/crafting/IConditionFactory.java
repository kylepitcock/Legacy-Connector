package legacyconnector.v1_12_2.net.minecraftforge.common.crafting;

import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;

public interface IConditionFactory {
    BooleanSupplier parse(JsonContext context, JsonObject json);
}
