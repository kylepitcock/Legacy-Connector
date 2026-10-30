package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.multipart;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;

/** 1.12.2 multipart condition on one property: {@code "a|b"} any of the values, a leading {@code !} negates. */
public class ConditionPropertyValue implements ICondition {
    private final String key;
    private final String value;

    public ConditionPropertyValue(String keyIn, String valueIn) {
        this.key = keyIn;
        this.value = valueIn;
    }

    @Override
    public Predicate<IBlockState> getPredicate(BlockStateContainer blockState) {
        IProperty<?> property = blockState.getProperty(key);
        if (property == null) throw new RuntimeException(toString() + ": Definition: " + blockState + " has no property: " + key);
        String s = value;
        boolean negate = !s.isEmpty() && s.charAt(0) == '!';
        if (negate) s = s.substring(1);
        String[] parts = s.split("\\|");
        if (parts.length == 0) throw new RuntimeException(toString() + ": empty value for property " + key);
        List<Predicate<IBlockState>> list = new ArrayList<>();
        for (String part : parts) list.add(makePredicate(blockState, property, part));
        Predicate<IBlockState> predicate = list.size() == 1 ? list.get(0) : Predicates.or(list);
        return negate ? Predicates.not(predicate) : predicate;
    }

    private <T extends Comparable<T>> Predicate<IBlockState> makePredicate(BlockStateContainer blockState, IProperty<T> property, String valueIn) {
        com.google.common.base.Optional<T> parsed = property.parseValue(valueIn);
        if (!parsed.isPresent()) throw new RuntimeException(toString() + ": has an unknown value: " + value);
        T expected = parsed.get();
        return state -> state != null && expected.equals(state.getValue(property));
    }

    @Override
    public String toString() {
        return "ConditionPropertyValue{key=" + key + ", value=" + value + "}";
    }
}
