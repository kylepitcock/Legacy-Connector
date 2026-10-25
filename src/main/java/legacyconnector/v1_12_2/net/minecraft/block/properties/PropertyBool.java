package legacyconnector.v1_12_2.net.minecraft.block.properties;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;

public class PropertyBool extends PropertyHelper<Boolean> {
    private final ImmutableSet<Boolean> allowedValues = ImmutableSet.of(true, false);

    protected PropertyBool(String name) {
        super(name, Boolean.class);
    }

    public static PropertyBool create(String name) {
        return new PropertyBool(name);
    }

    @Override
    public Collection<Boolean> getAllowedValues() {
        return allowedValues;
    }

    @Override
    public com.google.common.base.Optional<Boolean> parseValue(String value) {
        return "true".equals(value) ? com.google.common.base.Optional.of(true)
                : "false".equals(value) ? com.google.common.base.Optional.of(false) : com.google.common.base.Optional.absent();
    }

    @Override
    public String getName(Boolean value) {
        return value.toString();
    }
}
