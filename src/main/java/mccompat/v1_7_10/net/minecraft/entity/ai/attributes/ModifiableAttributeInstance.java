package mccompat.v1_7_10.net.minecraft.entity.ai.attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Attribute instance held by the legacy object itself (mod-defined attributes with no modern counterpart). */
public class ModifiableAttributeInstance implements IAttributeInstance {
    private final BaseAttributeMap attributeMap;
    private final IAttribute genericAttribute;
    private final Map<UUID, AttributeModifier> modifiers = new LinkedHashMap<>();
    private double baseValue;

    public ModifiableAttributeInstance(BaseAttributeMap map, IAttribute attribute) {
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
        this.baseValue = value;
    }

    @Override
    public Collection<AttributeModifier> func_111130_a(int operation) {
        List<AttributeModifier> out = new ArrayList<>();
        for (AttributeModifier m : modifiers.values()) if (m.getOperation() == operation) out.add(m);
        return out;
    }

    @Override
    public Collection<AttributeModifier> func_111122_c() {
        return new ArrayList<>(modifiers.values());
    }

    @Override
    public AttributeModifier getModifier(UUID id) {
        return modifiers.get(id);
    }

    @Override
    public void applyModifier(AttributeModifier modifier) {
        if (modifiers.containsKey(modifier.getID())) throw new IllegalArgumentException("Modifier is already applied on this attribute!");
        modifiers.put(modifier.getID(), modifier);
    }

    @Override
    public void removeModifier(AttributeModifier modifier) {
        modifiers.remove(modifier.getID());
    }

    @Override
    public void removeAllModifiers() {
        modifiers.clear();
    }

    /** 1.7.10 order: additions, then multiples of the base, then multiples of the running total; then clamped. */
    @Override
    public double getAttributeValue() {
        double v = baseValue;
        for (AttributeModifier m : func_111130_a(0)) v += m.getAmount();
        double total = v;
        for (AttributeModifier m : func_111130_a(1)) total += v * m.getAmount();
        for (AttributeModifier m : func_111130_a(2)) total *= 1.0 + m.getAmount();
        // Synthesized attribute constants have no range to clamp to.
        return genericAttribute.getAttributeUnlocalizedName() == null ? total : genericAttribute.clampValue(total);
    }
}
