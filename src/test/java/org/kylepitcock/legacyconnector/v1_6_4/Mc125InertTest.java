package org.kylepitcock.legacyconnector.v1_6_4;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.Mc125Host;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.ModLoaderHooks125;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** With Minecraft 1.2.5 hosting off (the default), the run-time paths shared with the other versions behave as before. */
class Mc125InertTest {
    @BeforeAll
    static void off() {
        System.clearProperty(Mc125Host.PROPERTY);
    }

    @Test
    void offByDefault() {
        assertFalse(Mc125Host.active());
    }

    @Test
    void tickHooksKeepRunningAndIgnoreUseClockAndTheResult() {
        ModLoaderHooks125.Hooks<String> hooks = new ModLoaderHooks125.Hooks<>();
        hooks.add("clock", true);
        hooks.add("once", false);
        List<String> calls = new ArrayList<>();
        for (int i = 0; i < 2; i++) hooks.run(false, m -> {
            calls.add(m);
            return false; // the 1.2.5 rule would drop the hook here
        });
        assertEquals(List.of("clock", "once", "clock", "once"), calls);
        assertEquals(2, hooks.size());
    }

    @Test
    void modLoaderMpRegistrationsAreNoOpsLikeBefore() {
        legacyconnector.v1_6_4.net.minecraft.src.ModLoaderMp.registerEntityTrackerEntry(Mc125InertTest.class, true, 77);
        assertNull(legacyconnector.v1_6_4.net.minecraft.src.ModLoaderMp.handleEntityTrackerEntries(new Mc125InertTest()));
        legacyconnector.v1_6_4.net.minecraft.src.ModLoaderMp.registerNetClientHandlerEntity(Mc125InertTest.class, true, 77);
        assertNull(legacyconnector.v1_6_4.net.minecraft.src.ModLoaderMp.handleNetClientHandlerEntities(77));
    }
}
