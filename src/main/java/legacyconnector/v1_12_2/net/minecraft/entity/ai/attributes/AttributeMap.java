package legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes;

/** The attribute map of a living entity on either side. */
public class AttributeMap extends AbstractAttributeMap {
    @Override
    protected IAttributeInstance createInstance(IAttribute attribute) {
        return new ModifiableAttributeInstance(this, attribute);
    }

    /** 1.12's map hands out modifiable instances (local ones, or views of the modern entity's). */
    @Override
    public ModifiableAttributeInstance getAttributeInstance(IAttribute attribute) {
        return super.getAttributeInstance(attribute) instanceof ModifiableAttributeInstance m ? m : null;
    }

    @Override
    public ModifiableAttributeInstance getAttributeInstanceByName(String name) {
        return super.getAttributeInstanceByName(name) instanceof ModifiableAttributeInstance m ? m : null;
    }

    /** Attribute instances whose values changed and must be sent to clients (the modern entity sends its own). */
    public java.util.Set<IAttributeInstance> getDirtyInstances() {
        return new java.util.HashSet<>();
    }

    public java.util.Collection<IAttributeInstance> getWatchedAttributes() {
        java.util.List<IAttributeInstance> out = new java.util.ArrayList<>();
        for (IAttributeInstance i : getAllAttributes()) if (i.getAttribute().getShouldWatch()) out.add(i);
        return out;
    }
}
