package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.EntityRegistry;
import legacyconnector.vb1_7_3.net.minecraft.src.ModLoader;
import org.junit.jupiter.api.Test;

class EntityRegistryB173Test {
    @Test void originalReflectionAliasesExposeTheSameLiveNameMap() throws Exception {
        var map=EntityRegistry.stringToClassMapping;
        assertSame(map,ModLoader.getPrivateValue(EntityRegistry.class,null,"a"));
        assertSame(map,ModLoader.getPrivateValue(EntityRegistry.class,null,"stringToClassMapping"));
        assertSame(map,ModLoader.getPrivateValue(EntityRegistry.class,null,"field_1611_a"));
        for(var registration:ContentB173.entities())assertEquals(registration.type(),map.get(registration.name()));
        assertNull(map.get("absent_registry_probe"));
    }
    @Test void mapCapturedBeforeRegistrationSeesLaterOriginalEntityNames() throws Exception {
        var map=EntityRegistry.stringToClassMapping;
        var field=ContentB173.class.getDeclaredField("ENTITIES");field.setAccessible(true);
        @SuppressWarnings("unchecked") var registrations=(java.util.List<ContentB173.Registration>)field.get(null);
        int size=registrations.size();
        try {
            ContentB173.registerEntity(legacyconnector.vb1_7_3.net.minecraft.entity.Entity.class,"RegistryLateProbe",31944);
            assertEquals(legacyconnector.vb1_7_3.net.minecraft.entity.Entity.class,map.get("RegistryLateProbe"));
            assertTrue(map.entrySet().stream().anyMatch(entry->entry.getKey().equals("RegistryLateProbe")));
        } finally {registrations.subList(size,registrations.size()).clear();}
    }
}
