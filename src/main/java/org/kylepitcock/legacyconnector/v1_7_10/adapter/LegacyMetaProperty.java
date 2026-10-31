package org.kylepitcock.legacyconnector.v1_7_10.adapter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * The {@code meta} property of a flattened subtype block (see {@link LegacyBlockAdapter#familyBase()}): only the metadata values
 * that block owns, which need not be a range (logs: 1, 5, 9, 13). Named {@code meta} like {@link LegacyBlockAdapter#META}, so the
 * generated blockstate files and {@code /setblock mod:block_1[meta=5]} read the same.
 */
public final class LegacyMetaProperty extends Property<Integer> {
    private final List<Integer> values;

    LegacyMetaProperty(int[] metas) {
        super("meta", Integer.class);
        int[] sorted = metas.clone();
        Arrays.sort(sorted);
        this.values = Arrays.stream(sorted).boxed().toList();
    }

    @Override
    public Collection<Integer> getPossibleValues() {
        return values;
    }

    @Override
    public String getName(Integer value) {
        return value.toString();
    }

    @Override
    public Optional<Integer> getValue(String value) {
        try {
            Integer v = Integer.valueOf(value);
            return values.contains(v) ? Optional.of(v) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof LegacyMetaProperty p && values.equals(p.values);
    }

    @Override
    public int generateHashCode() {
        return 31 * super.generateHashCode() + values.hashCode();
    }
}
