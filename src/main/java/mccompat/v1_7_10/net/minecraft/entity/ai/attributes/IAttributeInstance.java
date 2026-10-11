package mccompat.v1_7_10.net.minecraft.entity.ai.attributes;

import java.util.Collection;
import java.util.UUID;

public interface IAttributeInstance {
    IAttribute getAttribute();

    double getBaseValue();

    void setBaseValue(double value);

    Collection<AttributeModifier> func_111130_a(int operation);

    Collection<AttributeModifier> func_111122_c();

    AttributeModifier getModifier(UUID id);

    void applyModifier(AttributeModifier modifier);

    void removeModifier(AttributeModifier modifier);

    void removeAllModifiers();

    double getAttributeValue();
}
