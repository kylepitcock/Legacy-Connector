package legacyconnector.vb1_7_3.net.minecraft.entity;

import java.util.*;
import org.kylepitcock.legacyconnector.vb1_7_3.ContentB173;

/** Beta's reflective entity-name registry, including registrations made after the map was obtained. */
public final class EntityRegistry {
    public static final Map<String,Class<?>> stringToClassMapping=new AbstractMap<>() {
        @Override public Set<Entry<String,Class<?>>> entrySet(){
            var entries=new LinkedHashMap<String,Class<?>>();
            for(var registration:ContentB173.entities())entries.put(registration.name(),registration.type());
            return Collections.unmodifiableMap(entries).entrySet();
        }
        @Override public Class<?> get(Object name){
            var registrations=ContentB173.entities();
            for(int i=registrations.size()-1;i>=0;i--){var registration=registrations.get(i);if(registration.name().equals(name))return registration.type();}
            return null;
        }
    };
    /** Original obfuscated and MCP field aliases used by ModLoader.getPrivateValue callers. */
    public static final Map<String,Class<?>> idToClass=stringToClassMapping,a=stringToClassMapping,field_1611_a=stringToClassMapping;
    private EntityRegistry(){}
}
