package legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** An attribute instance the legacy entity holds itself (mod attributes with no modern counterpart). */
public class ModifiableAttributeInstance implements IAttributeInstance {
    private final AbstractAttributeMap attributeMap;
    private final IAttribute genericAttribute;
    private final Map<UUID, AttributeModifier> modifiers = new LinkedHashMap<>();
    private double baseValue;
    private boolean needsUpdate = true;
    private double cachedValue;

    public ModifiableAttributeInstance(AbstractAttributeMap map, IAttribute attribute) {
        this.attributeMap = map;
        this.genericAttribute = attribute;
        this.baseValue = attribute.getDefaultValue();
    }

    @Override
    public IAttribute getAttribute() {
        return genericAttribute;
    }

    @Override
    public double getBaseValue() {
        return baseValue;
    }

    @Override
    public void setBaseValue(double value) {
        if (value == baseValue) return;
        this.baseValue = value;
        flagForUpdate();
    }

    @Override
    public Collection<AttributeModifier> getModifiersByOperation(int operation) {
        List<AttributeModifier> out = new ArrayList<>();
        for (AttributeModifier m : modifiers.values()) if (m.getOperation() == operation) out.add(m);
        return out;
    }

    @Override
    public Collection<AttributeModifier> getModifiers() {
        return new ArrayList<>(modifiers.values());
    }

    @Override
    public boolean hasModifier(AttributeModifier modifier) {
        return modifier != null && modifiers.containsKey(modifier.getID());
    }

    @Override
    public AttributeModifier getModifier(UUID id) {
        return modifiers.get(id);
    }

    @Override
    public void applyModifier(AttributeModifier modifier) {
        if (modifier == null) return;
        if (modifiers.containsKey(modifier.getID())) throw new IllegalArgumentException("Modifier is already applied on this attribute!");
        modifiers.put(modifier.getID(), modifier);
        flagForUpdate();
    }

    @Override
    public void removeModifier(AttributeModifier modifier) {
        if (modifier != null && modifiers.remove(modifier.getID()) != null) flagForUpdate();
    }

    @Override
    public void removeModifier(UUID id) {
        if (modifiers.remove(id) != null) flagForUpdate();
    }

    @Override
    public void removeAllModifiers() {
        modifiers.clear();
        flagForUpdate();
    }

    protected void flagForUpdate() {
        needsUpdate = true;
        attributeMap.onAttributeModified(this);
    }

    /** 1.12 order: additions, then multiples of the base, then multiples of the running total; then clamped. */
    @Override
    public double getAttributeValue() {
        if (needsUpdate) {
            double v = baseValue;
            for (AttributeModifier m : getModifiersByOperation(0)) v += m.getAmount();
            double total = v;
            for (AttributeModifier m : getModifiersByOperation(1)) total += v * m.getAmount();
            for (AttributeModifier m : getModifiersByOperation(2)) total *= 1.0D + m.getAmount();
            cachedValue = genericAttribute.clampValue(total);
            needsUpdate = false;
        }
        return cachedValue;
    }
}
