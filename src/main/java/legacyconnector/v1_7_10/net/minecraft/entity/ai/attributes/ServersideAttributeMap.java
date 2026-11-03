package legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Server-side attribute map: besides the instances it tracks the ones changed since the last sync (what
 * {@code S20PacketEntityProperties} sends) and indexes watched attributes by description.
 */
public class ServersideAttributeMap extends BaseAttributeMap {
    private final Set<IAttributeInstance> attributeInstanceSet = new HashSet<>();
    protected final Map<String, IAttributeInstance> descriptionToAttributeInstanceMap = new HashMap<>();

    @Override
    public IAttributeInstance registerAttribute(IAttribute attribute) {
        IAttributeInstance instance = super.registerAttribute(attribute);
        if (attribute instanceof RangedAttribute r && r.getDescription() != null) {
            descriptionToAttributeInstanceMap.put(r.getDescription(), instance);
        }
        return instance;
    }

    @Override
    public void addInstanceToUpdate(ModifiableAttributeInstance instance) {
        if (instance.getAttribute().getShouldWatch()) attributeInstanceSet.add(instance);
    }

    /** The watched instances changed since the last sync (the sender clears the set after writing the packet). */
    public Set<IAttributeInstance> getAttributeInstanceSet() {
        return attributeInstanceSet;
    }

    /** Every watched attribute's instance (the full property list sent when an entity starts being tracked). */
    public Collection<IAttributeInstance> getWatchedAttributes() {
        List<IAttributeInstance> out = new ArrayList<>();
        for (IAttributeInstance i : getAllAttributes()) if (i.getAttribute().getShouldWatch()) out.add(i);
        return out;
    }
}
