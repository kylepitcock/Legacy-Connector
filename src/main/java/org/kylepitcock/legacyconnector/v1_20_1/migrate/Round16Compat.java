package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Failure-class round (hard ATM 9 jars): small Minecraft members of 1.20.1 that 1.21.1 changed or dropped (rows in mc-calls.tsv, mc-bridges.tsv and
 * mc-fields.tsv).
 */
public final class Round16Compat {
    private Round16Compat() {}

    private static final ResourceLocation UNKNOWN_RECIPE_ID = ResourceLocation.withDefaultNamespace("legacy_unknown_recipe");

    // ------------------------------------------------------------------ entity size

    /** {@code Entity.getStandingEyeHeight(pose, dimensions)} (call): 1.21.1 keeps the eye height in the dimensions. */
    public static float standingEyeHeight(Entity entity, net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dimensions) {
        return dimensions.eyeHeight();
    }

    // ------------------------------------------------------------------ portals

    /** {@code Entity.isInsidePortal} (field read, 1.20.1): the entity stands in a portal this tick. */
    public static boolean insidePortal(Entity entity) {
        PortalProcessor p = entity.portalProcess;
        return p != null && p.isInsidePortalThisTick();
    }

    /** {@code entity.isInsidePortal = value} (field write). Only the current portal process can be updated; a process cannot be created without a portal. */
    public static void setInsidePortal(Entity entity, boolean value) {
        PortalProcessor p = entity.portalProcess;
        if (p != null) p.setAsInsidePortalThisTick(value);
    }

    /** {@code Entity.portalTime} (field read, 1.20.1): ticks spent in the portal, 0 outside one. */
    public static int portalTime(Entity entity) {
        PortalProcessor p = entity.portalProcess;
        return p == null ? 0 : p.getPortalTime();
    }

    /** {@code entity.portalTime = value} (field write): 1.21.1 keeps the counter in a process that counts by itself; a write is dropped (APPROXIMATION). */
    public static void setPortalTime(Entity entity, int value) {}

    /**
     * {@code entity.handleInsidePortal(pos)} (call): 1.20.1 told the entity that it stands in a (nether) portal block; 1.21.1 hands the portal over as well.
     */
    public static void handleInsidePortal(Entity entity, BlockPos pos) {
        entity.setAsInsidePortal((Portal) Blocks.NETHER_PORTAL, pos);
    }

    /**
     * {@code super.handleInsidePortal(pos)} of an entity whose parent is Minecraft's: what {@code Entity.setAsInsidePortal} does (the override that the layer
     * bridged calls the mod's method, so the real method cannot be called here without looping).
     */
    public static void superHandleInsidePortal(Object receiver, BlockPos pos) {
        Entity entity = (Entity) receiver;
        if (entity.isOnPortalCooldown()) {
            entity.setPortalCooldown();
            return;
        }
        Portal portal = (Portal) Blocks.NETHER_PORTAL;
        if (entity.portalProcess == null || !entity.portalProcess.isSamePortal(portal)) {
            entity.portalProcess = new PortalProcessor(portal, pos.immutable());
        } else if (!entity.portalProcess.isInsidePortalThisTick()) {
            entity.portalProcess.updateEntryPosition(pos.immutable());
            entity.portalProcess.setAsInsidePortalThisTick(true);
        }
    }

    // ------------------------------------------------------------------ chunks, storage

