package legacyconnector.vb1_7_3.net.minecraft.entity;

import java.util.*;
import org.kylepitcock.legacyconnector.vb1_7_3.ContentB173;

/** Beta's reflective entity-name registry, including registrations made after the map was obtained. */
public final class EntityRegistry {
    /** Vanilla identities already materialized by the Beta shim; registrations below may replace their names. */
    private static final Map<String,Class<?>> VANILLA=Map.ofEntries(
            Map.entry("Arrow",legacyconnector.vb1_7_3.net.minecraft.entity.projectile.ArrowEntity.class),
            Map.entry("Item",ItemEntity.class),
            Map.entry("Painting",legacyconnector.vb1_7_3.net.minecraft.entity.decoration.painting.PaintingEntity.class),
            Map.entry("Creeper",legacyconnector.vb1_7_3.net.minecraft.entity.mob.CreeperEntity.class),
            Map.entry("Pig",legacyconnector.vb1_7_3.net.minecraft.entity.passive.PigEntity.class),
            Map.entry("Sheep",legacyconnector.vb1_7_3.net.minecraft.entity.passive.SheepEntity.class),
            Map.entry("Cow",legacyconnector.vb1_7_3.net.minecraft.entity.passive.CowEntity.class),
            Map.entry("Chicken",legacyconnector.vb1_7_3.net.minecraft.entity.passive.ChickenEntity.class),
            Map.entry("Squid",legacyconnector.vb1_7_3.net.minecraft.entity.passive.SquidEntity.class),
            Map.entry("Wolf",legacyconnector.vb1_7_3.net.minecraft.entity.passive.WolfEntity.class),
            Map.entry("PrimedTnt",TntEntity.class),Map.entry("FallingSand",FallingBlockEntity.class));
    public static final Map<String,Class<?>> stringToClassMapping=new AbstractMap<>() {
        @Override public Set<Entry<String,Class<?>>> entrySet(){
            var entries=new LinkedHashMap<String,Class<?>>(VANILLA);
            for(var registration:ContentB173.entities())entries.put(registration.name(),registration.type());
            return Collections.unmodifiableMap(entries).entrySet();
        }
        @Override public Class<?> get(Object name){
            var registrations=ContentB173.entities();
            for(int i=registrations.size()-1;i>=0;i--){var registration=registrations.get(i);if(registration.name().equals(name))return registration.type();}
            return VANILLA.get(name);
        }
    };
    /** Original obfuscated and MCP field aliases used by ModLoader.getPrivateValue callers. */
    public static final Map<String,Class<?>> idToClass=stringToClassMapping,a=stringToClassMapping,field_1611_a=stringToClassMapping;
    private EntityRegistry(){}
}
