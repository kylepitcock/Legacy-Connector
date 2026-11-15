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

    private static final Class<?> LIVING = legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase.class;
    private static final Class<?> BLOCK_STATE = legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState.class;

    /** BlockFalling.canFallThrough, last thing: the hook answers the value. Values: the value, the state. */
    public static final Site FALL_THROUGH = CoremodSites.site("block falls through a state", new Class<?>[] {boolean.class, BLOCK_STATE},
            "net/minecraft/block/BlockFalling.func_185759_i(Lnet/minecraft/block/state/IBlockState;)Z#tail").returns(boolean.class);

    /** EntityLivingBase.updatePotionEffects, before the effect particles: a hook answering false ends the method. Values: the entity. */
    public static final Site POTION_PARTICLES = CoremodSites.site("potion effect particles", new Class<?>[] {LIVING},
            "net/minecraft/entity/EntityLivingBase.func_70679_bo()V#after:aload 0").returns(boolean.class);

    /** RenderLivingBase.canRenderName, first thing: a hook answering false hides the name tag (client). Values: the entity. */
    public static final Site NAME_SHOWN = CoremodSites.site("name tag shown", new Class<?>[] {LIVING},
            "net/minecraft/client/renderer/entity/RenderLivingBase.func_177070_b(Lnet/minecraft/entity/EntityLivingBase;)Z#head").returns(boolean.class);

    // ---- third round: values that need converting, paired hooks, places in logic the shim owns, hooks that post their own events

    private static final Class<?> PLAYER = legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer.class;
    private static final Class<?> ITEM_STACK = legacyconnector.v1_12_2.net.minecraft.item.ItemStack.class;
    private static final Class<?> FOOD_STATS = legacyconnector.v1_12_2.net.minecraft.util.FoodStats.class;

    /**
     * The shift-click of the crafting table and the trading screen merges into a slot range (start, end); the hooks of these places get both numbers and
     * answer the new start or the new end. Each merge call of the method is one place, numbered by its position in the method ({@code =} keys: that
     * occurrence only).
     */
    private static final String WORKBENCH = "=net/minecraft/inventory/ContainerWorkbench.func_82846_b(Lnet/minecraft/entity/player/EntityPlayer;I)Lnet/minecraft/item/ItemStack;#after:";
    private static final String MERCHANT = "=net/minecraft/inventory/ContainerMerchant.func_82846_b(Lnet/minecraft/entity/player/EntityPlayer;I)Lnet/minecraft/item/ItemStack;#after:";
    private static final Class<?>[] RANGE = {int.class, int.class};

    public static final Site WORKBENCH_MERGE_START = CoremodSites.site("crafting table shift-click range (start)", RANGE,
            WORKBENCH + "iconst_1", WORKBENCH + "iconst_0", WORKBENCH + "iconst_0@3", WORKBENCH + "iconst_0@5").plain().returns(int.class);
    public static final Site WORKBENCH_MERGE_END = CoremodSites.site("crafting table shift-click range (end)", RANGE,
            WORKBENCH + "iconst_1@2", WORKBENCH + "iconst_0@2", WORKBENCH + "iconst_0@4", WORKBENCH + "iconst_0@6").plain().returns(int.class);
    public static final Site MERCHANT_MERGE_START = CoremodSites.site("trading screen shift-click range (start)", RANGE,
            MERCHANT + "iconst_1", MERCHANT + "iconst_0", MERCHANT + "iconst_0@3", MERCHANT + "iconst_0@5").plain().returns(int.class);
    public static final Site MERCHANT_MERGE_END = CoremodSites.site("trading screen shift-click range (end)", RANGE,
            MERCHANT + "iconst_1@2", MERCHANT + "iconst_0@2", MERCHANT + "iconst_0@4", MERCHANT + "iconst_0@6").plain().returns(int.class);

    /** Entity.move, first thing (a hook notes the motion before the move). */
    public static final Site MOVE_START = CoremodSites.site("entity move (start)", new Class<?>[] {ENTITY},
            "net/minecraft/entity/Entity.func_70091_d(Lnet/minecraft/entity/MoverType;DDD)V#after:aload 0");
    /** Entity.move, after the block collisions. Values: the entity, the movement that happened (x, y, z), the movement that was asked for (x, y, z). */
    public static final Site MOVE_DONE = CoremodSites.site("entity move (done)",
            new Class<?>[] {ENTITY, double.class, double.class, double.class, double.class, double.class, double.class},
            "net/minecraft/entity/Entity.func_70091_d(Lnet/minecraft/entity/MoverType;DDD)V#after:invokevirtual net/minecraft/entity/Entity.func_145775_I()V");

    /** FoodStats: the places of a mod that replaces hunger. Values: the food stats (and the player or the amount). */
    private static final String FOOD = "net/minecraft/util/FoodStats.";
    public static final Site FOOD_TICK = CoremodSites.site("hunger tick", new Class<?>[] {FOOD_STATS, PLAYER},
            FOOD + "func_75118_a(Lnet/minecraft/entity/player/EntityPlayer;)V#head-cancel").returns(boolean.class);
    public static final Site FOOD_NEEDED = CoremodSites.site("food needed", new Class<?>[] {FOOD_STATS}, FOOD + "func_75121_c()Z#const:20").returns(boolean.class);
    public static final Site FOOD_MAX = CoremodSites.site("maximum hunger", new Class<?>[] {FOOD_STATS}, FOOD + "func_75122_a(IF)V#const:20").returns(int.class);
    public static final Site EXHAUSTION_ADDED = CoremodSites.site("exhaustion added", new Class<?>[] {FOOD_STATS, float.class},
            FOOD + "func_75113_a(F)V#after:aload 0").returns(float.class);
    public static final Site EXHAUSTION_CAP = CoremodSites.site("exhaustion cap", new Class<?>[] {FOOD_STATS}, FOOD + "func_75113_a(F)V#const:40.0").returns(float.class);

    /** The exhaustion of the player's actions: the hook gets the action as a constant of its own enum (the place names it) and answers the amount. Values: player, amount. */
    private static final String PLAYER_CLASS = "net/minecraft/entity/player/EntityPlayer.";
    private static final String HARVEST_ARGS = "func_180657_a(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/item/ItemStack;)V#const:0.005";
    private static final Class<?>[] EXHAUSTION = {PLAYER, float.class};
    public static final Site EXHAUSTION_JUMP = CoremodSites.site("exhaustion of a jump", EXHAUSTION, PLAYER_CLASS + "func_70664_aZ()V#const:0.05")
            .named("NORMAL_JUMP").returns(float.class);
    public static final Site EXHAUSTION_SPRINT_JUMP = CoremodSites.site("exhaustion of a sprinting jump", EXHAUSTION, PLAYER_CLASS + "func_70664_aZ()V#const:0.2")
            .named("SPRINTING_JUMP").returns(float.class);
    public static final Site EXHAUSTION_ATTACK = CoremodSites.site("exhaustion of an attack", EXHAUSTION, PLAYER_CLASS + "func_71059_n(Lnet/minecraft/entity/Entity;)V#const:0.1")
            .named("ATTACK_ENTITY").returns(float.class);
    public static final Site EXHAUSTION_DAMAGE = CoremodSites.site("exhaustion of damage taken", EXHAUSTION,
            PLAYER_CLASS + "func_70665_d(Lnet/minecraft/util/DamageSource;F)V#after:invokevirtual net/minecraft/util/DamageSource.func_76345_d()F")
            .named("DAMAGE_TAKEN").returns(float.class);
    public static final Site EXHAUSTION_HARVEST = CoremodSites.site("exhaustion of breaking a block", EXHAUSTION, "net/minecraft/block/Block." + HARVEST_ARGS,
            "net/minecraft/block/BlockContainer." + HARVEST_ARGS, "net/minecraft/block/BlockIce." + HARVEST_ARGS).named("HARVEST_BLOCK").returns(float.class);
    public static final Site EXHAUSTION_HUNGER_EFFECT = CoremodSites.site("exhaustion of the hunger effect", EXHAUSTION,
            "net/minecraft/potion/Potion.func_76394_a(Lnet/minecraft/entity/EntityLivingBase;I)V#after:fmul").named("HUNGER_POTION").returns(float.class);

    /** What a shulker box slot or hopper may take: a hook that answers instead of the "no other shulker box" check. Values: the stack. */
    public static final Site SHULKER_INSERT = CoremodSites.site("item put into a shulker box", new Class<?>[] {ITEM_STACK},
            "net/minecraft/inventory/SlotShulkerBox.func_75214_a(Lnet/minecraft/item/ItemStack;)Z#replace:func_77973_b()Lnet/minecraft/item/Item;",
            "net/minecraft/tileentity/TileEntityShulkerBox.func_180462_a(ILnet/minecraft/item/ItemStack;Lnet/minecraft/util/EnumFacing;)Z#replace:func_77973_b()Lnet/minecraft/item/Item;")
            .returns(boolean.class);

    /** TileEntityFurnace.isItemValidForSlot: whether the stack may go into the input slot (false keeps it out). Values: the stack. */
    public static final Site FURNACE_INPUT = CoremodSites.site("furnace input", new Class<?>[] {ITEM_STACK},
            "net/minecraft/tileentity/TileEntityFurnace.func_94041_b(ILnet/minecraft/item/ItemStack;)Z#after:iload 1").returns(boolean.class);

    /** ItemChorusFruit.onItemUseFinish, before the teleport: a hook answering true took the teleport over. Values: the world, the eater. */
    public static final Site CHORUS_EATEN = CoremodSites.site("chorus fruit eaten", new Class<?>[] {WORLD, LIVING},
            "net/minecraft/item/ItemChorusFruit.func_77654_b(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;)Lnet/minecraft/item/ItemStack;#after:aload 3")
            .returns(boolean.class);

    /** The slot range of one merge call after the hooks of {@code start} and {@code end} had their say. */
    public static int[] mergeRange(Site start, Site end, int from, int to) {
        if (!start.listed() && !end.listed()) return new int[] {from, to};
        int a = start.active() ? ((Number) start.modify(0, from, to)).intValue() : from;
        int b = end.active() ? ((Number) end.modify(1, from, to)).intValue() : to;
        return new int[] {a, b};
    }

    /** {@link #MOVE_START}. */
    public static void moveStart(net.minecraft.world.entity.Entity entity) {
        if (MOVE_START.active()) MOVE_START.fire(EntityBridge1122.wrap(entity));
    }

    /** {@link #MOVE_DONE}. */
    public static void moveDone(net.minecraft.world.entity.Entity entity, double dx, double dy, double dz, double ax, double ay, double az) {
        if (MOVE_DONE.active()) MOVE_DONE.fire(EntityBridge1122.wrap(entity), dx, dy, dz, ax, ay, az);
    }

    private static legacyconnector.v1_12_2.net.minecraft.util.FoodStats food(net.minecraft.world.food.FoodData data) {
        return legacyconnector.v1_12_2.net.minecraft.util.FoodStats.mcc$of(data);
    }

    /** {@link #FOOD_TICK}: whether a hook took the hunger tick over. */
    public static boolean foodTick(net.minecraft.world.food.FoodData data, net.minecraft.world.entity.player.Player player) {
        return FOOD_TICK.active() && FOOD_TICK.cancels(food(data), EntityBridge1122.wrap(player));
    }

    /** {@link #FOOD_NEEDED}. */
    public static boolean foodNeeded(net.minecraft.world.food.FoodData data, boolean value) {
        return FOOD_NEEDED.active() ? Boolean.TRUE.equals(FOOD_NEEDED.replace(value, food(data))) : value;
    }

    /** {@link #FOOD_MAX}. */
    public static int foodMax(net.minecraft.world.food.FoodData data, int value) {
        return FOOD_MAX.active() ? ((Number) FOOD_MAX.replace(value, food(data))).intValue() : value;
    }

    /** {@link #EXHAUSTION_ADDED}. */
    public static float exhaustionAdded(net.minecraft.world.food.FoodData data, float value) {
        return EXHAUSTION_ADDED.active() ? ((Number) EXHAUSTION_ADDED.modify(1, food(data), value)).floatValue() : value;
    }

    /** {@link #EXHAUSTION_CAP}. */
    public static float exhaustionCap(net.minecraft.world.food.FoodData data, float value) {
        return EXHAUSTION_CAP.active() ? ((Number) EXHAUSTION_CAP.replace(value, food(data))).floatValue() : value;
    }

    /** The exhaustion of an action of the player after the hooks of its place (one of the {@code EXHAUSTION_*} sites). */
    public static float exhaustion(Site site, net.minecraft.world.entity.player.Player player, float amount) {
        if (!site.active()) return amount;
        return ((Number) site.modify(1, EntityBridge1122.wrap(player), amount)).floatValue();
    }

    /** {@link #SHULKER_INSERT}: the answer of the hook, or {@code value} when there is none. */
    public static boolean shulkerAllows(net.minecraft.world.item.ItemStack stack, boolean value) {
        if (!SHULKER_INSERT.listed() || !SHULKER_INSERT.active()) return value;
        return SHULKER_INSERT.replace(value, Stacks1122.view(stack)) instanceof Boolean b ? b : value;
    }

    /** {@link #FURNACE_INPUT} for a modern stack. */
    public static boolean furnaceInput(net.minecraft.world.item.ItemStack stack, boolean value) {
        return value && FURNACE_INPUT.listed() && FURNACE_INPUT.active() ? furnaceInput(Stacks1122.view(stack), true) : value;
    }

    /** {@link #FURNACE_INPUT}: false when a hook keeps the stack out. */
    public static boolean furnaceInput(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack, boolean value) {
        if (!value || !FURNACE_INPUT.listed() || !FURNACE_INPUT.active()) return value;
        return !Boolean.FALSE.equals(FURNACE_INPUT.replace(Boolean.TRUE, stack));
    }

    /** {@link #CHORUS_EATEN}: whether a hook took the teleport over. */
    public static boolean chorusEaten(net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity entity) {
        return CHORUS_EATEN.active() && EntityBridge1122.wrap(entity) instanceof legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase living
                && CHORUS_EATEN.cancels(WorldBridge1122.wrap(level), living);
    }

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

    /** {@link #FALL_THROUGH}. */
    public static boolean fallThrough(net.minecraft.world.level.block.state.BlockState state, boolean value) {
        if (!FALL_THROUGH.listed() || !FALL_THROUGH.active()) return value;
        return (boolean) FALL_THROUGH.modify(0, value, StatesBridge1122.toLegacy(state));
    }

    /** {@link #POTION_PARTICLES}: false when a hook says the particles are not drawn. */
    public static boolean potionParticles(net.minecraft.world.entity.LivingEntity entity) {
        return answers(POTION_PARTICLES, entity);
    }

    /** The first hook of a boolean place that asks about an entity: false when it says no, true when it says yes or there is none. */
    public static boolean answers(Site site, net.minecraft.world.entity.LivingEntity entity) {
        return !site.listed() || !site.active() || !Boolean.FALSE.equals(site.replace(Boolean.TRUE, EntityBridge1122.wrap(entity)));
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
