package org.kylepitcock.legacyconnector.loader.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EntityLangKeysTest {
    @Test
    void entityNamesGetTheModernTypeKey() {
        Map<String, String> out = new LinkedHashMap<>();
        LegacyResourceConverter.parseLang("entity.srparasites.pri_longarms.name=Primitive Longarms\nitem.foo.name=Foo\n", out);
        assertEquals("Primitive Longarms", out.get("entity.srparasites.pri_longarms"));
        assertEquals("Primitive Longarms", out.get("entity.srparasites.pri_longarms.name"));
        assertFalse(out.containsKey("item.foo"));
    }

    @Test
    void existingModernKeyIsKept() {
        Map<String, String> out = new LinkedHashMap<>();
        LegacyResourceConverter.parseLang("entity.m.x.name=Old\nentity.m.x=New\n", out);
        assertEquals("New", out.get("entity.m.x"));
    }

    @Test
    void nameWithMoreSegmentsIsLeftAlone() {
        Map<String, String> out = new LinkedHashMap<>();
        LegacyResourceConverter.parseLang("entity.a.b.c.name=Deep\n", out);
        assertFalse(out.containsKey("entity.a.b.c"));
    }
}
