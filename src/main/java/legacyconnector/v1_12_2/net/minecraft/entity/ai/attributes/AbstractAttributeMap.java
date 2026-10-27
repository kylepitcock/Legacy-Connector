package legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes;

import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;

/**
 * The attributes of one entity. Attributes with a modern counterpart (max health, speed, armor, ...) resolve to the modern entity's
 * real attribute instance; mod attributes are kept here.
 */
public abstract class AbstractAttributeMap {
    protected final Map<IAttribute, IAttributeInstance> attributes = new LinkedHashMap<>();
    protected final Map<String, IAttributeInstance> attributesByName = new LinkedHashMap<>();
    private java.util.function.Supplier<LivingEntity> mcc$entity = () -> null;

    public void mcc$bind(java.util.function.Supplier<LivingEntity> entity) {
        this.mcc$entity = entity;
    }

    /** The view of the modern instance for a vanilla attribute, or null. */
    protected IAttributeInstance mcc$modernInstance(IAttribute attribute) {
        LivingEntity l = mcc$entity.get();
        if (l == null || attribute == null) return null;
        var holder = attribute.mcc$modern();
        if (holder == null) return null;
        var instance = l.getAttribute(holder);
        return instance == null ? null : new org.kylepitcock.legacyconnector.v1_12_2.entity.AttributeView1122(attribute, instance);
    }

    public IAttributeInstance getAttributeInstance(IAttribute attribute) {
        if (attribute == null) return null;
        IAttributeInstance modern = mcc$modernInstance(attribute);
        if (modern != null) return modern;
        IAttributeInstance local = attributes.get(attribute);
        if (local == null) local = attributesByName.get(attribute.getName());
        return local;
    }

    public IAttributeInstance getAttributeInstanceByName(String name) {
        IAttributeInstance local = attributesByName.get(name);
        if (local != null && local.getAttribute().mcc$modern() == null) return local;
        IAttribute vanilla = legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes.mcc$byName(name);
        if (vanilla == null && local != null) vanilla = local.getAttribute();
        if (vanilla != null) {
            IAttributeInstance modern = mcc$modernInstance(vanilla);
            if (modern != null) return modern;
        }
        return local;
    }

    public IAttributeInstance registerAttribute(IAttribute attribute) {
        if (attribute == null) return null;
        if (attributesByName.containsKey(attribute.getName())) {
            IAttributeInstance modern = mcc$modernInstance(attribute);
            return modern != null ? modern : attributesByName.get(attribute.getName());
        }
        IAttributeInstance instance = createInstance(attribute);
        attributes.put(attribute, instance);
        attributesByName.put(attribute.getName(), instance);
        for (IAttribute parent = attribute.getParent(); parent != null; parent = parent.getParent()) {
            attributes.putIfAbsent(parent, instance);
        }
        IAttributeInstance modern = mcc$modernInstance(attribute);
        return modern != null ? modern : instance;
    }

    protected abstract IAttributeInstance createInstance(IAttribute attribute);

    public Collection<IAttributeInstance> getAllAttributes() {
        Collection<IAttributeInstance> out = new ArrayList<>();
        for (IAttributeInstance local : attributesByName.values()) {
            IAttributeInstance modern = mcc$modernInstance(local.getAttribute());
            out.add(modern != null ? modern : local);
        }
        return out;
    }

    public void onAttributeModified(IAttributeInstance instance) {
    }

    public void removeAttributeModifiers(Multimap<String, AttributeModifier> modifiers) {
        for (var e : modifiers.entries()) {
            IAttributeInstance i = getAttributeInstanceByName(e.getKey());
            if (i != null) i.removeModifier(e.getValue());
        }
    }

    public void applyAttributeModifiers(Multimap<String, AttributeModifier> modifiers) {
        for (var e : modifiers.entries()) {
            IAttributeInstance i = getAttributeInstanceByName(e.getKey());
            if (i != null) {
                i.removeModifier(e.getValue());
                i.applyModifier(e.getValue());
            }
        }
    }
}
