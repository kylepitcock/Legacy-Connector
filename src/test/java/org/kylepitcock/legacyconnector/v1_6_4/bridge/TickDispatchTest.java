package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import legacyconnector.v1_6_4.cpw.mods.fml.common.IScheduledTickHandler;
import legacyconnector.v1_6_4.cpw.mods.fml.common.ITickHandler;
import legacyconnector.v1_6_4.cpw.mods.fml.common.TickType;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import org.junit.jupiter.api.Test;

class TickDispatchTest {
    private static class Recorder implements ITickHandler {
        final EnumSet<TickType> wanted;
        final List<String> calls = new ArrayList<>();

        Recorder(TickType... wanted) {
            this.wanted = EnumSet.copyOf(List.of(wanted));
        }

        @Override
        public void tickStart(EnumSet<TickType> type, Object... tickData) {
            calls.add("start " + type + (tickData.length > 0 ? " " + tickData[0] : ""));
        }

        @Override
        public void tickEnd(EnumSet<TickType> type, Object... tickData) {
            calls.add("end " + type + (tickData.length > 0 ? " " + tickData[0] : ""));
        }

        @Override
        public EnumSet<TickType> ticks() {
            return wanted;
        }

        @Override
        public String getLabel() {
            return "recorder";
        }
    }

    @Test
    void handlersGetOnlyTheirTicksOnTheirSide() {
        TickDispatch d = new TickDispatch();
        Recorder server = new Recorder(TickType.SERVER, TickType.WORLD);
        Recorder client = new Recorder(TickType.CLIENT);
        d.add(server, Side.SERVER);
        d.add(client, Side.CLIENT);
        d.fire(Side.SERVER, TickType.SERVER, false);
        d.fire(Side.SERVER, TickType.SERVER, true);
        d.fire(Side.SERVER, TickType.WORLD, false, "overworld");
        d.fire(Side.SERVER, TickType.PLAYER, false, "steve");
        d.fire(Side.CLIENT, TickType.CLIENT, false);
        d.fire(Side.SERVER, TickType.CLIENT, false);
        assertEquals(List.of("start [SERVER]", "end [SERVER]", "start [WORLD] overworld"), server.calls);
        assertEquals(List.of("start [CLIENT]"), client.calls);
    }

    @Test
    void wantsReflectsTheRegisteredTypesAndSides() {
        TickDispatch d = new TickDispatch();
        d.add(new Recorder(TickType.RENDER), Side.CLIENT);
        assertTrue(d.wants(Side.CLIENT, TickType.RENDER));
        assertFalse(d.wants(Side.SERVER, TickType.RENDER));
        assertFalse(d.wants(Side.CLIENT, TickType.WORLD));
        assertFalse(d.wants(Side.CLIENT, null));
    }

    @Test
    void scheduledHandlersRunEveryNthGameTick() {
        TickDispatch d = new TickDispatch();
        int[] spacing = {3};
        List<Integer> ran = new ArrayList<>();
        int[] tick = {0};
        IScheduledTickHandler h = new IScheduledTickHandler() {
            @Override
            public int nextTickSpacing() {
                return spacing[0];
            }

            @Override
            public void tickStart(EnumSet<TickType> type, Object... tickData) {
                ran.add(tick[0]);
            }

            @Override
            public void tickEnd(EnumSet<TickType> type, Object... tickData) {
            }

            @Override
            public EnumSet<TickType> ticks() {
                return EnumSet.of(TickType.SERVER);
            }

            @Override
            public String getLabel() {
                return "scheduled";
            }
        };
        d.add(h, Side.SERVER);
        assertTrue(d.wants(Side.SERVER, null));
        for (tick[0] = 1; tick[0] <= 9; tick[0]++) {
            d.beginGameTick(Side.SERVER);
            d.fire(Side.SERVER, TickType.SERVER, false);
        }
        assertEquals(List.of(1, 4, 7), ran);
    }

    @Test
    void aFailingHandlerDoesNotStopTheOthers() {
        TickDispatch d = new TickDispatch();
        Recorder after = new Recorder(TickType.SERVER);
        d.add(new Recorder(TickType.SERVER) {
            @Override
            public void tickStart(EnumSet<TickType> type, Object... tickData) {
                throw new IllegalStateException("boom");
            }
        }, Side.SERVER);
        d.add(after, Side.SERVER);
        d.fire(Side.SERVER, TickType.SERVER, false);
        assertEquals(List.of("start [SERVER]"), after.calls);
    }

    @Test
    void partnerTicks() {
        assertEquals(EnumSet.of(TickType.WORLDLOAD), TickType.WORLD.partnerTicks());
        assertTrue(TickType.SERVER.partnerTicks().isEmpty());
    }
}
