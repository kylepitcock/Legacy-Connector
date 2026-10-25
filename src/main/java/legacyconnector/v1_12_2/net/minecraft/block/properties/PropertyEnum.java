package legacyconnector.v1_12_2.net.minecraft.block.properties;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.Collection;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;

public class PropertyEnum<T extends Enum<T> & IStringSerializable> extends PropertyHelper<T> {
    private final ImmutableSet<T> allowedValues;
    private final java.util.Map<String, T> nameToValue;

    protected PropertyEnum(String name, Class<T> valueClass, Collection<T> allowedValues) {
        super(name, valueClass);
        this.allowedValues = ImmutableSet.copyOf(allowedValues);
        ImmutableMap.Builder<String, T> b = ImmutableMap.builder();
        for (T t : allowedValues) b.put(t.getName(), t);
        this.nameToValue = b.build();
    }

    public static <T extends Enum<T> & IStringSerializable> PropertyEnum<T> create(String name, Class<T> clazz) {
        return create(name, clazz, com.google.common.base.Predicates.alwaysTrue());
    }

    public static <T extends Enum<T> & IStringSerializable> PropertyEnum<T> create(String name, Class<T> clazz, Predicate<T> filter) {
        return create(name, clazz, Lists.newArrayList(com.google.common.collect.Collections2.filter(Lists.newArrayList(clazz.getEnumConstants()), filter)));
    }

    public static <T extends Enum<T> & IStringSerializable> PropertyEnum<T> create(String name, Class<T> clazz, T... values) {
        return create(name, clazz, Lists.newArrayList(values));
    }

    public static <T extends Enum<T> & IStringSerializable> PropertyEnum<T> create(String name, Class<T> clazz, Collection<T> values) {
        return new PropertyEnum<>(name, clazz, values);
    }

    @Override
    public Collection<T> getAllowedValues() {
        return allowedValues;
    }

    @Override
    public com.google.common.base.Optional<T> parseValue(String value) {
        return com.google.common.base.Optional.fromNullable(nameToValue.get(value));
    }

    @Override
    public String getName(T value) {
        return value.getName();
    }
}
