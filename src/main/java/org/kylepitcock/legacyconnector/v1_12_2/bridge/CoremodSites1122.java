package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites.Site;

/**
 * The places of Minecraft 1.12.2's code where coremods put hooks, on the modern game: the 1.12.2 half of {@link CoremodSites} (same lists, same binding by
 * parameter type; only the offered values are 1.12.2 views). Nothing here knows a mod.
 */
public final class CoremodSites1122 {
    private static final Class<?> ENTITY = legacyconnector.v1_12_2.net.minecraft.entity.Entity.class;
    private static final Class<?> WORLD_SERVER = legacyconnector.v1_12_2.net.minecraft.world.WorldServer.class;
    private static final Class<?> TOOL_MATERIAL = legacyconnector.v1_12_2.net.minecraft.item.Item.ToolMaterial.class;
    private static final String MATERIAL = "net/minecraft/item/Item$ToolMaterial.";

    /** Entity.onEntityUpdate, first thing. Values: the entity. */
    public static final Site ENTITY_TICK = CoremodSites.site("entity base tick", new Class<?>[] {ENTITY},
            "net/minecraft/entity/Entity.func_70030_z()V#after:aload 0", "net/minecraft/entity/Entity.func_70030_z()V#head");

    /** WorldServer.wakeAllPlayers, first thing. Values: the world. */
    public static final Site PLAYERS_WAKE = CoremodSites.site("all players wake up", new Class<?>[] {WORLD_SERVER},
            "net/minecraft/world/WorldServer.func_73053_d()V#after:aload 0", "net/minecraft/world/WorldServer.func_73053_d()V#head");

    /** The getters of Item.ToolMaterial, last thing: the hook answers the value. Values: the value, the material. */
    public static final Site MATERIAL_MAX_USES = CoremodSites.site("tool material uses", new Class<?>[] {int.class, TOOL_MATERIAL},
            MATERIAL + "func_77997_a()I#tail").returns(int.class);
    public static final Site MATERIAL_HARVEST_LEVEL = CoremodSites.site("tool material harvest level", new Class<?>[] {int.class, TOOL_MATERIAL},
            MATERIAL + "func_77996_d()I#tail").returns(int.class);
    public static final Site MATERIAL_ENCHANTABILITY = CoremodSites.site("tool material enchantability", new Class<?>[] {int.class, TOOL_MATERIAL},
            MATERIAL + "func_77995_e()I#tail").returns(int.class);
    public static final Site MATERIAL_EFFICIENCY = CoremodSites.site("tool material efficiency", new Class<?>[] {float.class, TOOL_MATERIAL},
            MATERIAL + "func_77998_b()F#tail").returns(float.class);
    public static final Site MATERIAL_ATTACK_DAMAGE = CoremodSites.site("tool material attack damage", new Class<?>[] {float.class, TOOL_MATERIAL},
            MATERIAL + "func_78000_c()F#tail").returns(float.class);

    private static final Class<?> WORLD = legacyconnector.v1_12_2.net.minecraft.world.World.class;
    private static final Class<?> BLOCK_POS = legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.class;
    private static final Class<?> ENTITY_ITEM = legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem.class;

    /** World.isRainingAt, last thing: the hook answers the value. Values: the value, the world, the position. */
    public static final Site RAINING_AT = CoremodSites.site("rain at a position", new Class<?>[] {boolean.class, WORLD, BLOCK_POS},
            "net/minecraft/world/World.func_175727_C(Lnet/minecraft/util/math/BlockPos;)Z#tail").returns(boolean.class);

    /** World.canSnowAt, last thing: the hook answers the value. Values: the value, the world, the position. */
    public static final Site SNOW_AT = CoremodSites.site("snow at a position", new Class<?>[] {boolean.class, WORLD, BLOCK_POS},
            "net/minecraft/world/World.func_175708_f(Lnet/minecraft/util/math/BlockPos;Z)Z#tail").returns(boolean.class);

    /** EntityItem.onUpdate, first thing. Values: the item entity. */
    public static final Site ITEM_ENTITY_TICK = CoremodSites.site("item entity tick", new Class<?>[] {ENTITY_ITEM},
            "net/minecraft/entity/item/EntityItem.func_70071_h_()V#after:aload 0", "net/minecraft/entity/item/EntityItem.func_70071_h_()V#head");

    private static final ThreadLocal<Boolean> IN_WORLD_HOOK = new ThreadLocal<>();

    private CoremodSites1122() {}

    /** {@link #RAINING_AT} / {@link #SNOW_AT} for the shim world {@code world}; hooks that ask the world again get the unhooked answer. */
    public static boolean worldAnswer(Site site, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, boolean value) {
        if (!site.listed() || !site.active() || IN_WORLD_HOOK.get() != null || world == null) return value;
        IN_WORLD_HOOK.set(Boolean.TRUE);
        try {
            return (boolean) site.modify(0, value, world, pos);
        } finally {
            IN_WORLD_HOOK.remove();
        }
    }

    public static boolean raining(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, boolean value) {
        if (!RAINING_AT.listed() || !RAINING_AT.active() || IN_WORLD_HOOK.get() != null) return value;
        return worldAnswer(RAINING_AT, WorldBridge1122.wrap(level), legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.mcc$of(pos), value);
    }

    public static int material(Site site, Object material, int value) {
        return site.listed() && site.active() ? (int) site.modify(0, value, material) : value;
    }

    public static float material(Site site, Object material, float value) {
        return site.listed() && site.active() ? (float) site.modify(0, value, material) : value;
    }

    public static void playersWake(net.minecraft.server.level.ServerLevel level) {
        if (PLAYERS_WAKE.active() && WorldBridge1122.wrap(level) instanceof legacyconnector.v1_12_2.net.minecraft.world.WorldServer world) PLAYERS_WAKE.fire(world);
    }

    /** Wiring of the places that are an event of the modern game. */
    public static void register(net.neoforged.bus.api.IEventBus gameBus) {
        if (!CoremodSites.any()) return;
        CoremodSites.reportOnStart(gameBus);
        if (ENTITY_TICK.listed()) gameBus.addListener((net.neoforged.neoforge.event.tick.EntityTickEvent.Pre e) -> {
            if (ENTITY_TICK.active()) ENTITY_TICK.fire(EntityBridge1122.wrap(e.getEntity()));
        });
        if (ITEM_ENTITY_TICK.listed()) gameBus.addListener((net.neoforged.neoforge.event.tick.EntityTickEvent.Pre e) -> {
            if (e.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity && ITEM_ENTITY_TICK.active()
                    && EntityBridge1122.wrap(e.getEntity()) instanceof legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem item) ITEM_ENTITY_TICK.fire(item);
        });
    }
}
