package legacyconnector.v1_20_1;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

/**
 * The long tail of small Forge 1.20.1 event and hook methods whose NeoForge 1.21.1 form changed shape (a float became a
 * {@code DeltaTracker}, a getter was renamed, a value moved into the registry-aware overload). Reached from
 * {@code forward/1.20.1/forge-call-rewrites.tsv} with a {@code Tail.} prefix on the helper name; instance calls get the receiver first.
 */
public final class ForgeApiTail1201 {
    private ForgeApiTail1201() {}

    // ---- living / player events

    /** Looting is an enchantment effect on the damage source's weapon now; the event no longer carries a level. */
    public static int lootingLevel(LivingDropsEvent event) {
        return 0;
    }

    public static void criticalSetDamageModifier(CriticalHitEvent event, float modifier) {
        event.setDamageMultiplier(modifier);
    }

    public static float criticalGetDamageModifier(CriticalHitEvent event) {
        return event.getDamageMultiplier();
    }

    public static void changeTargetSet(LivingChangeTargetEvent event, LivingEntity target) {
        event.setNewAboutToBeSetTarget(target);
    }

    public static LivingEntity changeTargetGet(LivingChangeTargetEvent event) {
        return event.getNewAboutToBeSetTarget();
    }

    public static MobEffect removeEffect(MobEffectEvent.Remove event) {
        return event.getEffect().value();
    }

    public static BlockState bonemealBlock(BonemealEvent event) {
        return event.getState();
    }

    /** The eye height is part of the entity dimensions now. */
    public static void sizeSetNewEyeHeight(EntityEvent.Size event, float eyeHeight) {
        EntityDimensions size = event.getNewSize();
        if (size != null) event.setNewSize(size.withEyeHeight(eyeHeight));
    }

    public static float sizeGetNewEyeHeight(EntityEvent.Size event) {
        EntityDimensions size = event.getNewSize();
        return size == null ? 0.0F : size.eyeHeight();
    }

    // ---- energy

    public static Tag energySerialize(EnergyStorage storage) {
        return storage.serializeNBT(RegistryAccess.EMPTY);
    }

    public static void energyDeserialize(EnergyStorage storage, Tag tag) {
        storage.deserializeNBT(RegistryAccess.EMPTY, tag);
    }

    // ---- items

    /** Forge's relaxed stacking: same item and same data. */
    public static boolean canItemStacksStackRelaxed(net.minecraft.world.item.ItemStack a, net.minecraft.world.item.ItemStack b) {
        if (a.isEmpty() || b.isEmpty() || !a.isStackable()) return false;
        return net.minecraft.world.item.ItemStack.isSameItemSameComponents(a, b);
    }

    public static net.minecraft.world.level.material.Fluid fluidGetRawFluid(net.neoforged.neoforge.fluids.FluidStack stack) {
        return stack.getFluid();
    }

    // ---- interaction events

    private static legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result fromTri(net.neoforged.neoforge.common.util.TriState s) {
        return s == net.neoforged.neoforge.common.util.TriState.TRUE ? legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result.ALLOW
                : s == net.neoforged.neoforge.common.util.TriState.FALSE ? legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result.DENY
                : legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result.DEFAULT;
    }

    private static net.neoforged.neoforge.common.util.TriState toTri(legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result r) {
        return r == legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result.ALLOW ? net.neoforged.neoforge.common.util.TriState.TRUE
                : r == legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result.DENY ? net.neoforged.neoforge.common.util.TriState.FALSE
                : net.neoforged.neoforge.common.util.TriState.DEFAULT;
    }

    public static legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result leftClickGetUseBlock(
            net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock e) {
        return fromTri(e.getUseBlock());
    }

    public static legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result leftClickGetUseItem(
            net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock e) {
        return fromTri(e.getUseItem());
    }

