package mccompat.v1_7_10.net.minecraft.entity.ai.attributes;

import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;

/**
 * Legacy attribute map of one entity. Attributes with a modern counterpart (max health, speed, ...) resolve to the
 * modern entity's real attribute instance; others are kept here.
 */
public abstract class BaseAttributeMap {
    protected final Map<IAttribute, IAttributeInstance> attributes = new LinkedHashMap<>();
    protected final Map<String, IAttributeInstance> attributesByName = new LinkedHashMap<>();
    private java.util.function.Supplier<LivingEntity> mcc$entity = () -> null;

    public void mcc$bind(java.util.function.Supplier<LivingEntity> entity) {
        this.mcc$entity = entity;
    }

    public IAttributeInstance getAttributeInstance(IAttribute attribute) {
        if (attribute == null) return null;
        IAttributeInstance modern = SharedMonsterAttributes.mcc$instance(mcc$entity.get(), attribute);
        if (modern != null) return modern;
        return attributes.get(attribute);
    }

    public IAttributeInstance getAttributeInstanceByName(String name) {
        IAttribute vanilla = SharedMonsterAttributes.mcc$byName(name);
        if (vanilla != null) {
            IAttributeInstance modern = SharedMonsterAttributes.mcc$instance(mcc$entity.get(), vanilla);
            if (modern != null) return modern;
        }
        return attributesByName.get(name);
    }

    public IAttributeInstance registerAttribute(IAttribute attribute) {
        IAttributeInstance existing = getAttributeInstance(attribute);
        if (existing != null) return existing;
        IAttributeInstance instance = new ModifiableAttributeInstance(this, attribute);
        attributes.put(attribute, instance);
        attributesByName.put(attribute.getAttributeUnlocalizedName(), instance);
        return instance;
    }

    public Collection<IAttributeInstance> getAllAttributes() {
        Collection<IAttributeInstance> out = new ArrayList<>(attributes.values());
        for (IAttribute a : SharedMonsterAttributes.mcc$vanilla()) {
            IAttributeInstance modern = SharedMonsterAttributes.mcc$instance(mcc$entity.get(), a);
            if (modern != null) out.add(modern);
        }
        return out;
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
