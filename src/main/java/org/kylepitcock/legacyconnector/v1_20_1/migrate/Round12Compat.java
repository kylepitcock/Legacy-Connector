package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.io.DataInput;
import java.io.IOException;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;
import java.util.EnumMap;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;

/** Common-side helpers of gap round 12 (1.20.1 mods on 1.21.1). Reached from {@code mc-calls.tsv} and {@code mc-bridges.tsv}. */
public final class Round12Compat {
    private Round12Compat() {}

    // ---- NBT tag types: the nesting depth moved into the NbtAccounter ---------------------------------------------------------------------------

    /** {@code TagType.load(in, depth, accounter)}: 1.21.1 keeps the depth in the accounter, so the argument is dropped. */
    public static Tag tagLoad(TagType<?> type, DataInput in, int depth, NbtAccounter accounter) throws IOException {
        return type.load(in, accounter);
    }

    /** {@code TagType.parse(in, visitor)}: 1.21.1 wants an accounter (no limit: the old call had none). */
    public static StreamTagVisitor.ValueResult tagParse(TagType<?> type, DataInput in, StreamTagVisitor visitor) throws IOException {
        return type.parse(in, visitor, NbtAccounter.unlimitedHeap());
    }

    public static void tagSkipDepth(TagType<?> type, DataInput in, int count) throws IOException {
        type.skip(in, count, NbtAccounter.unlimitedHeap());
    }

    public static void tagSkipAll(TagType<?> type, DataInput in) throws IOException {
        type.skip(in, NbtAccounter.unlimitedHeap());
    }

    /** The depth a bridged {@code TagType.load(in, depth, accounter)} of a mod receives: 1.21.1 callers keep the depth in the accounter, so the mod sees 0. */
    public static int noDepth(NbtAccounter accounter) {
        return 0;
    }

    // ---- brains ------------------------------------------------------------------------------------------------------------------------------

    /** {@code BehaviorUtils.lockGazeAndWalkToEachOther(a, b, speed)}: the walk completion range became a parameter; 1.20.1 used 1. */
    public static void lockGazeAndWalkToEachOther(LivingEntity a, LivingEntity b, float speed) {
        BehaviorUtils.lockGazeAndWalkToEachOther(a, b, speed, 1);
    }

    // ---- enchantment effects of a hit ------------------------------------------------------------------------------------------------------------

    /**
     * {@code Entity.doEnchantDamageEffects(attacker, target)} (1.20.1: the target's armour enchantments hurt the attacker back, the attacker's equipment enchantments
     * act on the target). 1.21.1 has one call for both, {@code EnchantmentHelper.doPostAttackEffects(level, victim, source)}, driven by a damage source: a mob attack
     * of the attacker, or a projectile hit when the caller is a projectile. APPROXIMATION: the damage source is built here, not the one of the hit that happened.
     */
    public static void doEnchantDamageEffects(Entity self, LivingEntity attacker, Entity target) {
        if (attacker == null || target == null || !(self.level() instanceof ServerLevel level)) return;
        DamageSource source = self instanceof Projectile ? self.damageSources().mobProjectile(self, attacker) : self.damageSources().mobAttack(attacker);
        EnchantmentHelper.doPostAttackEffects(level, target, source);
    }

    // ---- armour ------------------------------------------------------------------------------------------------------------------------------

    private static EnumMap<ArmorItem.Type, UUID> armorUuids;

    /**
     * {@code ArmorItem.ARMOR_MODIFIER_UUID_PER_TYPE} (1.20.1; gone in 1.21.1, which identifies armour modifiers by id): the four constants (body armour is new in 1.21.1) of 1.20.1, so mod armour
     * keeps the modifier UUIDs it had in saves and configs.
     */
    public static synchronized EnumMap<ArmorItem.Type, UUID> armorModifierUuids() {
        if (armorUuids == null) {
            EnumMap<ArmorItem.Type, UUID> map = new EnumMap<>(ArmorItem.Type.class);
            map.put(ArmorItem.Type.BOOTS, UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"));
            map.put(ArmorItem.Type.LEGGINGS, UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"));
            map.put(ArmorItem.Type.CHESTPLATE, UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"));
            map.put(ArmorItem.Type.HELMET, UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150"));
            armorUuids = map;
        }
        return armorUuids;
    }

    // ---- entities and items: members the 1.21.1 classes lost -----------------------------------------------------------------------------------

    /** The level of a mob as the {@code ServerLevelAccessor} 1.21.1's spawn-equipment hooks take first ({@code null} on a client, where nothing spawns). */
    public static ServerLevelAccessor levelAccessor(Mob mob) {
        return mob.level() instanceof ServerLevelAccessor accessor ? accessor : null;
    }

    /** The server level of a living entity (the {@code dropAllDeathLoot(source)} of 1.20.1 only ever ran on the server); {@code null} on a client. */
    public static ServerLevel serverLevel(LivingEntity entity) {
        return entity.level() instanceof ServerLevel level ? level : null;
    }

    /** {@code DiggerItem.speed} (1.20.1 protected field: the tier's mining speed, copied in the constructor). */
    public static float diggerSpeed(DiggerItem item) {
        return item.getTier().getSpeed();
    }

    /** {@code ServerPlayer.latency} (1.20.1 field kept up to date by the keep-alive): the connection's measured latency. */
    public static int playerLatency(ServerPlayer player) {
        return player.connection == null ? 0 : player.connection.latency();
    }

    private static VoxelShape hopperInside;

    /** {@code Hopper.INSIDE} (1.20.1 interface constant: the hopper's inner box, {@code Block.box(2, 11, 2, 14, 16, 14)}). */
    public static synchronized VoxelShape hopperInside() {
        if (hopperInside == null) hopperInside = Block.box(2.0D, 11.0D, 2.0D, 14.0D, 16.0D, 14.0D);
        return hopperInside;
    }

    /** {@code BlockSnapshot.restore(force)} (Forge 1.20.1): neighbours are notified, as {@code restore(force, true)} did. */
    public static boolean restoreSnapshot(BlockSnapshot snapshot, boolean force) {
        return Round8Compat.restoreSnapshot(snapshot, force, true);
    }
}
