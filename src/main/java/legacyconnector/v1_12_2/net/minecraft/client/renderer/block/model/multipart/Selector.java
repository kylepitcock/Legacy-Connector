package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.multipart;

import com.google.common.base.Predicate;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.VariantList;

/** 1.12.2 multipart part: the models to {@code apply} {@code when} the condition holds. */
public class Selector {
    private final ICondition condition;
    private final VariantList variantList;

    public Selector(ICondition conditionIn, VariantList variantListIn) {
        if (conditionIn == null) throw new IllegalArgumentException("Missing condition for selector");
        if (variantListIn == null) throw new IllegalArgumentException("Missing variant for selector");
        this.condition = conditionIn;
        this.variantList = variantListIn;
    }

    public VariantList getVariantList() {
        return variantList;
    }

    public Predicate<IBlockState> getPredicate(BlockStateContainer state) {
        return condition.getPredicate(state);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Selector other && condition.equals(other.condition) && variantList.equals(other.variantList);
    }

    @Override
    public int hashCode() {
        return 31 * condition.hashCode() + variantList.hashCode();
    }

    /** A part from blockstate json: {@code {"when": {...}, "apply": ...}}. */
    public static Selector mcc$fromJson(JsonObject json) {
        ICondition condition = json.has("when") ? mcc$condition(json.getAsJsonObject("when")) : ICondition.TRUE;
        if (!json.has("apply")) throw new JsonParseException("Missing apply in multipart selector");
        return new Selector(condition, VariantList.mcc$fromJson(json.get("apply")));
    }

    /** {@code {"OR": [...]}}, or properties that must all match. */
    static ICondition mcc$condition(JsonObject json) {
        if (json.size() == 0) throw new JsonParseException("No elements found in selector");
        if (json.size() == 1 && json.has("OR")) {
            JsonArray array = json.getAsJsonArray("OR");
            List<ICondition> list = new ArrayList<>();
            for (JsonElement e : array) list.add(mcc$condition(e.getAsJsonObject()));
            return new ConditionOr(list);
        }
        List<ICondition> list = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : json.entrySet()) list.add(new ConditionPropertyValue(e.getKey(), e.getValue().getAsString()));
        return list.size() == 1 ? list.get(0) : new ConditionAnd(list);
    }
}
