package org.kylepitcock.legacyconnector.v1_7_10.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.core.degraded.Degraded;

class GlNotesTest {
    @BeforeEach
    @AfterEach
    void clear() {
        Degraded.reset();
        Degraded.setQuiet(true);
        GlNotes.reset();
    }

    @Test
    void queriesAndNoopsAreCountedPerFunctionAndPname() {
        for (int i = 0; i < 40; i++) GlNotes.realQuery("glGetInteger", 0x0BA2);
        GlNotes.realQuery("glGetInteger", 0x0BA2 + 1);
        GlNotes.zeroQuery("glGetFloat", 0x0B00);
        for (int i = 0; i < 3; i++) GlNotes.unemulated("glAlphaFunc");
        String owner = "(layer)";
        assertEquals(40, Degraded.count(Degraded.Category.GL_EMULATION, owner, "glGetInteger(0xba2)"));
        assertEquals(1, Degraded.count(Degraded.Category.GL_EMULATION, owner, "glGetInteger(0xba3)"));
        assertEquals(1, Degraded.count(Degraded.Category.GL_EMULATION, owner, "glGetFloat(0xb00) = 0"));
        assertEquals(3, Degraded.count(Degraded.Category.GL_EMULATION, owner, "glAlphaFunc"));
        assertEquals(4, Degraded.distinct(Degraded.Category.GL_EMULATION, owner));
    }
}
