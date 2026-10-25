package legacyconnector.v1_12_2.net.minecraft.block.properties;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class PropertyInteger extends PropertyHelper<Integer> {
    private final ImmutableSet<Integer> allowedValues;

    protected PropertyInteger(String name, int min, int max) {
        super(name, Integer.class);
        Set<Integer> set = new java.util.LinkedHashSet<>();
        for (int i = min; i <= max; i++) set.add(i);
        this.allowedValues = ImmutableSet.copyOf(set);
    }

    public static PropertyInteger create(String name, int min, int max) {
        return new PropertyInteger(name, min, max);
    }

    @Override
    public Collection<Integer> getAllowedValues() {
        return allowedValues;
    }

    @Override
    public com.google.common.base.Optional<Integer> parseValue(String value) {
        try {
            Integer i = Integer.valueOf(value);
            return allowedValues.contains(i) ? com.google.common.base.Optional.of(i) : com.google.common.base.Optional.absent();
        } catch (NumberFormatException e) {
            return com.google.common.base.Optional.absent();
        }
    }

    @Override
    public String getName(Integer value) {
        return value.toString();
    }

    /** The vanilla property constants the synthesizer cannot know (BlockFarmland.MOISTURE, BlockCrops.AGE...): named after the field, over the widest range. */
    public static PropertyInteger mcc$synthDefault(String key) {
        return create(key.substring(key.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT), 0, 15);
    }
}
