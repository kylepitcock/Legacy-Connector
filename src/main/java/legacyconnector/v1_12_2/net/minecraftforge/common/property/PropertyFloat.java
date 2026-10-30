package legacyconnector.v1_12_2.net.minecraftforge.common.property;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/** An unlisted float property (model data a tile entity supplies), optionally limited by a validator. */
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
