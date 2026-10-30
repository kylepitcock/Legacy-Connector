package legacyconnector.v1_12_2.net.minecraft.block.state;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;

/** 1.12.2 base of block states: cycling a property and the "block[prop=value,...]" form; mods' own state classes extend it. */
public abstract class BlockStateBase implements IBlockState {
    private static final Joiner COMMA_JOINER = Joiner.on(',');
    private static final Function<Map.Entry<IProperty<?>, Comparable<?>>, String> MAP_ENTRY_TO_STRING = entry -> {
        if (entry == null) return "<NULL>";
        IProperty<?> property = entry.getKey();
        return property.getName() + "=" + mcc$valueName(property, entry.getValue());
    };

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String mcc$valueName(IProperty property, Comparable value) {
        return property.getName(value);
    }

    public BlockStateBase() {
    }

    @Override
    public <T extends Comparable<T>> IBlockState cycleProperty(IProperty<T> property) {
        return withProperty(property, cyclePropertyValue(property.getAllowedValues(), getValue(property)));
    }

    protected static <T> T cyclePropertyValue(Collection<T> values, T currentValue) {
        Iterator<T> iterator = values.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().equals(currentValue)) {
                if (iterator.hasNext()) return iterator.next();
                return values.iterator().next();
            }
        }
        return iterator.next();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getBlock().getRegistryName());
        if (!getProperties().isEmpty()) {
            sb.append('[');
            COMMA_JOINER.appendTo(sb, com.google.common.collect.Iterables.transform(getProperties().entrySet(), MAP_ENTRY_TO_STRING));
            sb.append(']');
        }
        return sb.toString();
    }
}
