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

    // ---- fourth round: hunger movement, cake, wither, vehicle drops, mob targets, sleeping, liquids, furnace results, banner layers

    private static final Class<?> BLOCK = legacyconnector.v1_12_2.net.minecraft.block.Block.class;
    private static final Class<?> ENTITY_LIVING = legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving.class;

    /**
     * EntityPlayer.addMovementStat: the exhaustion of moving, one place per call in the order of the method (diving, swimming, sprinting, crouching, walking);
     * as with the other actions the hook is told which one by a constant of its own enum, and answers the amount. Values: player, amount.
     */
    private static final String MOVEMENT = "=net/minecraft/entity/player/EntityPlayer.func_71000_j(DDD)V#after:fmul";
    public static final Site EXHAUSTION_DIVE = CoremodSites.site("exhaustion of diving", EXHAUSTION, MOVEMENT).named("MOVEMENT_DIVE").returns(float.class);
    public static final Site EXHAUSTION_SWIM = CoremodSites.site("exhaustion of swimming", EXHAUSTION, MOVEMENT + "@2").named("MOVEMENT_SWIM").returns(float.class);
    public static final Site EXHAUSTION_SPRINT = CoremodSites.site("exhaustion of sprinting", EXHAUSTION, MOVEMENT + "@3").named("MOVEMENT_SPRINT").returns(float.class);
    public static final Site EXHAUSTION_CROUCH = CoremodSites.site("exhaustion of crouching", EXHAUSTION, MOVEMENT + "@4").named("MOVEMENT_CROUCH").returns(float.class);
    public static final Site EXHAUSTION_WALK = CoremodSites.site("exhaustion of walking", EXHAUSTION, MOVEMENT + "@5").named("MOVEMENT_WALK").returns(float.class);

    /** EntityLivingBase.getItemInUseMaxCount: stands around the use duration of the stack in use. Values: the entity, the duration. */
    public static final Site ITEM_USE_DURATION = CoremodSites.site("duration of the item in use", new Class<?>[] {LIVING, int.class},
            "net/minecraft/entity/EntityLivingBase.func_184612_cw()I#replace:func_77988_m()I").returns(int.class);

    /** BlockCake.eatCake: a hook that stands in for the player's hunger gain of the bite (it adds the hunger itself). Values: the block, the world, the player. */
    public static final Site CAKE_EATEN = CoremodSites.site("cake bite eaten", new Class<?>[] {BLOCK, WORLD, PLAYER},
            "net/minecraft/block/BlockCake.func_180682_b(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;"
                    + "Lnet/minecraft/entity/player/EntityPlayer;)Z#replace:func_71024_bL()Lnet/minecraft/util/FoodStats;");

    /** EntityWither.canDestroyBlock, first thing: the hook answers the block the check goes on with. Values: the block. */
    public static final Site WITHER_BLOCK = CoremodSites.site("block the wither destroys", new Class<?>[] {BLOCK},
            "net/minecraft/entity/boss/EntityWither.func_181033_a(Lnet/minecraft/block/Block;)Z#after:aload 0").returns(BLOCK);

    /** The drops of a destroyed boat and minecart (last thing after the item dropped). Values: the vehicle. */
    public static final Site BOAT_DROPS = CoremodSites.site("boat destroyed", new Class<?>[] {ENTITY},
            "net/minecraft/entity/item/EntityBoat.func_70097_a(Lnet/minecraft/util/DamageSource;F)Z#after:invokevirtual net/minecraft/entity/item/EntityBoat.func_145778_a("
                    + "Lnet/minecraft/item/Item;IF)Lnet/minecraft/entity/item/EntityItem;");
    public static final Site MINECART_DROPS = CoremodSites.site("minecart destroyed", new Class<?>[] {ENTITY},
            "net/minecraft/entity/item/EntityMinecart.func_94095_a(Lnet/minecraft/util/DamageSource;)V#after:invokevirtual net/minecraft/entity/item/EntityMinecart.func_70099_a("
                    + "Lnet/minecraft/item/ItemStack;F)Lnet/minecraft/entity/item/EntityItem;");

    /** EntityAITarget.isSuitableTarget, first thing: a hook answering true makes the target unsuitable. Values: the mob, the target. */
    public static final Site TARGET_HIDDEN = CoremodSites.site("mob target hidden", new Class<?>[] {ENTITY_LIVING, LIVING},
            "net/minecraft/entity/ai/EntityAITarget.func_179445_a(Lnet/minecraft/entity/EntityLiving;Lnet/minecraft/entity/EntityLivingBase;ZZ)Z#after:aload 1")
            .returns(boolean.class);

    /** WorldServer.areAllPlayersAsleep, first thing: 0 leaves the answer to the game, 1 answers no, 2 answers yes. Values: the world. */
    public static final Site ALL_ASLEEP = CoremodSites.site("all players asleep", new Class<?>[] {WORLD},
            "net/minecraft/world/WorldServer.func_73056_e()Z#after:aload 0").returns(int.class);

    /** BlockDynamicLiquid.isBlocked: a hook answering true says flowing liquid does not replace the block. Values: the block. */
    public static final Site LIQUID_BLOCKED = CoremodSites.site("block liquids do not replace", new Class<?>[] {BLOCK},
            "net/minecraft/block/BlockDynamicLiquid.func_176372_g(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)Z#after:iconst_1",
            "net/minecraft/block/BlockDynamicLiquid.func_176372_g(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)Z#after:iconst_1@2",
            "net/minecraft/block/BlockDynamicLiquid.func_176372_g(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)Z"
                    + "#after:invokevirtual net/minecraft/block/material/Material.func_76230_c()Z").returns(boolean.class);

    /** TileEntityFurnace.smeltItem, after the recipe's result was looked up: the hook answers the result. Values: the input stack, the result. */
    public static final Site SMELT_RESULT = CoremodSites.site("furnace result", new Class<?>[] {ITEM_STACK, ITEM_STACK},
            "net/minecraft/tileentity/TileEntityFurnace.func_145949_j()V#after:astore 2").returns(ITEM_STACK);

    /** RecipesBanners.RecipeAddPattern.matches: the number of layers as the 6 layer limit sees it. Values: the layer count. */
    public static final Site BANNER_LAYERS = CoremodSites.site("banner layers against the limit", new Class<?>[] {int.class},
            "net/minecraft/item/crafting/RecipesBanners$RecipeAddPattern.func_77569_a(Lnet/minecraft/inventory/InventoryCrafting;Lnet/minecraft/world/World;)Z"
                    + "#after:invokestatic net/minecraft/tileentity/TileEntityBanner.func_175113_c(Lnet/minecraft/item/ItemStack;)I").returns(int.class);

    private static final Class<?> ENUM_FACING = legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.class;
    private static final Class<?> CHUNK = legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk.class;
    private static final Class<?> INVENTORY_PLAYER = legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer.class;
    private static final Class<?> BOX = legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB.class;

    /**
     * World.getStrongPower / getRedstonePower, first thing: a hook answering a number other than 0 is the answer of the method. Values: the world, the
     * position, the side.
     */
    public static final Site STRONG_POWER = CoremodSites.site("redstone strong power", new Class<?>[] {WORLD, BLOCK_POS, ENUM_FACING},
            "net/minecraft/world/World.func_175627_a(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)I#after:aload 1").returns(int.class);
    public static final Site REDSTONE_POWER = CoremodSites.site("redstone power", new Class<?>[] {WORLD, BLOCK_POS, ENUM_FACING},
            "net/minecraft/world/World.func_175651_c(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)I#after:aload 1").returns(int.class);

    /** EntitySlime.getCanSpawnHere, after the chunk was read: a hook answering 0 or 1 is the answer of the method, -1 leaves it to the game. Values: the world, the chunk. */
    public static final Site SLIME_CHUNK = CoremodSites.site("slime chunk override", new Class<?>[] {WORLD, CHUNK},
            "net/minecraft/entity/monster/EntitySlime.func_70601_bi()Z#after:getfield net/minecraft/entity/monster/EntitySlime.field_70170_p Lnet/minecraft/world/World;")
            .returns(int.class);

    /** EntityLivingBase.travel, after the ground's slipperiness was multiplied by 0.91: the hook answers the value. Values: the value, the entity, the block below. */
    public static final Site SLIPPERINESS = CoremodSites.site("ground slipperiness", new Class<?>[] {float.class, LIVING, BLOCK},
            "net/minecraft/entity/EntityLivingBase.func_191986_a(FFF)V#after:fmul").returns(float.class);

    /** InventoryPlayer.dropAllItems, after each non-empty check: a hook answering false keeps the stack. Values: the inventory, the slot, the stack. */
    public static final Site PLAYER_DROPS = CoremodSites.site("stack dropped from a dead player", new Class<?>[] {INVENTORY_PLAYER, int.class, ITEM_STACK},
            "net/minecraft/entity/player/InventoryPlayer.func_70436_m()V#after:aload 0").returns(boolean.class);

    /** WorldGenAbstractTree.setDirtAt, after the dirt check: a hook answering true keeps the ground. Values: the block of the ground. */
    public static final Site TREE_GROUND = CoremodSites.site("ground a tree keeps", new Class<?>[] {BLOCK},
            "net/minecraft/world/gen/feature/WorldGenAbstractTree.func_175921_a(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V#after:if_acmpeq")
            .returns(boolean.class);

    /**
     * Block.addCollisionBoxToList, first thing: a hook answering true has added the boxes of the block itself (nothing means no collision). Values: the state,
     * the world, the position, the box asked about (the block's cell), the list, the entity.
     */
    public static final Site COLLISION_BOXES = CoremodSites.site("block collision boxes", new Class<?>[] {BLOCK_STATE, WORLD, BLOCK_POS, BOX, java.util.List.class, ENTITY},
            "net/minecraft/block/Block.func_185477_a(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;"
                    + "Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/List;Lnet/minecraft/entity/Entity;Z)V#cancel-after:aload 5").returns(boolean.class);

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

    /** {@link #ITEM_USE_DURATION}. */
    public static int itemUseDuration(net.minecraft.world.entity.LivingEntity entity, int duration) {
        return ITEM_USE_DURATION.active() ? ((Number) ITEM_USE_DURATION.modify(1, EntityBridge1122.wrap(entity), duration)).intValue() : duration;
    }

    /** {@link #CAKE_EATEN}: whether a hook ate the bite (the game's own hunger gain is then left out). */
    public static boolean cakeEaten(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.entity.player.Player player) {
        if (!CAKE_EATEN.active()) return false;
        return CAKE_EATEN.stands(StatesBridge1122.legacyBlock(state), WorldBridge1122.wrap(level), EntityBridge1122.wrap(player));
    }

    /** {@link #WITHER_BLOCK}: the state the wither's check goes on with ({@code state} when no hook changed the block). */
    public static net.minecraft.world.level.block.state.BlockState witherBlock(net.minecraft.world.level.block.state.BlockState state) {
        if (!WITHER_BLOCK.active()) return state;
        legacyconnector.v1_12_2.net.minecraft.block.Block block = StatesBridge1122.legacyBlock(state);
        if (block == null || !(WITHER_BLOCK.modify(0, block) instanceof legacyconnector.v1_12_2.net.minecraft.block.Block changed) || changed == block) return state;
        return StatesBridge1122.toModern(changed.getDefaultState());
    }

    /** {@link #BOAT_DROPS} and {@link #MINECART_DROPS}. */
    public static void vehicleDestroyed(net.minecraft.world.entity.Entity vehicle) {
        Site site = vehicle instanceof net.minecraft.world.entity.vehicle.Boat ? BOAT_DROPS
                : vehicle instanceof net.minecraft.world.entity.vehicle.AbstractMinecart ? MINECART_DROPS : null;
        if (site != null && site.active()) site.fire(EntityBridge1122.wrap(vehicle));
    }

    /** {@link #TARGET_HIDDEN}. */
    public static boolean targetHidden(net.minecraft.world.entity.Mob mob, net.minecraft.world.entity.LivingEntity target) {
        return TARGET_HIDDEN.active() && TARGET_HIDDEN.cancels(EntityBridge1122.wrap(mob), EntityBridge1122.wrap(target));
    }

    /** {@link #ALL_ASLEEP}: 0 when no hook answers, 1 for no, 2 for yes. */
    public static int allAsleep(net.minecraft.server.level.ServerLevel level) {
        return ALL_ASLEEP.active() ? ((Number) ALL_ASLEEP.replace(0, WorldBridge1122.wrap(level))).intValue() : 0;
    }

    /** {@link #LIQUID_BLOCKED}. */
    public static boolean liquidBlocked(net.minecraft.world.level.block.state.BlockState state) {
        if (!LIQUID_BLOCKED.active()) return false;
        legacyconnector.v1_12_2.net.minecraft.block.Block block = StatesBridge1122.legacyBlock(state);
        return block != null && LIQUID_BLOCKED.cancels(block);
    }

    /** {@link #SMELT_RESULT}: the stack a furnace makes after the hooks changed it. */
    public static net.minecraft.world.item.ItemStack smeltResult(net.minecraft.world.item.ItemStack input, net.minecraft.world.item.ItemStack result) {
        if (!SMELT_RESULT.active()) return result;
        Object changed = SMELT_RESULT.modify(1, Stacks1122.fromModern(input.copy()), Stacks1122.fromModern(result.copy()));
        return changed instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack ? Stacks1122.toModern(stack) : result;
    }

    /** {@link #BANNER_LAYERS}. */
    public static int bannerLayers(int layers) {
        return BANNER_LAYERS.active() ? ((Number) BANNER_LAYERS.modify(0, layers)).intValue() : layers;
    }

    /** {@link #STRONG_POWER} / {@link #REDSTONE_POWER}: what a hook says, 0 when none does. */
    public static int power(Site site, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
        if (!site.listed() || !site.active()) return 0;
        Object answer = site.replace(0, WorldBridge1122.wrap(level), legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.mcc$of(pos),
                legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.mcc$of(side));
        return answer instanceof Number n ? n.intValue() : 0;
    }

    /** {@link #SLIME_CHUNK}: 0 or 1 when a hook answers, -1 when the game decides. */
    public static int slimeChunk(net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos) {
        if (!SLIME_CHUNK.active()) return -1;
        legacyconnector.v1_12_2.net.minecraft.world.World world = WorldBridge1122.wrap(level);
        Object answer = SLIME_CHUNK.replace(-1, world, world.getChunk(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.mcc$of(pos)));
        return answer instanceof Number n ? n.intValue() : -1;
    }

    /** {@link #SLIPPERINESS}: the slipperiness of the ground (the game's value without the 0.91) after the hooks. */
    public static float slipperiness(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.level.block.state.BlockState ground, float slipperiness) {
        if (!SLIPPERINESS.active()) return slipperiness;
        legacyconnector.v1_12_2.net.minecraft.block.Block block = StatesBridge1122.legacyBlock(ground);
        if (block == null) return slipperiness;
        float changed = ((Number) SLIPPERINESS.modify(0, slipperiness * 0.91F, EntityBridge1122.wrap(entity), block)).floatValue();
        return changed / 0.91F;
    }

    /** {@link #PLAYER_DROPS}: false when a hook keeps the stack. */
    public static boolean playerDrops(net.minecraft.world.entity.player.Player player, int slot, net.minecraft.world.item.ItemStack stack) {
        if (!PLAYER_DROPS.active() || !(EntityBridge1122.wrap(player) instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer legacy)) return true;
        return !Boolean.FALSE.equals(PLAYER_DROPS.replace(Boolean.TRUE, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer.mcc$get$inventory(legacy), slot,
                Stacks1122.fromModern(stack)));
    }

    /** {@link #TREE_GROUND}: true when a hook keeps the ground under a tree. */
    public static boolean treeKeepsGround(net.minecraft.world.level.block.state.BlockState ground) {
        if (!TREE_GROUND.active()) return false;
        legacyconnector.v1_12_2.net.minecraft.block.Block block = StatesBridge1122.legacyBlock(ground);
        return block != null && Boolean.TRUE.equals(TREE_GROUND.replace(Boolean.FALSE, block));
    }

    /** {@link #COLLISION_BOXES}: the shape a hook gives the block, or null when none does. */
    public static net.minecraft.world.phys.shapes.VoxelShape collisionShape(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter getter,
            net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        if (!COLLISION_BOXES.active() || !(getter instanceof net.minecraft.world.level.Level level)) return null;
        net.minecraft.world.entity.Entity entity = context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext ec ? ec.getEntity() : null;
        legacyconnector.v1_12_2.net.minecraft.world.World world = WorldBridge1122.wrap(level);
        java.util.List<legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB> boxes = new java.util.ArrayList<>();
        legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB cell = new legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB(pos.getX(), pos.getY(),
                pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
        if (!COLLISION_BOXES.cancels(StatesBridge1122.toLegacy(state), world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.mcc$of(pos), cell, boxes,
                entity == null ? null : EntityBridge1122.wrap(entity))) return null;
        net.minecraft.world.phys.shapes.VoxelShape shape = net.minecraft.world.phys.shapes.Shapes.empty();
        for (legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB box : boxes) {
            shape = net.minecraft.world.phys.shapes.Shapes.or(shape, net.minecraft.world.phys.shapes.Shapes.create(box.minX - pos.getX(), box.minY - pos.getY(),
                    box.minZ - pos.getZ(), box.maxX - pos.getX(), box.maxY - pos.getY(), box.maxZ - pos.getZ()));
        }
        return shape;
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
