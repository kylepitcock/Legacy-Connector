package legacyconnector.v1_12_2.net.minecraft.block.properties;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import java.util.Collection;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

public class PropertyDirection extends PropertyEnum<EnumFacing> {
    protected PropertyDirection(String name, Collection<EnumFacing> values) {
        super(name, EnumFacing.class, values);
    }

    public static PropertyDirection create(String name) {
        return create(name, com.google.common.base.Predicates.alwaysTrue());
    }

    public static PropertyDirection create(String name, Predicate<EnumFacing> filter) {
        return create(name, Lists.newArrayList(com.google.common.collect.Collections2.filter(Lists.newArrayList(EnumFacing.values()), filter)));
    }

    public static PropertyDirection create(String name, Collection<EnumFacing> values) {
        return new PropertyDirection(name, values);
    }

    public static PropertyDirection mcc$synthDefault(String key) {
        return create(key.substring(key.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT));
    }
}
