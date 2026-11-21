package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Helpers of 1.20.1 calls that 1.21.1 dropped (ATM 9 ranking round 13, 2026-11-21). */
public final class Round13Compat {
    private Round13Compat() {}

    // ------------------------------------------------------------------ crossbow mobs

    /**
     * 1.20.1's default {@code CrossbowAttackMob.shootCrossbowProjectile(target, crossbow, projectile, angle)}: aims the projectile at the target
     * from the mob with vanilla's velocity 1.6 (1.21.1 moved the aiming into {@code CrossbowItem}).
     */
    public static void shootCrossbowProjectile(CrossbowAttackMob mob, LivingEntity target, ItemStack crossbow, Projectile projectile, float angle) {
        if (mob instanceof LivingEntity user) shootCrossbowProjectile(user, target, projectile, angle, 1.6F);
    }

    /** The default instance method {@code shootCrossbowProjectile(user, target, projectile, angle, velocity)} called on a mob. */
    public static void shootCrossbowProjectile(CrossbowAttackMob mob, LivingEntity user, LivingEntity target, Projectile projectile, float angle, float velocity) {
        shootCrossbowProjectile(user, target, projectile, angle, velocity);
    }

    /** 1.20.1's {@code shootCrossbowProjectile(user, target, projectile, angle, velocity)}: aim with a little arc, inaccuracy by difficulty, shot sound. */
    public static void shootCrossbowProjectile(LivingEntity user, LivingEntity target, Projectile projectile, float angle, float velocity) {
        if (user == null || target == null || projectile == null) return;
        double dx = target.getX() - user.getX();
        double dz = target.getZ() - user.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double dy = target.getY(0.3333333333333333D) - projectile.getY() + horizontal * 0.2F;
        Vector3f dir = shotVector(user, new Vec3(dx, dy, dz), angle);
        projectile.shoot(dir.x(), dir.y(), dir.z(), velocity, 14 - user.level().getDifficulty().getId() * 4);
        user.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F / (user.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    /** 1.20.1's {@code CrossbowAttackMob.getProjectileShotVector}: the direction turned by {@code angle} degrees around the shooter's up axis. */
    static Vector3f shotVector(LivingEntity user, Vec3 direction, float angle) {
        Vector3f dir = direction.normalize().toVector3f();
        Vector3f side = new Vector3f(dir).cross(new Vector3f(0.0F, 1.0F, 0.0F));
        if (side.lengthSquared() <= 1.0E-7D) side = new Vector3f(dir).cross(user.getUpVector(1.0F).toVector3f());
        Vector3f up = new Vector3f(dir).rotateAxis((float) (Math.PI / 2), side.x(), side.y(), side.z());
        return new Vector3f(dir).rotateAxis(angle * ((float) Math.PI / 180F), up.x(), up.y(), up.z());
    }

    // ------------------------------------------------------------------ recipes

    /**
     * {@code RecipeManager.getRecipeFor(type, container, level, lastRecipe)}: 1.20.1 answered the id and the recipe as a pair (the id tells a caching
     * caller which recipe matched last time); 1.21.1 answers the holder.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Optional getRecipeFor(RecipeManager manager, RecipeType type, Container container, Level level, ResourceLocation last) {
        Optional<RecipeHolder> found = manager.getRecipeFor(type, BridgeHelpers.input(container), level, last);
        return found.map(h -> com.mojang.datafixers.util.Pair.of(h.id(), h.value()));
    }

    /** Forge 1.20.1's {@code Ingredient.of(item, nbt)} (partial NBT match): the item with at least that custom data. */
    public static Ingredient partialIngredient(ItemLike item, net.minecraft.nbt.CompoundTag nbt) {
        return RoundCompat.partialIngredient(nbt, item);
    }

    // ------------------------------------------------------------------ blocks, items, entities

    /** {@code Explosion.getExploder()}: the entity that exploded (1.21.1 names it the direct source). */
    public static Entity getExploder(Explosion explosion) {
        return explosion.getDirectSourceEntity();
    }

    /** {@code CompassItem.isLodestoneCompass(stack)}: the compass tracks a lodestone (a component in 1.21.1). */
    public static boolean isLodestoneCompass(ItemStack stack) {
        return stack != null && stack.has(DataComponents.LODESTONE_TRACKER);
    }

    /** {@code Item.canBeHurtBy(source)}: fire-resistant items survive fire and lava (1.21.1 asks the stack's damage resistance). */
    public static boolean canBeHurtBy(Item item, DamageSource source) {
        return new ItemStack(item).canBeHurtBy(source);
    }

    /** {@code ButtonBlock.press(state, level, pos)}: pressed without a player (1.21.1 passes the presser for the game event). */
    public static void pressButton(ButtonBlock button, BlockState state, Level level, BlockPos pos) {
        button.press(state, level, pos, null);
    }

    /** {@code BaseSpawner.getSpawnerBlockEntity()}: the spawner's block entity, null for a spawner minecart. */
    public static BlockEntity getSpawnerBlockEntity(BaseSpawner spawner) {
        var owner = spawner.getOwner();
        return owner == null ? null : owner.left().orElse(null);
    }

    /** {@code new BeaconBeamSection(float[] rgb)}: the colour as one RGB int in 1.21.1. */
    public static BeaconBlockEntity.BeaconBeamSection newBeamSection(float[] rgb) {
        return new BeaconBlockEntity.BeaconBeamSection(rgbToInt(rgb));
    }

    /** {@code BeaconBeamSection.getColor()}: 1.20.1 answered red, green, blue as 0-1 floats. */
    public static float[] beamColor(BeaconBlockEntity.BeaconBeamSection section) {
        int c = section.getColor();
        return new float[] {((c >> 16) & 0xFF) / 255.0F, ((c >> 8) & 0xFF) / 255.0F, (c & 0xFF) / 255.0F};
    }

    /** {@code new ItemInput(item, nbt)} (command item arguments): the tag becomes the stack's components as {@link ItemStackNbt#setTag} reads it. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static net.minecraft.commands.arguments.item.ItemInput newItemInput(net.minecraft.core.Holder item, net.minecraft.nbt.CompoundTag nbt) {
        ItemStack stack = new ItemStack((net.minecraft.core.Holder<Item>) item);
        if (nbt != null && !stack.isEmpty()) ItemStackNbt.setTag(stack, nbt);
        return new net.minecraft.commands.arguments.item.ItemInput((net.minecraft.core.Holder<Item>) item, stack.getComponentsPatch());
    }

    static int rgbToInt(float[] rgb) {
        if (rgb == null || rgb.length < 3) return 0xFFFFFFFF;
        int r = Math.round(Math.max(0.0F, Math.min(1.0F, rgb[0])) * 255.0F);
        int g = Math.round(Math.max(0.0F, Math.min(1.0F, rgb[1])) * 255.0F);
        int b = Math.round(Math.max(0.0F, Math.min(1.0F, rgb[2])) * 255.0F);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
