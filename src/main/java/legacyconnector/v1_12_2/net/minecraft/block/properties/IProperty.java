package legacyconnector.v1_12_2.net.minecraft.block.properties;

import java.util.Collection;

public interface IProperty<T extends Comparable<T>> {
    String getName();

    Collection<T> getAllowedValues();

    Class<T> getValueClass();

    String getName(T value);

    com.google.common.base.Optional<T> parseValue(String value);
}
