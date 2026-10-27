package legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes;

import java.util.Collection;
import java.util.UUID;

/** One entity's value of an attribute: a base value and modifiers. Entities hand out views over their modern attribute instances. */
public interface IAttributeInstance {
    IAttribute getAttribute();

    double getBaseValue();

    void setBaseValue(double value);

    Collection<AttributeModifier> getModifiersByOperation(int operation);

    Collection<AttributeModifier> getModifiers();

    boolean hasModifier(AttributeModifier modifier);

    AttributeModifier getModifier(UUID id);

    void applyModifier(AttributeModifier modifier);

    void removeModifier(AttributeModifier modifier);

    void removeModifier(UUID id);

    void removeAllModifiers();

    double getAttributeValue();
}
