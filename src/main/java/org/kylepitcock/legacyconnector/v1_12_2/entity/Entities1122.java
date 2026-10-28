package org.kylepitcock.legacyconnector.v1_12_2.entity;

/** Game-bus registrations of the 1.12.2 entity layer (live stacks, capability saving, Forge entity events). */
public final class Entities1122 {
    private Entities1122() {}

    public static void register(net.neoforged.bus.api.IEventBus game) {
        LiveStacks1122.register(game);
        org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.registerSaving(game);
        ForgeEvents1122.register(game);
        VillagerTrades1122.register(game);
    }
}