    public static void leftClickSetUseBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock e,
            legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result r) {
        e.setUseBlock(toTri(r));
    }

    public static void leftClickSetUseItem(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock e,
            legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result r) {
        e.setUseItem(toTri(r));
    }

    /** Forge's base event carried the cancellation result; only the right-click events do now (for the others the result is the vanilla default). */
    public static void interactSetCancellationResult(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent e, net.minecraft.world.InteractionResult result) {
        if (e instanceof net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock b) b.setCancellationResult(result);
        else if (e instanceof net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem i) i.setCancellationResult(result);
        else if (e instanceof net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract x) x.setCancellationResult(result);
    }

    // ---- attributes

    private static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> holder(net.minecraft.world.entity.ai.attributes.Attribute a) {
        return net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.wrapAsHolder(a);
    }

    @SuppressWarnings("unchecked")
    public static void attributeAdd(net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent e, net.minecraft.world.entity.EntityType<?> type,
            net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        e.add((net.minecraft.world.entity.EntityType<? extends LivingEntity>) type, holder(attribute));
    }

    @SuppressWarnings("unchecked")
    public static void attributeAddValue(net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent e, net.minecraft.world.entity.EntityType<?> type,
            net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        e.add((net.minecraft.world.entity.EntityType<? extends LivingEntity>) type, holder(attribute), value);
    }

    @SuppressWarnings("unchecked")
    public static boolean attributeHas(net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent e, net.minecraft.world.entity.EntityType<?> type,
            net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        return e.has((net.minecraft.world.entity.EntityType<? extends LivingEntity>) type, holder(attribute));
    }

    public static void interactSetCancellationResult(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock e,
            net.minecraft.world.InteractionResult result) {
    }

    public static void interactSetCancellationResult(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickEmpty e,
            net.minecraft.world.InteractionResult result) {
    }

    // ---- fml event bus subscriber: Bus.bus() was a supplier of the bus

    public static java.util.function.Supplier<net.neoforged.bus.api.IEventBus> busBus(net.neoforged.fml.common.EventBusSubscriber.Bus bus) {
        if (bus == net.neoforged.fml.common.EventBusSubscriber.Bus.MOD) {
            return () -> net.neoforged.fml.ModLoadingContext.get().getActiveContainer().getEventBus();
        }
        return () -> net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
    }

    // ---- block break experience: no longer part of the event (it is a drop now); the value is kept per event so a handler reads back what it set

    private static final java.util.Map<Object, Integer> EXP = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public static int breakGetExpToDrop(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent e) {
        return EXP.getOrDefault(e, 0);
    }

    public static void breakSetExpToDrop(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent e, int exp) {
        EXP.put(e, exp);
    }

    public static int commonHooksLootingLevel(net.minecraft.world.entity.Entity target, net.minecraft.world.entity.Entity killer,
            net.minecraft.world.damagesource.DamageSource source) {
        return 0;
    }

    public static net.minecraft.core.Registry<?> registerEventVanillaRegistry(net.neoforged.neoforge.registries.RegisterEvent e) {
        return e.getRegistry();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static net.neoforged.neoforge.registries.DeferredHolder holderCreateWithNamespace(net.minecraft.resources.ResourceLocation name,
            net.minecraft.resources.ResourceKey registry, String modid) {
        return net.neoforged.neoforge.registries.DeferredHolder.create(registry, name);
    }

    /** {@code NewRegistryEvent.create(builder, onCreate)}: the callback gets the registry as soon as it exists (registries are bound at once here). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static java.util.function.Supplier newRegistryCreateCallback(net.neoforged.neoforge.registries.NewRegistryEvent event,
            legacyconnector.v1_20_1.net.minecraftforge.registries.RegistryBuilder builder, java.util.function.Consumer onCreate) {
        java.util.function.Supplier supplier = ForgeApi1201.newRegistryCreate(event, builder);
        if (onCreate != null) onCreate.accept(supplier.get());
        return supplier;
    }

    // ---- spawning, bone meal, buckets, brewing

    public static net.minecraft.world.entity.SpawnGroupData onFinalizeSpawn(net.minecraft.world.entity.Mob mob, net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType type, net.minecraft.world.entity.SpawnGroupData data,
            net.minecraft.nbt.CompoundTag tag) {
        return net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(mob, level, difficulty, type, data);
    }

    /** Forge's result: -1 cancelled, 1 bone meal used up, 0 nothing special. */
    public static int onApplyBonemeal(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.item.ItemStack stack) {
        var event = net.neoforged.neoforge.event.EventHooks.fireBonemealEvent(player, level, pos, state, stack);
        if (event.isCanceled()) return -1;
        if (event.isSuccessful()) {
            stack.shrink(1);
            return 1;
        }
        return 0;
    }

    /** Forge's bucket event could replace the result; no handler means null (the vanilla use goes on). */
    public static net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> onBucketUse(net.minecraft.world.entity.player.Player player,
            net.minecraft.world.level.Level level, net.minecraft.world.item.ItemStack stack, net.minecraft.world.phys.HitResult target) {
        return null;
    }

    /** Brewing recipes are registered through RegisterBrewingRecipesEvent now; recipes added the old way are accepted and not brewed. */
    public static boolean brewAddRecipe(net.minecraft.world.item.crafting.Ingredient ingredient, net.minecraft.world.item.crafting.Ingredient input, net.minecraft.world.item.ItemStack output) {
        return true;
    }

    public static java.util.List brewGetRecipes() {
        return new java.util.ArrayList<>();
    }

    // ---- item pickup, sleeping and sapling growth hooks of Forge's ForgeEventFactory

    /** Forge's result of the pickup event: -1 denied, 1 forced, 0 the vanilla rule. */
    public static int onItemPickup(net.minecraft.world.entity.item.ItemEntity item, net.minecraft.world.entity.player.Player player) {
        var pre = net.neoforged.neoforge.event.EventHooks.fireItemPickupPre(item, player);
        return switch (pre.canPickup()) {
            case TRUE -> 1;
            case FALSE -> -1;
            default -> 0;
        };
    }

    public static void firePlayerItemPickupEvent(net.minecraft.world.entity.player.Player player, net.minecraft.world.entity.item.ItemEntity item,
            net.minecraft.world.item.ItemStack copy) {
        net.neoforged.neoforge.event.EventHooks.fireItemPickupPost(item, player, copy);
    }

    /** Forge's sleeping time check: an event could allow or deny; without one sleeping is possible at night. 1.21.1 has no such event, so the vanilla rule decides. */
    public static boolean fireSleepingTimeCheck(net.minecraft.world.entity.player.Player player, java.util.Optional<net.minecraft.core.BlockPos> sleepingLocation) {
        return !player.level().isDay();
    }

    /** Forge's sleep-in-bed event: a problem that stops the sleep, or null. NeoForge's {@code CanPlayerSleepEvent} answers it. */
    public static net.minecraft.world.entity.player.Player.BedSleepingProblem onPlayerSleepInBed(net.minecraft.world.entity.player.Player player,
            java.util.Optional<net.minecraft.core.BlockPos> pos) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp && pos.isPresent()) {
            return net.neoforged.neoforge.event.EventHooks.canPlayerStartSleeping(sp, pos.get(),
                    com.mojang.datafixers.util.Either.right(net.minecraft.util.Unit.INSTANCE)).left().orElse(null);
        }
        return null;
    }

    /** Forge's sapling growth event, fired through NeoForge's block grow feature event; a cancellation shows as the DENY result the old API had. */
    public static legacyconnector.v1_20_1.net.minecraftforge.event.level.SaplingGrowTreeEvent fireBlockGrowFeature(net.minecraft.world.level.LevelAccessor level,
            net.minecraft.util.RandomSource random, net.minecraft.core.BlockPos pos, net.minecraft.core.Holder<?> feature) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        var real = net.neoforged.neoforge.event.EventHooks.fireBlockGrowFeature(level, random, pos, (net.minecraft.core.Holder) feature);
        var event = new legacyconnector.v1_20_1.net.minecraftforge.event.level.SaplingGrowTreeEvent(level, random, pos);
        if (real.isCanceled()) ForgeApi1201.eventSetResult(event, legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result.DENY);
        return event;
    }
}