    /** {@code ChunkHolder.getFullChunk()}: the chunk once it is full, else null. */
    public static LevelChunk fullChunk(ChunkHolder holder) {
        ChunkResult<LevelChunk> result = holder.getFullChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK);
        return result.orElse(null);
    }

    // ------------------------------------------------------------------ projectiles

    /** {@code ThrowableProjectile.getGravity()} (a float in 1.20.1) as 1.21.1's {@code getDefaultGravity()} asks it. */
    public static double gravityOf(float gravity) {
        return gravity;
    }

    /** {@code super.getGravity()} of a throwable projectile: the 1.20.1 default. */
    public static float superThrowableGravity(Object projectile) {
        return 0.03F;
    }

    // ------------------------------------------------------------------ structures

    /** The pool aliases of a jigsaw structure built through the 1.20.1 constructor (there were none). */
    public static java.util.List<net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding> poolAliases(
            net.minecraft.world.level.levelgen.structure.Structure.StructureSettings settings) {
        return java.util.List.of();
    }

    /** The dimension padding of a jigsaw structure built through the 1.20.1 constructor (none). */
    public static net.minecraft.world.level.levelgen.structure.pools.DimensionPadding dimensionPadding(
            net.minecraft.world.level.levelgen.structure.Structure.StructureSettings settings) {
        return net.minecraft.world.level.levelgen.structure.pools.DimensionPadding.ZERO;
    }

    /** The liquid settings of a jigsaw structure built through the 1.20.1 constructor (waterlogging, as 1.20.1 did). */
    public static net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings liquidSettings(
            net.minecraft.world.level.levelgen.structure.Structure.StructureSettings settings) {
        return net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings.APPLY_WATERLOGGING;
    }

    // ------------------------------------------------------------------ skeleton arrows, levers, armor trims, dispensers, enchantment protection

    /** The weapon argument 1.21.1 added to {@code AbstractSkeleton.getArrow}: none known to a 1.20.1 caller. */
    public static net.minecraft.world.item.ItemStack noWeapon(net.minecraft.world.item.ItemStack arrow) {
        return null;
    }

    /** {@code LeverBlock.pull(state, level, pos)} (1.20.1 returned the new state; 1.21.1 takes the pulling player and returns nothing). */
    public static net.minecraft.world.level.block.state.BlockState pullLever(net.minecraft.world.level.block.LeverBlock lever,
            net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, BlockPos pos) {
        lever.pull(state, level, pos, null);
        return level.getBlockState(pos);
    }

    /** {@code ArmorTrim.setTrim(registries, stack, trim)}: the trim is a data component in 1.21.1; trimmable armor only, as 1.20.1 checked. */
    public static boolean setTrim(net.minecraft.core.RegistryAccess registries, net.minecraft.world.item.ItemStack stack,
            net.minecraft.world.item.armortrim.ArmorTrim trim) {
        if (!stack.is(net.minecraft.tags.ItemTags.TRIMMABLE_ARMOR)) return false;
        stack.set(net.minecraft.core.component.DataComponents.TRIM, trim);
        return true;
    }

    private static boolean warnedProtection;

    /**
     * {@code EnchantmentHelper.getDamageProtection(armor, source)}: the protection of 1.20.1 enchantments is data in 1.21.1 and is computed for the victim,
     * which this call never names. APPROXIMATION: 0 (logged once).
     */
    public static int damageProtection(Iterable<net.minecraft.world.item.ItemStack> stacks, net.minecraft.world.damagesource.DamageSource source) {
        if (!warnedProtection) {
            warnedProtection = true;
            org.slf4j.LoggerFactory.getLogger("legacyconnector").warn("EnchantmentHelper.getDamageProtection(Iterable, DamageSource) has no 1.21.1 equivalent without the victim: answering 0 (APPROXIMATION)");
        }
        return 0;
    }

    /**
     * {@code DispenseItemBehavior.setEntityPokingOutOfBlock(source, entity, direction)} (removed in 1.21.1): puts a spawned entity just outside the dispenser
     * face it was dispensed from. APPROXIMATION of the removed vanilla helper (centre of the face, pushed out by half the entity's size).
     */
    public static void setEntityPokingOutOfBlock(net.minecraft.core.dispenser.BlockSource source, Entity entity, net.minecraft.core.Direction direction) {
        BlockPos pos = source.pos();
        double halfWidth = entity.getBbWidth() / 2.0;
        double halfHeight = entity.getBbHeight() / 2.0;
        double x = pos.getX() + 0.5 + direction.getStepX() * (0.5000099999997474 + (direction.getStepX() != 0 ? halfWidth : 0));
        double y = pos.getY() + 0.5 + direction.getStepY() * (0.5000099999997474 + (direction.getStepY() != 0 ? halfHeight : 0)) - halfHeight;
        double z = pos.getZ() + 0.5 + direction.getStepZ() * (0.5000099999997474 + (direction.getStepZ() != 0 ? halfWidth : 0));
        entity.setPos(x, y, z);
    }

    // ------------------------------------------------------------------ trading, spiders, arrows, crossbows, armor wear

    /** {@code MerchantOffer.baseCostA} (field read, an ItemStack in 1.20.1; an ItemCost in 1.21.1). */
    public static net.minecraft.world.item.ItemStack merchantBaseCostA(net.minecraft.world.item.trading.MerchantOffer offer) {
        return offer.getBaseCostA();
    }

    /** {@code MerchantOffer.costB} (field read, an ItemStack in 1.20.1; an optional ItemCost in 1.21.1): empty stack when there is no second cost. */
    public static net.minecraft.world.item.ItemStack merchantCostB(net.minecraft.world.item.trading.MerchantOffer offer) {
        return offer.getCostB();
    }

    /** {@code SpiderEffectsGroupData.effect} (field read, a MobEffect in 1.20.1; a Holder in 1.21.1). */
    public static net.minecraft.world.effect.MobEffect spiderEffect(net.minecraft.world.entity.monster.Spider.SpiderEffectsGroupData data) {
        return data.effect == null ? null : data.effect.value();
    }

    /** {@code data.effect = effect} (field write). */
    public static void setSpiderEffect(net.minecraft.world.entity.monster.Spider.SpiderEffectsGroupData data, net.minecraft.world.effect.MobEffect effect) {
        data.effect = effect == null ? null : net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }

    /**
     * {@code AbstractArrow.setEnchantmentEffectsFromEntity(shooter, velocity)}: 1.21.1 applies the shooter's enchantments when the weapon creates the arrow.
     * APPROXIMATION: only the velocity-based base damage ({@code setBaseDamageFromMob}) of the removed method is applied.
     */
    public static void arrowEnchantmentEffects(net.minecraft.world.entity.projectile.AbstractArrow arrow, net.minecraft.world.entity.LivingEntity shooter, float velocity) {
        arrow.setBaseDamageFromMob(velocity);
    }

    /** {@code CrossbowItem.getStartSound(quickCharge)} (private in 1.20.1, gone in 1.21.1). */
    public static net.minecraft.sounds.SoundEvent crossbowStartSound(net.minecraft.world.item.CrossbowItem item, int quickCharge) {
        return switch (quickCharge) {
            case 1 -> net.minecraft.sounds.SoundEvents.CROSSBOW_QUICK_CHARGE_1.value();
            case 2 -> net.minecraft.sounds.SoundEvents.CROSSBOW_QUICK_CHARGE_2.value();
            case 3 -> net.minecraft.sounds.SoundEvents.CROSSBOW_QUICK_CHARGE_3.value();
            default -> net.minecraft.sounds.SoundEvents.CROSSBOW_LOADING_START.value();
        };
    }

    private static final net.minecraft.world.entity.EquipmentSlot[] ARMOR_SLOTS = {net.minecraft.world.entity.EquipmentSlot.FEET,
            net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.HEAD};

    /** {@code Inventory.hurtArmor(source, damage, slots)} (moved to {@code LivingEntity.doHurtEquipment} in 1.21.1): wears the armor pieces at the inventory indexes. */
    public static void hurtArmor(net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.damagesource.DamageSource source, float damage, int[] slots) {
        if (damage <= 0.0F) return;
        int amount = (int) Math.max(1.0F, damage / 4.0F);
        for (int index : slots) {
            if (index < 0 || index >= ARMOR_SLOTS.length) continue;
            net.minecraft.world.item.ItemStack stack = inventory.armor.get(index);
            if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem && stack.canBeHurtBy(source)) {
                net.minecraft.world.entity.EquipmentSlot slot = ARMOR_SLOTS[index];
                stack.hurtAndBreak(amount, inventory.player, slot);
            }
        }
    }

    // ------------------------------------------------------------------ recipes

    /**
     * The 1.20.1 {@code SingleItemMaker} a serializer was built with, as 1.21.1's recipe factory (no recipe id any more: a placeholder is passed, a recipe
     * that needs its id reads it from the holder).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static SingleItemRecipe.Factory<?> singleItemFactory(LegacySingleItemMaker maker) {
        return (group, ingredient, result) -> (SingleItemRecipe) maker.create(UNKNOWN_RECIPE_ID, group, ingredient, result);
    }
}
