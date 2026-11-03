package legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Attribute instance held by the legacy object itself (mod-defined attributes with no modern counterpart). Keeps 1.7.10's
 * bookkeeping: modifiers indexed by operation, name and UUID, a cached value, and the "changed since last sync" flag that
 * puts the instance into the owning map's update set.
 */
public class ModifiableAttributeInstance implements IAttributeInstance {
    private final BaseAttributeMap attributeMap;
    private final IAttribute genericAttribute;
    private final Map<Integer, Set<AttributeModifier>> mapByOperation = new HashMap<>();
    private final Map<String, Set<AttributeModifier>> mapByName = new HashMap<>();
    private final Map<UUID, AttributeModifier> mapByUUID = new LinkedHashMap<>();
    private double baseValue;
    private boolean needsUpdate = true;
    private double cachedValue;

    public ModifiableAttributeInstance(BaseAttributeMap map, IAttribute attribute) {
        this.attributeMap = map;
        this.genericAttribute = attribute;
        this.baseValue = attribute.getDefaultValue();
        for (int i = 0; i < 3; i++) mapByOperation.put(i, new HashSet<>());
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
        if (value == this.baseValue) return;
        this.baseValue = value;
        flagForUpdate();
    }

    @Override
    /** The SRG name of {@link #getModifiersByOperation} (1.7.10's mappings name it {@code getModifiersByOperation}); shim code still calls this one. */
    public Collection<AttributeModifier> func_111130_a(int operation) {
        return getModifiersByOperation(operation);
    }

    public Collection<AttributeModifier> getModifiersByOperation(int operation) {
        Set<AttributeModifier> set = mapByOperation.get(operation);
        return set == null ? new ArrayList<>() : new ArrayList<>(set);
    }

    @Override
    public Collection<AttributeModifier> func_111122_c() {
        return new ArrayList<>(mapByUUID.values());
    }

    @Override
    public AttributeModifier getModifier(UUID id) {
        return mapByUUID.get(id);
    }

    @Override
    public void applyModifier(AttributeModifier modifier) {
        if (mapByUUID.containsKey(modifier.getID())) throw new IllegalArgumentException("Modifier is already applied on this attribute!");
        mapByName.computeIfAbsent(modifier.getName(), k -> new HashSet<>()).add(modifier);
        mapByOperation.computeIfAbsent(modifier.getOperation(), k -> new HashSet<>()).add(modifier);
        mapByUUID.put(modifier.getID(), modifier);
        flagForUpdate();
    }

    /** 1.7.10: marks the cached value stale and queues this instance for the next attribute sync. */
    protected void flagForUpdate() {
        this.needsUpdate = true;
        this.attributeMap.addInstanceToUpdate(this);
    }

    @Override
    public void removeModifier(AttributeModifier modifier) {
        for (int op = 0; op < 3; op++) {
            Set<AttributeModifier> set = mapByOperation.get(op);
            if (set != null) set.remove(modifier);
        }
        Set<AttributeModifier> named = mapByName.get(modifier.getName());
        if (named != null) {
            named.remove(modifier);
            if (named.isEmpty()) mapByName.remove(modifier.getName());
        }
        mapByUUID.remove(modifier.getID());
        flagForUpdate();
    }

    @Override
    public void removeAllModifiers() {
        for (AttributeModifier m : func_111122_c()) removeModifier(m);
    }

    @Override
    public double getAttributeValue() {
        if (needsUpdate) {
            cachedValue = computeValue();
            needsUpdate = false;
        }
        return cachedValue;
    }

    /** 1.7.10 order: additions, then multiples of the base, then multiples of the running total; then clamped. */
    private double computeValue() {
        double v = baseValue;
        for (AttributeModifier m : func_111130_a(0)) v += m.getAmount();
        double total = v;
        for (AttributeModifier m : func_111130_a(1)) total += v * m.getAmount();
        for (AttributeModifier m : func_111130_a(2)) total *= 1.0 + m.getAmount();
        // Synthesized attribute constants have no range to clamp to.
        return genericAttribute.getAttributeUnlocalizedName() == null ? total : genericAttribute.clampValue(total);
    }
}
