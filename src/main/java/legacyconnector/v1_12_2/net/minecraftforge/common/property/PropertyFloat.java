package legacyconnector.v1_12_2.net.minecraftforge.common.property;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/** Forge: an unlisted float property (fluid corner heights, flow direction), optionally limited to a range. */
public class PropertyFloat implements IUnlistedProperty<Float> {
    private final String name;
    private final Predicate<Float> validator;

    public PropertyFloat(String name) {
        this(name, Predicates.alwaysTrue());
    }

    public PropertyFloat(String name, float min, float max) {
        this(name, f -> f != null && f >= min && f <= max);
    }

    public PropertyFloat(String name, Predicate<Float> validator) {
        this.name = name;
        this.validator = validator;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isValid(Float value) {
        return validator.apply(value);
    }

    @Override
    public Class<Float> getType() {
        return Float.class;
    }

    @Override
    public String valueToString(Float value) {
        return value.toString();
    }
}
