package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replays the hooks legacy coremods put into Minecraft 1.7.10's own classes. Those classes are gone, so the transformers cannot run; the loader instead lists
 * what they would have added ({@code CoremodHookScan}: each added static call with its <i>site</i>, the place in 1.7.10's code). A {@link Site} here is such
 * a place offered on the modern game: it names the 1.7.10 site keys it stands for and the values available there, and the code at the modern place (a mixin,
 * or the shim class itself) hands the values over. Nothing here knows a mod: any coremod that hooked a site is called, with the arguments its hook method
 * declares.
 *
 * <p>A hook that changes a value without receiving it (1.7.10 coremods then combined the two with a helper of their own next to the hook, e.g.
 * {@code ASMHelper.multiply(float, float)} or {@code and(boolean, boolean)}) has its result combined with the value through that helper; without one its
 * result replaces the value.
 *
 * <p>Arguments are bound by type: each parameter of a hook takes the first value the site offers that fits and is not taken yet (1.7.10 hooks receive the
 * receiver, the method's arguments and the value being changed in an order of the author's choosing, e.g. {@code getHealRate(EntityPlayer, int)} and
 * {@code getEntityGravity(double, Entity)}). A hook that asks for something a site does not offer is left out and reported, as are hooks at places not offered
 * yet; the report is one log line at start-up ({@code -Dlegacyconnector.coremodHooks.report=full} lists every hook).
 */
public final class CoremodSites {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Coremods");
    private static final String SHIM = "legacyconnector.v1_7_10.";
    private static final String SYNTH = "legacyconnector_synth.v1_7_10.";

    /** A discovered hook call: where, and the static method. */
    private record Listed(String site, String owner, String name, String desc) {}

    /** The same, keyed with the {@code @n} of repeats kept (the first occurrence has none): a place key written {@code =site@3} asks for that occurrence only. */
    private static final Map<String, List<Listed>> EXACT = new HashMap<>();
    /** Site key (without the {@code @n} of repeats) to the hooks there, as the loader listed them. */
    private static final Map<String, List<Listed>> LISTED = load();
    private static final List<Site> SITES = new ArrayList<>();
    private static volatile boolean ready;

    // ---- the catalog: 1.7.10 places that exist on the modern game. Values are legacy views (EntityBridge.wrap).
    private static final Class<?> ENTITY = legacyconnector.v1_7_10.net.minecraft.entity.Entity.class;
    private static final Class<?> LIVING = legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase.class;
    private static final Class<?> PLAYER = legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer.class;
    private static final Class<?> WORLD = legacyconnector.v1_7_10.net.minecraft.world.World.class;

    /** EntityLivingBase.moveEntityWithHeading, first thing: true cancels the vanilla movement. Values: entity, strafe, forward. */
    public static final Site TRAVEL = site("living entity movement", new Class<?>[] {LIVING, float.class, float.class},
            "net/minecraft/entity/EntityLivingBase.func_70612_e(FF)V#head-cancel");

    /** The gravity constant of an entity's movement step. Values: gravity, entity. */
    public static final Site GRAVITY = site("entity gravity", new Class<?>[] {double.class, ENTITY},
            "net/minecraft/entity/EntityLivingBase.func_70612_e(FF)V#after:ldc 0.08",
            "net/minecraft/entity/item/EntityItem.func_70071_h_()V#after:ldc 0.03999999910593033",
            "net/minecraft/entity/item/EntityFallingBlock.func_70071_h_()V#after:ldc 0.03999999910593033",
            "net/minecraft/entity/item/EntityMinecart.func_70071_h_()V#after:ldc 0.03999999910593033",
            "net/minecraft/entity/item/EntityTNTPrimed.func_70071_h_()V#after:ldc 0.03999999910593033",
            "net/minecraft/entity/projectile/EntityArrow.func_70071_h_()V#after:f2d",
            "net/minecraft/entity/projectile/EntityThrowable.func_70071_h_()V#after:invokevirtual net/minecraft/entity/projectile/EntityThrowable.func_70185_h()F");

    /** FoodStats.onUpdate: the ticks between two natural heals (80). Values: player, ticks. */
    public static final Site HEAL_RATE = site("natural healing rate", new Class<?>[] {PLAYER, int.class},
            "net/minecraft/util/FoodStats.func_75118_a(Lnet/minecraft/entity/player/EntityPlayer;)V#const:80");

    /** MinecraftServer.run: the milliseconds of one tick (50). Value: milliseconds. */
    public static final Site TICK_MILLIS = site("server tick length", new Class<?>[] {long.class},
            "net/minecraft/server/MinecraftServer.run()V#const:50");

    /** World.updateWeather, first thing: true skips the weather step. Value: world. */
    public static final Site WEATHER = site("weather step", new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.world.World.class},
            "net/minecraft/world/World.func_72835_b()V#head-cancel");

    /** EntityLivingBase.getArmSwingAnimationEnd: the ticks of an arm swing. Values: ticks, entity. */
    public static final Site SWING_TICKS = site("arm swing length", new Class<?>[] {int.class, LIVING},
            "net/minecraft/entity/EntityLivingBase.func_82166_i()I#tail");

    /** Entity.onEntityUpdate: whether a sprinting entity kicks up particles. Values: sprinting, entity. */
    public static final Site SPRINT_PARTICLES = site("sprint particles", new Class<?>[] {boolean.class, ENTITY},
            "net/minecraft/entity/Entity.func_70030_z()V#after:invokevirtual net/minecraft/entity/Entity.func_70051_ag()Z");

    /** World.removeEntity, last thing. Values: world, entity. */
    public static final Site ENTITY_REMOVED = site("entity removed from the world", new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.world.World.class, ENTITY},
            "net/minecraft/world/World.func_72900_e(Lnet/minecraft/entity/Entity;)V#tail");

    /** EntityPlayer.attackTargetEntityWithCurrentItem: stands in for {@code target.attackEntityFrom(source, amount)}. Values: target, source, amount, attacker. */
    public static final Site PLAYER_ATTACK = site("player attack damage",
            new Class<?>[] {ENTITY, legacyconnector.v1_7_10.net.minecraft.util.DamageSource.class, float.class, PLAYER},
            "net/minecraft/entity/player/EntityPlayer.func_71059_n(Lnet/minecraft/entity/Entity;)V#replace:func_70097_a(Lnet/minecraft/util/DamageSource;F)Z");

    /**
     * The gravity constants themselves, where a hook stands in for the number (0.08 living, 0.04 item, 0.05 arrow): the modern gravity is scaled by
     * the hook's answer over that number, so attributes and other mods' changes stay. Value: entity.
     */
    public static final Site GRAVITY_LIVING = site("living entity gravity constant", new Class<?>[] {ENTITY},
            "net/minecraft/entity/EntityLivingBase.func_70612_e(FF)V#const:0.08");
    public static final Site GRAVITY_ITEM = site("item gravity constant", new Class<?>[] {ENTITY},
            "net/minecraft/entity/item/EntityItem.func_70071_h_()V#const:0.03999999910593033");
    public static final Site GRAVITY_ARROW = site("arrow gravity constant", new Class<?>[] {ENTITY},
            "net/minecraft/entity/projectile/EntityArrow.func_70071_h_()V#const:0.05");

    /** EntityPlayer.damageEntity: the amount before armour. Values: player, source, amount. */
    public static final Site PLAYER_DAMAGE = site("player damage amount",
            new Class<?>[] {PLAYER, legacyconnector.v1_7_10.net.minecraft.util.DamageSource.class, float.class},
            "net/minecraft/entity/player/EntityPlayer.func_70665_d(Lnet/minecraft/util/DamageSource;F)V#after:fload 2");

    /** EntityPlayer.isInBed (the check that wakes a sleeper whose bed is gone), first thing: true keeps the player asleep. Value: player. */
    public static final Site IN_BED = site("sleeping without a bed", new Class<?>[] {PLAYER},
            "net/minecraft/entity/player/EntityPlayer.func_71065_l()Z#after:aload 0");

    /** World.isBlockIndirectlyGettingPowered, last thing. Values: powered, world, x, y, z. */
    public static final Site NEIGHBOR_POWERED = site("redstone: block powered by its neighbours",
            new Class<?>[] {boolean.class, legacyconnector.v1_7_10.net.minecraft.world.World.class, int.class, int.class, int.class},
            "net/minecraft/world/World.func_72864_z(III)Z#tail").returns(boolean.class);

    /** World.getIndirectPowerLevelTo, last thing. Values: power, world, block at the position, x, y, z, direction. */
    public static final Site POWER_LEVEL = site("redstone: power level at a side",
            new Class<?>[] {int.class, legacyconnector.v1_7_10.net.minecraft.world.World.class, legacyconnector.v1_7_10.net.minecraft.block.Block.class, int.class, int.class,
                    int.class, int.class},
            "net/minecraft/world/World.func_72878_l(IIII)I#tail").returns(int.class);

    /** EntityTracker.addEntityToTracker: the range (blocks) an entity is tracked at. Values: range, entity, update frequency. */
    public static final Site TRACKING_RANGE = site("entity tracking range", new Class<?>[] {int.class, ENTITY, int.class},
            "net/minecraft/entity/EntityTracker.func_72785_a(Lnet/minecraft/entity/Entity;IIZ)V#after:iload 2").returns(int.class);

    /** Entity.moveEntity: the distance one footstep counts (1). Values: entity, step. The hook answers the new step length. */
    public static final Site STEP_DISTANCE = site("footstep distance", new Class<?>[] {ENTITY, int.class},
            "net/minecraft/entity/Entity.func_70091_d(DDD)V#const:1").returns(float.class);

    /** World.getRainStrength: a hook that stands in for the whole answer (dimensions without rain). Values: world, partial tick. */
    public static final Site RAIN_STRENGTH = site("rain strength", new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.world.World.class, float.class},
            "net/minecraft/world/World.func_72867_j(F)F#after:aload 0").returns(float.class);

    /** Block.onBlockDestroyedByExplosion, last thing (blocks that keep the base method). Values: block, world, x, y, z. */
    public static final Site BLOCK_EXPLODED = site("block destroyed by an explosion",
            new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.block.Block.class, legacyconnector.v1_7_10.net.minecraft.world.World.class, int.class, int.class, int.class},
            "net/minecraft/block/Block.func_149723_a(Lnet/minecraft/world/World;IIILnet/minecraft/world/Explosion;)V#tail");

    /** EntityXPOrb.onUpdate, last thing. Values: the orb. */
    public static final Site XP_ORB_TICK = site("experience orb tick", new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.entity.item.EntityXPOrb.class},
            "net/minecraft/entity/item/EntityXPOrb.func_70071_h_()V#tail");

    /** BlockFarmland.onFallenUpon: a hook that stands in for turning the farmland to dirt (it does that itself). Values: world, x, y, z, entity, fall distance. */
    public static final Site FARMLAND_TRAMPLE = site("farmland trampled",
            new Class<?>[] {WORLD, int.class, int.class, int.class, ENTITY, float.class},
            "net/minecraft/block/BlockFarmland.func_149746_a(Lnet/minecraft/world/World;IIILnet/minecraft/entity/Entity;F)V#replace:func_147449_b(IIILnet/minecraft/block/Block;)Z");

    /** EntityLiving.canDespawn: a hook that answers instead of the base "yes". Values: the mob. */
    public static final Site FAR_DESPAWN = site("mob despawns far from players", new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving.class},
            "net/minecraft/entity/EntityLiving.func_70692_ba()Z#const:1").returns(boolean.class);

    /** ItemStack.updateAnimation (the inventory tick of a stack), last thing. Values: stack, entity, slot, held. */
    public static final Site ITEM_TICK = site("item stack inventory tick",
            new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.item.ItemStack.class, ENTITY, int.class, boolean.class},
            "net/minecraft/item/ItemStack.func_77945_a(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;IZ)V#tail");

    /** ItemStack.tryPlaceItemIntoWorld, after a use that counted. Values: stack, player, world, x, y, z, side, hit x, hit y, hit z. */
    public static final Site ITEM_USED = site("item used on a block",
            new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.item.ItemStack.class, PLAYER, WORLD, int.class, int.class, int.class, int.class,
                    float.class, float.class, float.class},
            "net/minecraft/item/ItemStack.func_77943_a(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;IIIIFFF)Z"
                    + "#after:invokevirtual net/minecraft/entity/player/EntityPlayer.func_71064_a(Lnet/minecraft/stats/StatBase;I)V");

    /** The server's entity action packet, after it started the player's sprint. Values: player. */
    public static final Site SPRINT_STARTED = site("player started sprinting (server)", new Class<?>[] {PLAYER},
            "net/minecraft/network/NetHandlerPlayServer.func_147357_a(Lnet/minecraft/network/play/client/C0BPacketEntityAction;)V"
                    + "#after:invokevirtual net/minecraft/entity/player/EntityPlayerMP.func_70031_b(Z)V");

    /** The 40 levels above which the anvil refuses (container and screen). Values: none. */
    public static final Site ANVIL_MAX_COST = site("anvil maximum cost", new Class<?>[0],
            "net/minecraft/inventory/ContainerRepair.func_82848_d()V#const:40", "net/minecraft/client/gui/GuiRepair.func_146979_b(II)V#const:40").returns(int.class);

    /** EntityArrow.onUpdate, first thing. Values: the arrow (as an entity: a hook for arrows is only called with the legacy arrow class). */
    public static final Site ARROW_TICK = site("arrow tick", new Class<?>[] {ENTITY},
            "net/minecraft/entity/projectile/EntityArrow.func_70071_h_()V#after:aload 0", "net/minecraft/entity/projectile/EntityArrow.func_70071_h_()V#head");

    /** EntityLiving.updateAITasks, first thing: a hook that answers true runs instead of the mob's AI. Values: the mob. */
    public static final Site AI_TASKS = site("mob AI tasks", new Class<?>[] {LIVING},
            "net/minecraft/entity/EntityLiving.func_70619_bc()V#head-cancel");

    /** EnchantmentHelper.calcItemStackEnchantability, the enchantability of the item. Values: stack, enchantability. */
    public static final Site ENCHANTABILITY = site("item enchantability", new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.item.ItemStack.class, int.class},
            "net/minecraft/enchantment/EnchantmentHelper.func_77514_a(Ljava/util/Random;IILnet/minecraft/item/ItemStack;)I#after:iload 5").returns(int.class);

    /** InventoryPlayer.getCurrentItem, last thing: the hook answers the stack. Values: the stack, the inventory. */
    public static final Site CURRENT_ITEM = site("held item of a player inventory",
            new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.item.ItemStack.class, legacyconnector.v1_7_10.net.minecraft.entity.player.InventoryPlayer.class},
            "net/minecraft/entity/player/InventoryPlayer.func_70448_g()Lnet/minecraft/item/ItemStack;#tail").returns(legacyconnector.v1_7_10.net.minecraft.item.ItemStack.class);

    /** ItemStack.readFromNBT, last thing. Values: the stack, the tag it was read from. */
    public static final Site STACK_READ = site("item stack read from NBT",
            new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.item.ItemStack.class, legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound.class},
            "net/minecraft/item/ItemStack.func_77963_c(Lnet/minecraft/nbt/NBTTagCompound;)V#tail");

    /** ItemStack.getRarity: a hook that answers instead of {@code getItem().getRarity(stack)}. Values: the stack. */
    public static final Site ITEM_RARITY = site("item rarity", new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.item.ItemStack.class},
            "net/minecraft/item/ItemStack.func_77953_t()Lnet/minecraft/item/EnumRarity;#replace:func_77973_b()Lnet/minecraft/item/Item;")
            .returns(legacyconnector.v1_7_10.net.minecraft.item.EnumRarity.class);

    /**
     * ContainerFurnace.transferStackInSlot (shift click): a hook that carries the whole method out instead of the base one (coremods that repair
     * the furnace's shift click). Values: the container, the player, the slot index.
     */
    public static final Site FURNACE_SHIFT_CLICK = site("furnace shift click",
            new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.inventory.ContainerFurnace.class, PLAYER, int.class},
            "net/minecraft/inventory/ContainerFurnace.func_82846_b(Lnet/minecraft/entity/player/EntityPlayer;I)Lnet/minecraft/item/ItemStack;#replace:get(I)Ljava/lang/Object;")
            .returns(legacyconnector.v1_7_10.net.minecraft.item.ItemStack.class);

    /**
     * Explosion.doExplosionA: a hook that stands in for {@code EnchantmentProtection.getBlastDamageReduction(entity, knockback)} for each living
     * entity in the blast. Values: the entity, the knockback factor before the entity's protection.
     */
    public static final Site EXPLOSION_KNOCKBACK = site("explosion knockback reduction", new Class<?>[] {ENTITY, double.class},
            "net/minecraft/world/Explosion.func_77278_a()V#replace:func_92092_a(Lnet/minecraft/entity/Entity;D)D").returns(double.class);

    /** World.checkBlockCollision, last thing: a hook answering true adds a collision the blocks do not have. Values: the world, the box. */
    public static final Site BLOCK_COLLISION_EXTRA = site("extra block collision",
            new Class<?>[] {WORLD, legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB.class},
            "net/minecraft/world/World.func_72829_c(Lnet/minecraft/util/AxisAlignedBB;)Z#tail").returns(boolean.class);

    /** TileEntity.validate and invalidate, first thing (tile entities that keep or call the base method). Values: the tile entity. */
    public static final Site TILE_VALIDATE = site("tile entity validated", new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity.class},
            "net/minecraft/tileentity/TileEntity.func_145829_t()V#after:aload 0");
    public static final Site TILE_INVALIDATE = site("tile entity invalidated", new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity.class},
            "net/minecraft/tileentity/TileEntity.func_145843_s()V#after:aload 0");

    /** World.notifyBlocksOfNeighborChange, around the six updates. Values: world, x, y, z of the block that changed. */
    public static final Site NEIGHBORS_START = site("neighbour updates (start)", new Class<?>[] {WORLD, int.class, int.class, int.class},
            "net/minecraft/world/World.func_147459_d(IIILnet/minecraft/block/Block;)V#after:iload 1", "net/minecraft/world/World.func_147459_d(IIILnet/minecraft/block/Block;)V#head");
    public static final Site NEIGHBORS_STOP = site("neighbour updates (end)", new Class<?>[] {WORLD, int.class, int.class, int.class},
            "net/minecraft/world/World.func_147459_d(IIILnet/minecraft/block/Block;)V#tail");

    /** World.notifyBlockOfNeighborChange, first thing. Values: world, x, y, z of the block that is told. */
    public static final Site NEIGHBOR_NOTIFIED = site("neighbour update of one block", new Class<?>[] {WORLD, int.class, int.class, int.class},
            "net/minecraft/world/World.func_147460_e(IIILnet/minecraft/block/Block;)V#after:aload 0", "net/minecraft/world/World.func_147460_e(IIILnet/minecraft/block/Block;)V#head");

    /** EntityLivingBase.updatePotionEffects: the name of the particle an effect gives off ("mobSpell", "mobSpellAmbient"). Values: entity, name. */
    public static final Site POTION_PARTICLE_NAME = site("potion effect particle name", new Class<?>[] {ENTITY, String.class},
            "net/minecraft/entity/EntityLivingBase.func_70679_bo()V#const:\"mobSpell\"", "net/minecraft/entity/EntityLivingBase.func_70679_bo()V#const:\"mobSpellAmbient\"")
            .returns(String.class);

    /** EntityPlayer.getArmorVisibility: the share of armour pieces worn (what mobs see of the player), as the hook answers it. Values: amount, player. */
    public static final Site ARMOR_VISIBILITY = site("armour visibility", new Class<?>[] {float.class, PLAYER},
            "net/minecraft/entity/player/EntityPlayer.func_82243_bO()F#after:fdiv").returns(float.class);

    /** EntityPlayer.getHurtSound: the name of the sound ("game.player.hurt"). Values: player, name. */
    public static final Site PLAYER_HURT_SOUND = site("player hurt sound", new Class<?>[] {PLAYER, String.class},
            "net/minecraft/entity/player/EntityPlayer.func_70621_aR()Ljava/lang/String;#const:\"game.player.hurt\"").returns(String.class);

    /** BlockLiquid.onBlockAdded, first thing: true skips the method (the liquid is not scheduled to flow). Values: block, world, x, y, z. */
    public static final Site LIQUID_ADDED = site("liquid block added",
            new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.block.Block.class, WORLD, int.class, int.class, int.class},
            "net/minecraft/block/BlockLiquid.func_149726_b(Lnet/minecraft/world/World;III)V#cancel-after:iload 4");

    /** EntityFireworkRocket.onUpdate: the upward push per tick (0.04). Values: push, rocket. */
    public static final Site FIREWORK_LIFT = site("firework rocket lift", new Class<?>[] {double.class, ENTITY},
            "net/minecraft/entity/item/EntityFireworkRocket.func_70071_h_()V#after:ldc 0.04").returns(double.class);

    /** EntityLivingBase.moveEntityWithHeading: the horizontal distance moved in the tick, which drives the limb swing. Values: distance, entity. */
    public static final Site LIMB_SWING = site("limb swing distance", new Class<?>[] {float.class, LIVING},
            "net/minecraft/entity/EntityLivingBase.func_70612_e(FF)V#after:invokestatic net/minecraft/util/MathHelper.func_76133_a(D)F").returns(float.class);

    /** EntityFlying.moveEntityWithHeading, last thing (ghasts and other flying mobs). Values: the mob. */
    public static final Site FLYING_TRAVEL_END = site("flying mob movement (end)", new Class<?>[] {LIVING},
            "net/minecraft/entity/EntityFlying.func_70612_e(FF)V#tail");

    /** EntitySquid.onLivingUpdate, last thing. Values: the squid. */
    public static final Site SQUID_TICK = site("squid update", new Class<?>[] {LIVING},
            "net/minecraft/entity/passive/EntitySquid.func_70636_d()V#tail");

    /** ItemBow.onPlayerStoppedUsing: the ticks of a full draw (20). Values: ticks, player. */
    public static final Site BOW_DRAW_TIME = site("bow draw time", new Class<?>[] {float.class, PLAYER},
            "net/minecraft/item/ItemBow.func_77615_a(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;I)V#const:20.0")
            .returns(float.class);

    /** BlockPressurePlate.getPower: stands in for {@code world.getEntitiesWithinAABB(class, box)}. Values: world, entity class, box. */
    public static final Site PRESSURE_ENTITIES = site("entities on a pressure plate",
            new Class<?>[] {WORLD, Class.class, legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB.class},
            "net/minecraft/block/BlockPressurePlate.func_150065_e(Lnet/minecraft/world/World;III)I#replace:func_72872_a(Ljava/lang/Class;Lnet/minecraft/util/AxisAlignedBB;)Ljava/util/List;")
            .returns(List.class);

    /** BlockPressurePlate.getPower, after {@code entity.doesEntityNotTriggerPressurePlate()}: the answer is true for an entity that does not press. Values: answer, entity, sensitivity. */
    public static final Site PRESSURE_IGNORED = site("entity ignored by a pressure plate",
            new Class<?>[] {boolean.class, ENTITY, legacyconnector.v1_7_10.net.minecraft.block.BlockPressurePlate.Sensitivity.class},
            "net/minecraft/block/BlockPressurePlate.func_150065_e(Lnet/minecraft/world/World;III)I#after:invokevirtual net/minecraft/entity/Entity.func_145773_az()Z")
            .returns(boolean.class);

    /** World.setBlock: the block that is placed. Values: world, x, y, z, block, metadata, flags. */
    public static final Site SET_BLOCK_REPLACE = site("block placed in the world",
            new Class<?>[] {WORLD, int.class, int.class, int.class, legacyconnector.v1_7_10.net.minecraft.block.Block.class, int.class, int.class},
            "net/minecraft/world/World.func_147465_d(IIILnet/minecraft/block/Block;II)Z#after:aload 4", "net/minecraft/world/World.func_147465_d(IIILnet/minecraft/block/Block;II)Z#after:iload 5")
            .returns(legacyconnector.v1_7_10.net.minecraft.block.Block.class);

    /** World.setBlock: the build height (256) above which nothing is placed. Values: height, world, x, y, z, block, metadata, flags. */
    public static final Site SET_BLOCK_LIMIT = site("build height of block placement",
            new Class<?>[] {int.class, WORLD, int.class, int.class, int.class, legacyconnector.v1_7_10.net.minecraft.block.Block.class, int.class, int.class},
            "net/minecraft/world/World.func_147465_d(IIILnet/minecraft/block/Block;II)Z#after:sipush 256").returns(int.class);

    /** EntityAINearestAttackableTarget.shouldExecute: stands in for {@code world.selectEntitiesWithinAABB(class, box, selector)}. Values: the mob, entity class, box, selector. */
    public static final Site TARGET_CANDIDATES = site("mob target candidates",
            new Class<?>[] {LIVING, Class.class, legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB.class, legacyconnector.v1_7_10.net.minecraft.command.IEntitySelector.class},
            "net/minecraft/entity/ai/EntityAINearestAttackableTarget.func_75250_a()Z#replace:func_82733_a(Ljava/lang/Class;Lnet/minecraft/util/AxisAlignedBB;Lnet/minecraft/command/IEntitySelector;)Ljava/util/List;")
            .returns(List.class);

    /** NetHandlerPlayServer.processUseEntity: the squared distance within which a player may use an entity (36). Values: the connection, squared distance. */
    public static final Site USE_ENTITY_REACH = site("server reach of using an entity",
            new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer.class, double.class},
            "net/minecraft/network/NetHandlerPlayServer.func_147340_a(Lnet/minecraft/network/play/client/C02PacketUseEntity;)V#after:dload 5").returns(double.class);

    /** EntityTracker.addEntityToTracker: the most blocks any entity is tracked at (the view distance). Values: range, entity. */
    public static final Site TRACKING_MAX = site("entity tracking range limit", new Class<?>[] {int.class, ENTITY},
            "net/minecraft/entity/EntityTracker.func_72785_a(Lnet/minecraft/entity/Entity;IIZ)V#after:getfield net/minecraft/entity/EntityTracker.field_72792_d I").returns(int.class);

    /** BlockFence.addCollisionBoxesToList, first thing: a hook that answers true added the boxes itself and the block's own are left out. Values: fence, world, x, y, z, box, list, entity. */
    public static final Site FENCE_BOXES = site("fence collision boxes",
            new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.block.BlockFence.class, WORLD, int.class, int.class, int.class,
                    legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB.class, List.class, ENTITY},
            "net/minecraft/block/BlockFence.func_149743_a(Lnet/minecraft/world/World;IIILnet/minecraft/util/AxisAlignedBB;Ljava/util/List;Lnet/minecraft/entity/Entity;)V#cancel-after:iload 4");

    /** BlockPane.addCollisionBoxesToList, first thing: true gives the entity no collision with the pane. Values: pane, entity. */
    public static final Site PANE_IGNORED = site("pane collision ignored",
            new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.block.BlockPane.class, ENTITY},
            "net/minecraft/block/BlockPane.func_149743_a(Lnet/minecraft/world/World;IIILnet/minecraft/util/AxisAlignedBB;Ljava/util/List;Lnet/minecraft/entity/Entity;)V#head-cancel");

    /** BlockBasePressurePlate.updateTick, last thing. Values: plate, world, x, y, z. */
    public static final Site PLATE_TICK = site("pressure plate tick",
            new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.block.BlockBasePressurePlate.class, WORLD, int.class, int.class, int.class},
            "net/minecraft/block/BlockBasePressurePlate.func_149674_a(Lnet/minecraft/world/World;IIILjava/util/Random;)V#tail");

    /** Entity.moveEntity: the vertical movement after the collisions. Values: entity, movement after collisions, movement asked for. */
    public static final Site STEP_DOWN = site("entity step down", new Class<?>[] {ENTITY, double.class, double.class},
            "net/minecraft/entity/Entity.func_70091_d(DDD)V#after:aload 0").returns(double.class);

    /** Entity.moveEntity of an entity without collisions: a hook stands in for moving the bounding box. Values: entity, x, y, z. */
    public static final Site NOCLIP_MOVE = site("entity movement without collisions", new Class<?>[] {ENTITY, double.class, double.class, double.class},
            "net/minecraft/entity/Entity.func_70091_d(DDD)V#replace:func_72317_d(DDD)Lnet/minecraft/util/AxisAlignedBB;").plain();

    /** World.getClosestVulnerablePlayerToEntity: stands in for the closest player the mob may attack. Values: the mob, x, y, z, distance. */
    public static final Site CLOSEST_VULNERABLE = site("closest vulnerable player",
            new Class<?>[] {ENTITY, double.class, double.class, double.class, double.class},
            "net/minecraft/world/World.func_72856_b(Lnet/minecraft/entity/Entity;D)Lnet/minecraft/entity/player/EntityPlayer;#replace:func_72846_b(DDDD)Lnet/minecraft/entity/player/EntityPlayer;")
            .returns(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer.class);

    /** NetHandlerPlayServer.processUseEntity: stands in for the "is this entity an arrow" check that keeps players from attacking it. Values: the entity. */
    public static final Site ARROW_TARGET = site("entity counted as an arrow when attacked", new Class<?>[] {ENTITY},
            "net/minecraft/network/NetHandlerPlayServer.func_147340_a(Lnet/minecraft/network/play/client/C02PacketUseEntity;)V#before:ifne").returns(boolean.class);

    /** TileEntityFurnace.updateEntity, first thing: true skips the furnace's own update (another block took the furnace over). Values: the furnace. */
    public static final Site FURNACE_TAKEN_OVER = site("furnace taken over",
            new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityFurnace.class},
            "net/minecraft/tileentity/TileEntityFurnace.func_145845_h()V#head-cancel");

    /** The model sites: client classes, so apart from the rest. Values: model, entity, the six animation numbers. */
    public static final class Client {
        private static final Class<?>[] MODEL = {legacyconnector.v1_7_10.net.minecraft.client.model.ModelBiped.class, ENTITY, float.class, float.class, float.class,
                float.class, float.class, float.class};
        private static final String BIPED = "net/minecraft/client/model/ModelBiped.";
        /** ModelBiped.render, first and last thing. */
        public static final Site RENDER_PRE = site("biped model render (start)", MODEL, BIPED + "func_78088_a(Lnet/minecraft/entity/Entity;FFFFFF)V#after:aload 0",
                BIPED + "func_78088_a(Lnet/minecraft/entity/Entity;FFFFFF)V#head");
        public static final Site RENDER_POST = site("biped model render (end)", MODEL, BIPED + "func_78088_a(Lnet/minecraft/entity/Entity;FFFFFF)V#tail");
        /** ModelBiped.setRotationAngles, first and last thing. */
        public static final Site ANGLES_PRE = site("biped model pose (start)", MODEL, BIPED + "func_78087_a(FFFFFFLnet/minecraft/entity/Entity;)V#after:aload 0",
                BIPED + "func_78087_a(FFFFFFLnet/minecraft/entity/Entity;)V#head");
        public static final Site ANGLES_POST = site("biped model pose (end)", MODEL, BIPED + "func_78087_a(FFFFFFLnet/minecraft/entity/Entity;)V#tail");

        /** EntityPlayerSP.onLivingUpdate, after the keyboard was read. Values: player, its movement input. */
        public static final Site MOVEMENT_INPUT = site("movement input",
                new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.client.entity.EntityPlayerSP.class, legacyconnector.v1_7_10.net.minecraft.util.MovementInput.class},
                "net/minecraft/client/entity/EntityPlayerSP.func_70636_d()V#after:aload 0");
        /** Minecraft.rightClickMouse, first thing: true drops the click. */
        public static final Site USE_CLICK = site("right click", new Class<?>[0], "net/minecraft/client/Minecraft.func_147121_ag()V#head-cancel");
        /** Minecraft.startGame, last thing. */
        public static final Site GAME_STARTED = site("game started", new Class<?>[0], "net/minecraft/client/Minecraft.func_71384_a()V#tail");

        private static final Class<?> PLAYER_SP = legacyconnector.v1_7_10.net.minecraft.client.entity.EntityPlayerSP.class;
        private static final String MC = "net/minecraft/client/Minecraft.";

        /** Minecraft.clickMouse and sendClickBlockToController, first thing: true skips the left click (attack, block breaking). */
        public static final Site LEFT_CLICK = site("left click", new Class<?>[0], MC + "func_147116_af()V#head-cancel", MC + "func_147115_a(Z)V#head-cancel");
        /** Minecraft.rightClickMouse: the ticks before the next right click (4). */
        public static final Site RIGHT_CLICK_DELAY = site("right click delay", new Class<?>[] {int.class}, MC + "func_147121_ag()V#const:4").returns(int.class);
        /** PlayerControllerMP.onPlayerDestroyBlock, first thing: true leaves the block. */
        public static final Site BLOCK_DESTRUCTION = site("block destruction", new Class<?>[0], "net/minecraft/client/multiplayer/PlayerControllerMP.func_78751_a(IIII)Z#head");
        /** EntityPlayerSP.setSprinting: true keeps the player from sprinting. Values: player, sprinting. */
        public static final Site SPRINT = site("sprinting", new Class<?>[] {PLAYER_SP, boolean.class},
                "net/minecraft/client/entity/EntityPlayerSP.func_70031_b(Z)V#cancel-after:iload 1");
        /** EntityPlayerSP.getFOVMultiplier: stands in for {@code capabilities.getWalkSpeed()}. Values: capabilities, player. */
        public static final Site FOV_WALK_SPEED = site("field of view walk speed",
                new Class<?>[] {legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities.class, PLAYER_SP},
                "net/minecraft/client/entity/EntityPlayerSP.func_71151_f()F#replace:func_75094_b()F");
        /** EntityRenderer.updateLightmap: whether the player has night vision. Value: has it. */
        public static final Site NIGHT_VISION_ACTIVE = site("night vision (light map)", new Class<?>[] {boolean.class},
                "net/minecraft/client/renderer/EntityRenderer.func_78472_g(F)V#after:invokevirtual net/minecraft/client/entity/EntityClientPlayerMP.func_70644_a(Lnet/minecraft/potion/Potion;)Z")
                .returns(boolean.class);
        /** EntityRenderer.getNightVisionBrightness. Values: amount, player, partial tick. */
        public static final Site NIGHT_VISION_AMOUNT = site("night vision amount", new Class<?>[] {float.class, PLAYER, float.class},
                "net/minecraft/client/renderer/EntityRenderer.func_82830_a(Lnet/minecraft/entity/player/EntityPlayer;F)F#after:aload 1",
                "net/minecraft/client/renderer/EntityRenderer.func_82830_a(Lnet/minecraft/entity/player/EntityPlayer;F)F#tail").returns(float.class);

        /** GuiSleepMP.wakeFromSleep (the "Leave Bed" button), last thing. */
        public static final Site SLEEP_CANCELLED = site("leave bed button", new Class<?>[0], "net/minecraft/client/gui/GuiSleepMP.func_146418_g()V#tail");

        /** GuiContainer.keyTyped, last thing. Values: character, 1.7.10 key code. */
        public static final Site CONTAINER_KEY = site("key typed in a container screen", new Class<?>[] {char.class, int.class},
                "net/minecraft/client/gui/inventory/GuiContainer.func_73869_a(CI)V#tail");

        /** {@link #LEFT_CLICK}. */
        public static boolean leftClick() {
            return LEFT_CLICK.active() && LEFT_CLICK.cancels();
        }

        /** {@link #RIGHT_CLICK_DELAY}. */
        public static int rightClickDelay(int ticks) {
            return RIGHT_CLICK_DELAY.active() ? ((Number) RIGHT_CLICK_DELAY.modify(0, ticks)).intValue() : ticks;
        }

        /** {@link #BLOCK_DESTRUCTION}. */
        public static boolean blockDestruction() {
            return BLOCK_DESTRUCTION.active() && BLOCK_DESTRUCTION.cancels();
        }

        /** {@link #SPRINT}: whether a hook keeps this entity from sprinting (only the local player is a client player). */
        public static boolean sprintCancelled(net.minecraft.world.entity.LivingEntity entity, boolean sprinting) {
            if (!sprinting || !SPRINT.active() || !(entity instanceof net.minecraft.client.player.LocalPlayer)) return false;
            return SPRINT.cancels(EntityBridge.wrap(entity), true);
        }

        /** {@link #FOV_WALK_SPEED}. */
        public static float fovWalkSpeed(net.minecraft.client.player.AbstractClientPlayer player, float walkSpeed) {
            if (!FOV_WALK_SPEED.active() || !(player instanceof net.minecraft.client.player.LocalPlayer)
                    || !(EntityBridge.wrap(player) instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer view)) return walkSpeed;
            return FOV_WALK_SPEED.replace(walkSpeed, view.capabilities, view) instanceof Number n ? n.floatValue() : walkSpeed;
        }

        /** {@link #NIGHT_VISION_ACTIVE}. */
        public static boolean nightVisionActive(boolean active) {
            return NIGHT_VISION_ACTIVE.active() ? Boolean.TRUE.equals(NIGHT_VISION_ACTIVE.modify(0, active)) : active;
        }

        /** {@link #NIGHT_VISION_AMOUNT}. */
        public static float nightVisionAmount(net.minecraft.world.entity.LivingEntity entity, float amount, float partialTick) {
            if (!NIGHT_VISION_AMOUNT.active() || !(EntityBridge.wrap(entity) instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer view)) return amount;
            return ((Number) NIGHT_VISION_AMOUNT.modify(0, amount, view, partialTick)).floatValue();
        }

        private Client() {}
    }

    // ---- what the modern places call (mixins): the values as legacy views

    /** {@link #TRAVEL}: whether a hook took the movement step over (the travel vector is strafe, up, forward). */
    public static boolean travel(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.phys.Vec3 travel) {
        return TRAVEL.active() && TRAVEL.cancels(EntityBridge.wrap(entity), (float) travel.x, (float) travel.z);
    }

    /** {@link #GRAVITY}. */
    public static double gravity(net.minecraft.world.entity.Entity entity, double gravity) {
        if (gravity == 0.0 || !GRAVITY.active()) return gravity;
        return ((Number) GRAVITY.modify(0, gravity, EntityBridge.wrap(entity))).doubleValue();
    }

    /** {@link #HEAL_RATE}. */
    public static int healRate(net.minecraft.world.entity.player.Player player, int ticks) {
        if (player.level().isClientSide || !HEAL_RATE.active()) return ticks;
        return ((Number) HEAL_RATE.modify(1, EntityBridge.wrap(player), ticks)).intValue();
    }

    /** {@link #WEATHER}: whether a hook skips this weather step. */
    public static boolean skipsWeather(net.minecraft.world.level.Level level) {
        return WEATHER.active() && WEATHER.cancels(legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level));
    }

    /** {@link #SWING_TICKS}. */
    public static int swingTicks(net.minecraft.world.entity.LivingEntity entity, int ticks) {
        if (!SWING_TICKS.active()) return ticks;
        return ((Number) SWING_TICKS.modify(0, ticks, EntityBridge.wrap(entity))).intValue();
    }

    /** {@link #SPRINT_PARTICLES}. */
    public static boolean sprintParticles(net.minecraft.world.entity.Entity entity, boolean sprinting) {
        if (!SPRINT_PARTICLES.active()) return sprinting;
        return Boolean.TRUE.equals(SPRINT_PARTICLES.modify(0, sprinting, EntityBridge.wrap(entity)));
    }

    /** {@link #PLAYER_ATTACK}: the hook's answer, or null when there is none (the game then damages the target itself). */
    public static Boolean playerAttack(net.minecraft.world.entity.player.Player attacker, net.minecraft.world.entity.Entity target,
            net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!PLAYER_ATTACK.active()) return null;
        return PLAYER_ATTACK.replace(null, EntityBridge.wrap(target), EventBridge1710.legacySource(source), amount, EntityBridge.wrap(attacker)) instanceof Boolean b ? b : null;
    }

    /** {@link #GRAVITY_LIVING}, {@link #GRAVITY_ITEM}, {@link #GRAVITY_ARROW}. */
    public static double gravityConstant(net.minecraft.world.entity.Entity entity, double gravity) {
        if (gravity == 0.0) return gravity;
        Site site = entity instanceof net.minecraft.world.entity.LivingEntity ? GRAVITY_LIVING
                : entity instanceof net.minecraft.world.entity.item.ItemEntity ? GRAVITY_ITEM
                : entity instanceof net.minecraft.world.entity.projectile.AbstractArrow ? GRAVITY_ARROW : null;
        if (site == null || !site.active()) return gravity;
        double constant = site == GRAVITY_LIVING ? 0.08 : site == GRAVITY_ITEM ? 0.03999999910593033 : 0.05;
        Object[] values = {EntityBridge.wrap(entity)};
        for (Bound b : site.bound()) {
            if (!b.combiner && site.call(b, values) instanceof Number n) gravity *= n.doubleValue() / constant;
        }
        return gravity;
    }

    /** Whether one of the gravity constant sites has a hook (known from the start). */
    public static boolean gravityConstantListed() {
        return GRAVITY_LIVING.listed() || GRAVITY_ITEM.listed() || GRAVITY_ARROW.listed();
    }

    /** {@link #NEIGHBOR_POWERED}. */
    public static boolean neighborPowered(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, boolean powered) {
        if (!NEIGHBOR_POWERED.active()) return powered;
        return Boolean.TRUE.equals(NEIGHBOR_POWERED.modify(0, powered, legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level), pos.getX(), pos.getY(), pos.getZ()));
    }

    /** {@link #POWER_LEVEL}. */
    public static int powerLevel(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side, int power) {
        if (!POWER_LEVEL.active()) return power;
        legacyconnector.v1_7_10.net.minecraft.world.World world = legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level);
        return ((Number) POWER_LEVEL.modify(0, power, world, world.getBlock(pos.getX(), pos.getY(), pos.getZ()), pos.getX(), pos.getY(), pos.getZ(), side.get3DDataValue()))
                .intValue();
    }

    /** {@link #TRACKING_RANGE}: the range in blocks. */
    public static int trackingRange(net.minecraft.world.entity.Entity entity, int range) {
        if (!TRACKING_RANGE.active()) return range;
        return ((Number) TRACKING_RANGE.modify(0, range, EntityBridge.wrap(entity), 3)).intValue();
    }

    /** {@link #STEP_DISTANCE}: Entity.nextStep returns (int) distance + 1; the hook replaces that 1. */
    public static float stepDistance(net.minecraft.world.entity.Entity entity, float next) {
        if (!STEP_DISTANCE.active()) return next;
        return next - 1.0F + ((Number) STEP_DISTANCE.modify(1, EntityBridge.wrap(entity), 1)).floatValue();
    }

    public static void itemTick(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.Entity entity, int slot, boolean held) {
        if (ITEM_TICK.active()) ITEM_TICK.fire(legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$wrap(stack), EntityBridge.wrap(entity), slot, held);
    }

    public static void itemUsed(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.context.UseOnContext context) {
        if (context.getPlayer() == null || !ITEM_USED.active()) return;
        net.minecraft.core.BlockPos pos = context.getClickedPos();
        net.minecraft.world.phys.Vec3 hit = context.getClickLocation();
        ITEM_USED.fire(legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$wrap(stack), EntityBridge.wrap(context.getPlayer()),
                legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(context.getLevel()), pos.getX(), pos.getY(), pos.getZ(),
                context.getClickedFace().get3DDataValue(), (float) (hit.x - pos.getX()), (float) (hit.y - pos.getY()), (float) (hit.z - pos.getZ()));
    }

    public static void sprintStarted(net.minecraft.world.entity.player.Player player) {
        if (SPRINT_STARTED.active()) SPRINT_STARTED.fire(EntityBridge.wrap(player));
    }

    public static int anvilMaxCost(int cost) {
        return ANVIL_MAX_COST.active() ? ((Number) ANVIL_MAX_COST.modify(0, cost)).intValue() : cost;
    }

    public static boolean aiTasks(net.minecraft.world.entity.Mob mob) {
        return AI_TASKS.active() && AI_TASKS.cancels(EntityBridge.wrap(mob));
    }

    public static int enchantability(net.minecraft.world.item.ItemStack stack, int value) {
        if (!ENCHANTABILITY.active()) return value;
        return ((Number) ENCHANTABILITY.modify(1, legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$wrap(stack), value)).intValue();
    }

    /** {@link #EXPLOSION_KNOCKBACK}: the factor after the hook (the game's own protection is applied to it afterwards, as the shim's reduction is the identity). */
    public static double explosionKnockback(net.minecraft.world.entity.LivingEntity entity, double factor) {
        if (!EXPLOSION_KNOCKBACK.active()) return factor;
        return ((Number) EXPLOSION_KNOCKBACK.modify(1, EntityBridge.wrap(entity), factor)).doubleValue();
    }

    /** {@link #BLOCK_COLLISION_EXTRA}. */
    public static boolean blockCollisionExtra(legacyconnector.v1_7_10.net.minecraft.world.World world, legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB box) {
        return BLOCK_COLLISION_EXTRA.active() && BLOCK_COLLISION_EXTRA.cancels(world, box);
    }

    public static void neighbors(Site site, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        if (site.active()) site.fire(legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level), pos.getX(), pos.getY(), pos.getZ());
    }

    private static final ThreadLocal<Boolean> IN_RAIN = new ThreadLocal<>();

    /** {@link #RAIN_STRENGTH}. */
    public static float rainStrength(net.minecraft.world.level.Level level, float strength, float partialTick) {
        if (!RAIN_STRENGTH.active() || IN_RAIN.get() != null) return strength;
        IN_RAIN.set(Boolean.TRUE); // a hook that asks the world itself gets the game's answer
        try {
            return RAIN_STRENGTH.replace(strength, legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level), partialTick) instanceof Number n ? n.floatValue() : strength;
        } finally {
            IN_RAIN.remove();
        }
    }

    /** {@link #BLOCK_EXPLODED}. */
    public static void blockExploded(net.minecraft.world.level.block.Block block, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        if (!BLOCK_EXPLODED.active()) return;
        BLOCK_EXPLODED.fire(BlockBridge.legacyBlock(block.defaultBlockState()), legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level), pos.getX(), pos.getY(), pos.getZ());
    }

    /** {@link #POTION_PARTICLE_NAME}: the particle the game picked, or the one the hook named instead. */
    public static net.minecraft.core.particles.ParticleOptions potionParticle(net.minecraft.world.entity.LivingEntity entity, net.minecraft.core.particles.ParticleOptions original) {
        if (!POTION_PARTICLE_NAME.active()) return original;
        if (!(POTION_PARTICLE_NAME.modify(1, EntityBridge.wrap(entity), "mobSpell") instanceof String name) || name.equals("mobSpell") || name.equals("mobSpellAmbient")) return original;
        net.minecraft.core.particles.ParticleOptions named = SoundBridge.particle(name, 0.0, 0.0, 0.0);
        return named != null ? named : original;
    }

    /** {@link #ARMOR_VISIBILITY}. */
    public static float armorVisibility(net.minecraft.world.entity.LivingEntity entity, float amount) {
        if (!(entity instanceof net.minecraft.world.entity.player.Player) || !ARMOR_VISIBILITY.active()) return amount;
        return ((Number) ARMOR_VISIBILITY.modify(0, amount, EntityBridge.wrap(entity))).floatValue();
    }

    /** {@link #PLAYER_HURT_SOUND}: the sound the game picked, or the one the hook named instead. */
    public static net.minecraft.sounds.SoundEvent playerHurtSound(net.minecraft.world.entity.player.Player player, net.minecraft.sounds.SoundEvent original) {
        if (!PLAYER_HURT_SOUND.active()) return original;
        if (!(PLAYER_HURT_SOUND.modify(1, EntityBridge.wrap(player), "game.player.hurt") instanceof String name) || name.equals("game.player.hurt")) return original;
        net.minecraft.sounds.SoundEvent named = SoundBridge.sound(name);
        return named != null ? named : original;
    }

    /** {@link #LIQUID_ADDED}: whether a hook skips the liquid's own start-up. */
    public static boolean liquidAdded(net.minecraft.world.level.block.Block block, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        return LIQUID_ADDED.active() && LIQUID_ADDED.cancels(BlockBridge.legacyBlock(block.defaultBlockState()), legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level),
                pos.getX(), pos.getY(), pos.getZ());
    }

    /** {@link #FIREWORK_LIFT}. */
    public static double fireworkLift(net.minecraft.world.entity.Entity rocket, double lift) {
        if (!FIREWORK_LIFT.active()) return lift;
        return ((Number) FIREWORK_LIFT.modify(0, lift, EntityBridge.wrap(rocket))).doubleValue();
    }

    /** {@link #LIMB_SWING}. */
    public static float limbSwing(net.minecraft.world.entity.LivingEntity entity, float distance) {
        if (!LIMB_SWING.active()) return distance;
        return ((Number) LIMB_SWING.modify(0, distance, EntityBridge.wrap(entity))).floatValue();
    }

    /** {@link #FLYING_TRAVEL_END}. */
    public static void flyingTravelEnd(net.minecraft.world.entity.Mob mob) {
        if (FLYING_TRAVEL_END.active()) FLYING_TRAVEL_END.fire(EntityBridge.wrap(mob));
    }

    // ---- Fisk-style server places (pressure plates, block placement, mob targets, reach)

    /** {@link #PRESSURE_ENTITIES} and {@link #PRESSURE_IGNORED}: the count of entities that press a plate, or null when no hook is listed. */
    public static Integer pressureCount(net.minecraft.world.level.Level level, net.minecraft.world.phys.AABB box, Class<? extends net.minecraft.world.entity.Entity> cls) {
        boolean list = PRESSURE_ENTITIES.active();
        boolean ignore = PRESSURE_IGNORED.active();
        if (!list && !ignore) return null;
        boolean mobs = cls != net.minecraft.world.entity.Entity.class; // PressurePlateBlock: EVERYTHING reads Entity, MOBS reads LivingEntity
        legacyconnector.v1_7_10.net.minecraft.block.BlockPressurePlate.Sensitivity sensitivity = mobs
                ? legacyconnector.v1_7_10.net.minecraft.block.BlockPressurePlate.Sensitivity.mobs
                : legacyconnector.v1_7_10.net.minecraft.block.BlockPressurePlate.Sensitivity.everything;
        if (list && PRESSURE_ENTITIES.replace(null, legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level), mobs ? LIVING : ENTITY,
                legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB.mcc$of(box)) instanceof List<?> found) {
            int n = 0;
            for (Object o : found) {
                if (ignore && Boolean.TRUE.equals(PRESSURE_IGNORED.modify(0, false, o, sensitivity))) continue;
                n++;
            }
            return n;
        }
        int n = 0;
        for (net.minecraft.world.entity.Entity e : level.getEntitiesOfClass(cls, box)) {
            boolean skip = e.isIgnoringBlockTriggers();
            if (ignore) skip = Boolean.TRUE.equals(PRESSURE_IGNORED.modify(0, skip, EntityBridge.wrap(e), sensitivity));
            if (!skip) n++;
        }
        return n;
    }

    private static final ThreadLocal<Boolean> IN_SET_BLOCK = new ThreadLocal<>();

    /**
     * {@link #SET_BLOCK_REPLACE} and {@link #SET_BLOCK_LIMIT}: the state to place instead (the same one when nothing changes), or null when the height limit
     * a hook set keeps the block from being placed.
     */
    public static net.minecraft.world.level.block.state.BlockState setBlock(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state) {
        if (IN_SET_BLOCK.get() != null || !(SET_BLOCK_REPLACE.active() || SET_BLOCK_LIMIT.active())) return state;
        IN_SET_BLOCK.set(Boolean.TRUE);
        try {
            legacyconnector.v1_7_10.net.minecraft.world.World world = legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level);
            legacyconnector.v1_7_10.net.minecraft.block.Block block = BlockBridge.legacyBlock(state);
            int meta = BlockBridge.legacyMeta(state);
            if (SET_BLOCK_LIMIT.active()) {
                int limit = ((Number) SET_BLOCK_LIMIT.modify(0, 256, world, pos.getX(), pos.getY(), pos.getZ(), block, meta, 3)).intValue();
                if (limit != 256 && pos.getY() >= limit) return null;
            }
            if (SET_BLOCK_REPLACE.active()) {
                Object r = SET_BLOCK_REPLACE.modify(4, world, pos.getX(), pos.getY(), pos.getZ(), block, meta, 3);
                if (r instanceof legacyconnector.v1_7_10.net.minecraft.block.Block replacement && replacement != block) return BlockBridge.toModern(replacement, meta);
            }
            return state;
        } finally {
            IN_SET_BLOCK.remove();
        }
    }

    /**
     * {@link #TARGET_CANDIDATES}: whether the hook's candidate list for a box around the target leaves the target out (the mob does not see it). The hook is
     * asked for the entities of that box as {@code world.selectEntitiesWithinAABB} would give them.
     */
    public static boolean targetLeftOut(net.minecraft.world.entity.Mob mob, net.minecraft.world.entity.LivingEntity target) {
        if (!TARGET_CANDIDATES.active()) return false;
        Object found = TARGET_CANDIDATES.replace(null, EntityBridge.wrap(mob), LIVING, legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB.mcc$of(target.getBoundingBox()), null);
        return found instanceof List<?> list && !list.contains(EntityBridge.wrap(target));
    }

    private static final Map<net.minecraft.world.entity.player.Player, legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer> HANDLERS =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** {@link #USE_ENTITY_REACH}: the entity interaction range (blocks) after the hook, which answered for 1.7.10's 6 blocks (36 squared). */
    public static double useEntityReach(net.minecraft.world.entity.player.Player player, double range) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer) || !USE_ENTITY_REACH.active()) return range;
        legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer handler = HANDLERS.computeIfAbsent(player,
                p -> new legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer(
                        (legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP) EntityBridge.wrap(p)));
        double squared = ((Number) USE_ENTITY_REACH.modify(1, handler, 36.0)).doubleValue();
        return squared == 36.0 || squared <= 0.0 ? range : range * Math.sqrt(squared / 36.0);
    }

    /** {@link #TRACKING_MAX}: the largest tracking range in blocks (the game's own is the player's view distance). */
    public static int trackingMax(net.minecraft.world.entity.Entity entity, int max) {
        if (!TRACKING_MAX.active()) return max;
        int hooked = ((Number) TRACKING_MAX.modify(0, max, EntityBridge.wrap(entity))).intValue();
        return Math.max(hooked, max);
    }

    private static net.minecraft.world.phys.shapes.VoxelShape shapeOf(List<?> boxes, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.phys.shapes.VoxelShape shape = net.minecraft.world.phys.shapes.Shapes.empty();
        for (Object o : boxes) {
            if (o instanceof legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB box) {
                shape = net.minecraft.world.phys.shapes.Shapes.or(shape,
                        net.minecraft.world.phys.shapes.Shapes.create(box.mcc$modern().move(-pos.getX(), -pos.getY(), -pos.getZ())));
            }
        }
        return shape;
    }

    /**
     * {@link #FENCE_BOXES} and {@link #PANE_IGNORED}: the collision shape a hook put in place of the fence's or the pane's, or null (the block's own).
     * Only collisions of entities are asked (the hooks look at the entity).
     */
    public static net.minecraft.world.phys.shapes.VoxelShape crossShape(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter getter,
            net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        if (!(context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext ec) || ec.getEntity() == null
                || !(getter instanceof net.minecraft.world.level.LevelAccessor level)) return null;
        boolean fence = state.getBlock() instanceof net.minecraft.world.level.block.FenceBlock;
        if (fence ? !FENCE_BOXES.active() : !(state.getBlock() instanceof net.minecraft.world.level.block.IronBarsBlock && PANE_IGNORED.active())) return null;
        legacyconnector.v1_7_10.net.minecraft.block.Block block = BlockBridge.legacyBlock(state);
        legacyconnector.v1_7_10.net.minecraft.entity.Entity entity = EntityBridge.wrap(ec.getEntity());
        if (!fence) return PANE_IGNORED.cancels(block, entity) ? net.minecraft.world.phys.shapes.Shapes.empty() : null;
        List<Object> boxes = new ArrayList<>();
        legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB mask = legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB.mcc$of(new net.minecraft.world.phys.AABB(pos).inflate(3.0));
        return FENCE_BOXES.cancels(block, legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level), pos.getX(), pos.getY(), pos.getZ(), mask, boxes, entity)
                ? shapeOf(boxes, pos) : null;
    }

    /** {@link #PLATE_TICK}. */
    public static void plateTick(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        if (PLATE_TICK.active()) PLATE_TICK.fire(BlockBridge.legacyBlock(state), legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level), pos.getX(), pos.getY(), pos.getZ());
    }

    /** {@link #STEP_DOWN}: the vertical movement the hook leaves after the collisions. */
    public static double stepDown(net.minecraft.world.entity.Entity entity, double collided, double asked) {
        if (!STEP_DOWN.active()) return collided;
        return ((Number) STEP_DOWN.modify(1, EntityBridge.wrap(entity), collided, asked)).doubleValue();
    }

    /** {@link #NOCLIP_MOVE}: whether a hook moved the entity. */
    public static boolean noClipMove(net.minecraft.world.entity.Entity entity, net.minecraft.world.phys.Vec3 move) {
        return NOCLIP_MOVE.active() && NOCLIP_MOVE.stands(EntityBridge.wrap(entity), move.x, move.y, move.z);
    }

    /** {@link #CLOSEST_VULNERABLE}: the hook's player (or none) as the only element, or null when no hook answered. */
    public static Object[] closestVulnerable(net.minecraft.world.entity.Mob mob, double distance) {
        if (!CLOSEST_VULNERABLE.active()) return null;
        return CLOSEST_VULNERABLE.answer(EntityBridge.wrap(mob), mob.getX(), mob.getY(), mob.getZ(), distance);
    }

    /** {@link #ARROW_TARGET}: the hook's answer (true when there is none). */
    public static boolean isArrowTarget(net.minecraft.world.entity.Entity entity) {
        return !ARROW_TARGET.active() || !Boolean.FALSE.equals(ARROW_TARGET.replace(Boolean.TRUE, EntityBridge.wrap(entity)));
    }

    /** {@link #FURNACE_TAKEN_OVER}. */
    public static boolean furnaceTakenOver(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        if (!FURNACE_TAKEN_OVER.active()) return false;
        if (!(TileEntityBridge.legacyAt(legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level), pos) instanceof legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityFurnace furnace)) return false;
        return FURNACE_TAKEN_OVER.cancels(furnace);
    }

    private CoremodSites() {}

    /** One place of 1.7.10's code on the modern game. */
    public static final class Site {
        private final String label;
        private final Class<?>[] offered;
        private final List<Listed> listed = new ArrayList<>();
        private volatile Bound[] hooks;
        private int failures;
        private Class<?> result;
        private boolean plain;
        private String constant;

        private Site(String label, Class<?>[] offered, String[] keys) {
            this.label = label;
            this.offered = offered;
            Map<String, Listed> once = new LinkedHashMap<>(); // several 1.7.10 places can be one modern place: each hook method once
            for (String key : keys) {
                List<Listed> found = key.startsWith("=") ? EXACT.getOrDefault(key.substring(1), List.of()) : LISTED.getOrDefault(key, List.of());
                for (Listed l : found) once.putIfAbsent(l.owner + "." + l.name + l.desc, l);
            }
            listed.addAll(once.values());
        }

        /** Only hooks returning this type answer here (a place can be listed by hooks of other shapes, which stand for other changes). */
        public Site returns(Class<?> type) {
            this.result = type;
            return this;
        }

        /**
         * Hooks here are never value joiners: a hook of the shape {@code (II)I} (or {@code (FF)F}...) is otherwise taken for the helper that combines a value with
         * another hook's result, which is right where a hook changes a value without receiving it, and wrong where the place offers two numbers.
         */
        public Site plain() {
            this.plain = true;
            return this;
        }

        /**
         * The constant a hook parameter of an enum type takes when the place offers no value of that type: coremods tell their hook which action it is for by
         * passing a constant of their own enum, chosen by the place they patched (for example "the jump of a sprinting player").
         */
        public Site named(String constantName) {
            this.constant = constantName;
            return this;
        }

        /** Whether the loader listed a hook here (known from the start; the hooks may not be callable yet). */
        public boolean listed() {
            return !listed.isEmpty();
        }

        /** Whether there is a hook to call: check before building the values. */
        public boolean active() {
            if (listed.isEmpty()) return false;
            Bound[] h = hooks;
            if (h == null) {
                if (!ready()) return false;
                h = resolve();
            }
            return h.length > 0;
        }

        /** Calls every hook; true when one answered true (a head hook that cancels the method). */
        public boolean cancels(Object... values) {
            boolean cancel = false;
            for (Bound b : bound()) {
                if (!b.combiner && Boolean.TRUE.equals(call(b, values))) cancel = true;
            }
            return cancel;
        }

        /** Calls every hook, ignoring results. */
        public void fire(Object... values) {
            for (Bound b : bound()) if (!b.combiner) call(b, values);
        }

        /**
         * Calls the hooks that stand in for a call of the game: true when one of them ran through, so the game's own call is left out. A hook that fails (or
         * is for another subclass) does not count, the game's call then still happens.
         */
        public boolean stands(Object... values) {
            boolean ran = false;
            for (Bound b : bound()) {
                if (b.combiner) continue;
                try {
                    Object[] args = new Object[b.from.length];
                    boolean fits = true;
                    for (int i = 0; i < args.length && fits; i++) {
                        Object v = b.from[i] < 0 ? b.fixed[i] : values[b.from[i]];
                        fits = b.narrowed[i] == null || v == null || b.narrowed[i].isInstance(v);
                        args[i] = v;
                    }
                    if (!fits) continue;
                    b.handle.invoke(args);
                    ran = true;
                } catch (Throwable t) {
                    if (failures++ < 3) LOG.warn("A coremod hook failed ({}, {})", label, b.name, t);
                }
            }
            return ran;
        }

        /** Passes {@code values[index]} through every hook (each result replaces it) and returns it. */
        public Object modify(int index, Object... values) {
            Bound[] all = bound();
            for (Bound b : all) {
                if (b.combiner) continue;
                Object r = call(b, values);
                if (r == null) continue;
                Bound combine = takes(b, index) ? null : combiner(all, b);
                if (combine != null) {
                    Object joined = call(combine, new Object[] {values[index], r});
                    if (joined != null) values[index] = joined;
                } else {
                    values[index] = r;
                }
            }
            return values[index];
        }

        /** The answer of the first hook that ran, as the only element of an array (null inside for "nothing"), or null when no hook ran. */
        public Object[] answer(Object... values) {
            for (Bound b : bound()) {
                if (b.combiner) continue;
                try {
                    Object[] args = new Object[b.from.length];
                    boolean fits = true;
                    for (int i = 0; i < args.length && fits; i++) {
                        Object v = b.from[i] < 0 ? b.fixed[i] : values[b.from[i]];
                        fits = b.narrowed[i] == null || v == null || b.narrowed[i].isInstance(v);
                        args[i] = v;
                    }
                    if (fits) return new Object[] {b.handle.invoke(args)};
                } catch (Throwable t) {
                    if (failures++ < 3) LOG.warn("A coremod hook failed ({}, {})", label, b.name, t);
                }
            }
            return null;
        }

        /** The result of the first hook (one that stands in for a call), or {@code fallback} when it failed. */
        public Object replace(Object fallback, Object... values) {
            for (Bound b : bound()) {
                if (b.combiner) continue;
                Object r = call(b, values);
                return r != null ? r : fallback;
            }
            return fallback;
        }

        private Bound[] bound() {
            Bound[] h = hooks;
            return h != null ? h : active() ? hooks : NONE;
        }

        private Object call(Bound b, Object[] values) {
            try {
                Object[] args = new Object[b.from.length];
                for (int i = 0; i < args.length; i++) {
                    Object v = b.from[i] < 0 ? b.fixed[i] : values[b.from[i]];
                    if (b.narrowed[i] != null && v != null && !b.narrowed[i].isInstance(v)) return null; // the hook is for a subclass
                    args[i] = v;
                }
                return b.handle.invoke(args);
            } catch (Throwable t) {
                if (failures++ < 3) LOG.warn("A coremod hook failed ({}, {})", label, b.name, t);
                return null;
            }
        }

        private synchronized Bound[] resolve() {
            if (hooks != null) return hooks;
            List<Bound> out = new ArrayList<>();
            for (Listed l : listed) {
                Class<?> joins = plain ? null : combines(l.desc);
                Bound b = joins != null ? bind(l, new Class<?>[] {joins, joins}, true, null, null) : bind(l, offered, false, result, constant);
                if (b != null) out.add(b);
            }
            return hooks = out.toArray(NONE);
        }
    }

    private record Bound(String name, MethodHandle handle, int[] from, Class<?>[] narrowed, Object[] fixed, boolean combiner, ClassLoader mod) {}

    private static boolean takes(Bound b, int index) {
        for (int from : b.from) if (from == index) return true;
        return false;
    }

    /** The helper of the same mod that joins a value with a hook's result. */
    private static Bound combiner(Bound[] all, Bound hook) {
        for (Bound b : all) if (b.combiner && b.mod == hook.mod) return b;
        return null;
    }

    /** The type a helper like {@code multiply(FF)F} joins, or null when the descriptor is not of that form. */
    private static Class<?> combines(String desc) {
        return switch (desc) {
            case "(ZZ)Z" -> boolean.class;
            case "(II)I" -> int.class;
            case "(FF)F" -> float.class;
            case "(DD)D" -> double.class;
            case "(JJ)J" -> long.class;
            default -> null;
        };
    }

    private static final Bound[] NONE = new Bound[0];

    /** A place; other legacy versions declare theirs with their own types ({@code CoremodSites1122}), the lists and the binding are shared. */
    public static Site site(String label, Class<?>[] offered, String... keys) {
        Site s = new Site(label, offered, keys);
        SITES.add(s);
        return s;
    }

    /** Whether the loader listed any hook at all (cheap; false in a pack without coremods that patch Minecraft). */
    public static boolean any() {
        return !LISTED.isEmpty();
    }

    /** Server side wiring of the sites that are not a place in code: the tick length goes to the game's tick rate manager (which tells the clients too). */
    public static void register(net.neoforged.bus.api.IEventBus gameBus) {
        if (LISTED.isEmpty()) return;
        reportOnStart(gameBus);
        if (ENTITY_REMOVED.listed()) {
            gameBus.addListener((net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent e) -> {
                net.minecraft.world.entity.Entity entity = e.getEntity();
                // 1.7.10 removed entities that died, were discarded or logged out through this method; a chunk unload went another way
                net.minecraft.world.entity.Entity.RemovalReason reason = entity.getRemovalReason();
                if (reason == null || !(reason.shouldDestroy() || entity instanceof net.minecraft.world.entity.player.Player)) return;
                if (ENTITY_REMOVED.active()) ENTITY_REMOVED.fire(legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(e.getLevel()), EntityBridge.wrap(entity));
            });
        }
        if (ARROW_TICK.listed()) gameBus.addListener((net.neoforged.neoforge.event.tick.EntityTickEvent.Pre e) -> {
            if (e.getEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow && ARROW_TICK.active()) ARROW_TICK.fire(EntityBridge.wrap(e.getEntity()));
        });
        if (TICK_MILLIS.listed()) {
            float[] last = {20.0F};
            gameBus.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> {
                if (!TICK_MILLIS.active()) return;
                long millis = ((Number) TICK_MILLIS.modify(0, 50L)).longValue();
                if (millis <= 0) return;
                float rate = Math.max(1.0F, Math.min(100.0F, 1000.0F / millis));
                if (Math.abs(rate - last[0]) > 0.01F) { // only changes are applied, so /tick rate keeps working while the mods ask for the normal length
                    last[0] = rate;
                    e.getServer().tickRateManager().setTickRate(rate);
                }
            });
        }
        if (PLAYER_DAMAGE.listed()) {
            gameBus.addListener((net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e) -> {
                if (!(e.getEntity() instanceof net.minecraft.world.entity.player.Player player) || !PLAYER_DAMAGE.active()) return;
                float amount = e.getAmount();
                float changed = ((Number) PLAYER_DAMAGE.modify(2, EntityBridge.wrap(player), EventBridge1710.legacySource(e.getSource()), amount)).floatValue();
                if (changed != amount) e.setAmount(changed);
            });
        }
        if (XP_ORB_TICK.listed()) {
            gameBus.addListener((net.neoforged.neoforge.event.tick.EntityTickEvent.Post e) -> {
                if (e.getEntity() instanceof net.minecraft.world.entity.ExperienceOrb orb && XP_ORB_TICK.active()) XP_ORB_TICK.fire(EntityBridge.wrap(orb));
            });
        }
        if (FARMLAND_TRAMPLE.listed()) {
            gameBus.addListener((net.neoforged.neoforge.event.level.BlockEvent.FarmlandTrampleEvent e) -> {
                if (!(e.getLevel() instanceof net.minecraft.world.level.Level level) || !FARMLAND_TRAMPLE.active()) return;
                // the hook decides and turns the farmland to dirt itself, as the call it replaced did
                e.setCanceled(true);
                net.minecraft.core.BlockPos pos = e.getPos();
                FARMLAND_TRAMPLE.fire(legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level), pos.getX(), pos.getY(), pos.getZ(),
                        EntityBridge.wrap(e.getEntity()), e.getFallDistance());
            });
        }
        if (FAR_DESPAWN.listed()) {
            gameBus.addListener((net.neoforged.neoforge.event.entity.living.MobDespawnEvent e) -> {
                if (!FAR_DESPAWN.active()) return;
                if (Boolean.FALSE.equals(FAR_DESPAWN.replace(Boolean.TRUE, EntityBridge.wrap(e.getEntity())))) {
                    e.setResult(net.neoforged.neoforge.event.entity.living.MobDespawnEvent.Result.DENY);
                }
            });
        }
        if (SQUID_TICK.listed()) {
            gameBus.addListener((net.neoforged.neoforge.event.tick.EntityTickEvent.Post e) -> {
                if (e.getEntity() instanceof net.minecraft.world.entity.animal.Squid squid && SQUID_TICK.active()) SQUID_TICK.fire(EntityBridge.wrap(squid));
            });
        }
        if (BOW_DRAW_TIME.listed()) {
            // the draw time scales the charge the game computed against its 20 ticks
            gameBus.addListener((net.neoforged.neoforge.event.entity.player.ArrowLooseEvent e) -> {
                if (!BOW_DRAW_TIME.active()) return;
                float ticks = ((Number) BOW_DRAW_TIME.modify(0, 20.0F, EntityBridge.wrap(e.getEntity()))).floatValue();
                if (ticks > 0.0F && ticks != 20.0F) e.setCharge(Math.max(0, Math.round(e.getCharge() * 20.0F / ticks)));
            });
        }
        if (IN_BED.listed()) {
            gameBus.addListener((net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent e) -> {
                if (e.mayContinueSleeping() || !(e.getEntity() instanceof net.minecraft.world.entity.player.Player player) || !IN_BED.active()) return;
                if (IN_BED.cancels(EntityBridge.wrap(player))) e.setContinueSleeping(true);
            });
        }
    }

    private static boolean reporting;

    /** Asks for the report line when the server starts (once, whichever version's bridge comes first). */
    public static synchronized void reportOnStart(net.neoforged.bus.api.IEventBus gameBus) {
        if (reporting || LISTED.isEmpty()) return;
        reporting = true;
        gameBus.addListener((net.neoforged.neoforge.event.server.ServerStartingEvent e) -> report());
    }

    /** One line: how many of the listed hooks have a place here. */
    private static void report() {
        if (!ready()) return;
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) Client.RENDER_PRE.listed(); // registers the client places too (their class initialises on first use)
        Map<String, Boolean> replayed = new HashMap<>();
        int sites = 0;
        for (Site s : SITES) {
            if (!s.active()) continue;
            sites++;
            for (Bound b : s.hooks) replayed.put(b.name, true);
        }
        Map<String, Integer> missing = new TreeMap<>();
        int total = 0;
        Map<String, Boolean> seen = new HashMap<>();
        for (List<Listed> list : LISTED.values()) {
            for (Listed l : list) {
                String name = l.owner.replace('/', '.') + "." + l.name + l.desc;
                if (seen.put(l.site + name, true) != null) continue;
                total++;
                if (!replayed.containsKey(name)) missing.merge(l.site, 1, Integer::sum);
            }
        }
        LOG.info("Legacy coremods put {} hook(s) into their Minecraft's classes; {} hook method(s) are replayed at {} place(s) of the modern game, {} place(s) have no "
                + "equivalent yet (features of those mods that depend on them do nothing)", total, replayed.size(), sites, missing.size());
        if ("full".equals(System.getProperty("legacyconnector.coremodHooks.report"))) {
            missing.forEach((site, n) -> LOG.info("  no place yet: {} ({} hook(s): {})", site, n, LISTED.get(site).stream().map(l -> l.owner + "." + l.name).distinct().toList()));
        } else {
            missing.forEach((site, n) -> LOG.debug("  no place yet: {} ({} hook(s))", site, n));
        }
    }

    /** Hooks are mod classes: callable once every mod of the versions with coremods has been constructed (their classes must not be initialised earlier). */
    private static boolean ready() {
        if (ready) return true;
        boolean any = false;
        for (LegacyModRecord rec : LegacyMods.all()) {
            String family = LegacyMods.family(rec.version);
            if (!"1.7.10".equals(family) && !"1.12.2".equals(family)) continue;
            if (rec.mainClass == null) return false;
            any = true;
        }
        return ready = any;
    }

    private static Bound bind(Listed l, Class<?>[] offered, boolean combiner, Class<?> result, String constant) {
        String className = l.owner.replace('/', '.');
        try {
            Class<?> owner = null;
            for (LegacyModRecord rec : LegacyMods.all()) {
                if (rec.mainClass == null) continue;
                try {
                    owner = Class.forName(className, true, rec.mainClass.getClassLoader());
                    break;
                } catch (ClassNotFoundException e) {
                    // another mod's loader
                }
            }
            if (owner == null) {
                LOG.debug("Coremod hook {}.{}: the class is not loaded (its mod is not in this pack any more)", className, l.name);
                return null;
            }
            String[] params = parameters(l.desc);
            Method method = null;
            for (Method m : owner.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers()) || !m.getName().equals(l.name) || m.getParameterCount() != params.length) continue;
                boolean same = true;
                for (int i = 0; i < params.length && same; i++) same = legacyName(m.getParameterTypes()[i]).equals(params[i]);
                if (same) {
                    method = m;
                    break;
                }
            }
            if (method == null) {
                // older versions: a parameter class the forward layer renamed; the only static method of that name and size is the hook
                int n = 0;
                for (Method m : owner.getDeclaredMethods()) {
                    if (Modifier.isStatic(m.getModifiers()) && m.getName().equals(l.name) && m.getParameterCount() == params.length && n++ == 0) method = m;
                }
                if (n != 1) method = null;
            }
            if (method == null) {
                LOG.debug("Coremod hook {}.{}{} was not found in the translated mod", className, l.name, l.desc);
                return null;
            }
            if (result != null && method.getReturnType() != result) {
                LOG.debug("Coremod hook {}.{}{} returns {}, this place wants {}", className, l.name, l.desc, method.getReturnType(), result);
                return null;
            }
            Class<?>[] types = method.getParameterTypes();
            int[] from = new int[types.length];
            Class<?>[] narrowed = new Class<?>[types.length];
            Object[] fixed = new Object[types.length];
            boolean[] taken = new boolean[offered.length];
            for (int i = 0; i < types.length; i++) {
                int pick = -1;
                for (int o = 0; o < offered.length && pick < 0; o++) if (!taken[o] && types[i].isAssignableFrom(offered[o])) pick = o;
                for (int o = 0; o < offered.length && pick < 0; o++) {
                    if (!taken[o] && !types[i].isPrimitive() && offered[o].isAssignableFrom(types[i])) {
                        pick = o;
                        narrowed[i] = types[i];
                    }
                }
                if (pick < 0 && constant != null && types[i].isEnum()) {
                    for (Object c : types[i].getEnumConstants()) if (((Enum<?>) c).name().equals(constant)) fixed[i] = c;
                    if (fixed[i] != null) {
                        from[i] = -1;
                        continue;
                    }
                }
                if (pick < 0) {
                    LOG.debug("Coremod hook {}.{}{} takes a value its place does not offer (parameter {})", className, l.name, l.desc, i + 1);
                    return null;
                }
                taken[pick] = true;
                from[i] = pick;
            }
            method.setAccessible(true);
            MethodHandle handle = MethodHandles.lookup().unreflect(method);
            handle = handle.asType(handle.type().generic().changeReturnType(Object.class)).asSpreader(Object[].class, types.length)
                    .asType(MethodType.methodType(Object.class, Object[].class));
            return new Bound(className + "." + l.name + l.desc, handle, from, narrowed, fixed, combiner, owner.getClassLoader());
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            LOG.warn("Coremod hook {}.{} is not available: {}", className, l.name, e.toString());
            return null;
        }
    }

    /** The 1.7.10 name of a translated parameter type, as descriptors spell it ({@code F}, {@code net/minecraft/entity/Entity}, {@code [I}). */
    private static String legacyName(Class<?> type) {
        if (type.isArray()) return "[" + legacyName(type.getComponentType());
        if (type.isPrimitive()) return MethodType.methodType(type).toMethodDescriptorString().substring(2);
        String name = type.getName();
        if (name.startsWith(SHIM)) name = name.substring(SHIM.length());
        else if (name.startsWith(SYNTH)) name = name.substring(SYNTH.length());
        else if (name.startsWith("legacyconnector.v") || name.startsWith("legacyconnector_synth.v")) name = name.substring(name.indexOf('.', name.indexOf('.') + 1) + 1); // 1.6.4 and older
        return name.replace('.', '/');
    }

    private static String[] parameters(String desc) {
        List<String> out = new ArrayList<>();
        int i = 1;
        while (desc.charAt(i) != ')') {
            int start = i;
            while (desc.charAt(i) == '[') i++;
            if (desc.charAt(i) == 'L') {
                int end = desc.indexOf(';', i);
                out.add(desc.substring(start, i) + desc.substring(i + 1, end));
                i = end + 1;
            } else {
                out.add(desc.substring(start, ++i));
            }
        }
        return out.toArray(new String[0]);
    }

    private static Map<String, List<Listed>> load() {
        Map<String, List<Listed>> out = new HashMap<>();
        String files = System.getProperty("legacyconnector.coremodHooks");
        if (files == null) return out;
        // one list per legacy Minecraft version
        for (String file : files.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))) try {
            Path path = Path.of(file);
            if (!Files.isRegularFile(path)) continue;
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t");
                if (p.length < 4) continue;
                String site = p[0].replaceFirst("@\\d+$", "");
                Listed listed = new Listed(site, p[1], p[2], p[3]);
                out.computeIfAbsent(site, k -> new ArrayList<>()).add(listed);
                EXACT.computeIfAbsent(p[0], k -> new ArrayList<>()).add(listed);
            }
        } catch (java.io.IOException | RuntimeException e) {
            LOG.warn("The coremod hook list {} could not be read: {}", file, e.toString());
        }
        return out;
    }
}
