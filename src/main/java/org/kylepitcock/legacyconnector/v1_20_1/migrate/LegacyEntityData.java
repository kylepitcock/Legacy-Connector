package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.item.ItemStack;

/**
 * Entity data a 1.20.1 mod added to a <b>vanilla</b> entity class with a mixin ({@code SynchedEntityData.defineId(Entity.class, ...)} in a static field and the
 * {@code define} call at the end of {@code Entity.<init>}). In 1.21.1 the data is collected by a builder before the entity exists, and {@code build()} refuses
 * ids that nobody defined. The ids reserved here are recorded, and {@code build()} (see {@code SynchedEntityDataBuilderMixin}) defines the missing ones with the
 * default of their serializer; the mod's later {@code define} call then sets the value it wanted.
 */
public final class LegacyEntityData {
    private record Reserved(Class<?> owner, EntityDataAccessor<?> accessor) {}

    private static final List<Reserved> RESERVED = new CopyOnWriteArrayList<>();

    private LegacyEntityData() {}

    /** {@code SynchedEntityData.defineId}: the real call, recorded when the class is a Minecraft one. */
    public static <T> EntityDataAccessor<T> defineId(Class<? extends net.minecraft.network.syncher.SyncedDataHolder> owner, EntityDataSerializer<T> serializer) {
        EntityDataAccessor<T> accessor = SynchedEntityData.defineId(owner, serializer);
        if (owner.getName().startsWith("net.minecraft.")) RESERVED.add(new Reserved(owner, accessor));
        return accessor;
    }

    /** Called at the start of {@code Builder.build()}: defines the recorded ids of vanilla classes the builder's entity belongs to and that nobody defined. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void fillHoles(Object entity, Object[] itemsById, SynchedEntityData.Builder builder) {
        if (RESERVED.isEmpty() || entity == null) return;
        for (Reserved r : RESERVED) {
            int id = r.accessor().id();
            if (id < 0 || id >= itemsById.length || itemsById[id] != null || !r.owner().isInstance(entity)) continue;
            Object value = defaultOf(r.accessor().serializer());
            if (value == null) continue;
            builder.define((EntityDataAccessor) r.accessor(), value);
        }
    }

    private static Object defaultOf(EntityDataSerializer<?> s) {
        if (s == EntityDataSerializers.BYTE) return (byte) 0;
        if (s == EntityDataSerializers.INT) return 0;
        if (s == EntityDataSerializers.LONG) return 0L;
        if (s == EntityDataSerializers.FLOAT) return 0.0F;
        if (s == EntityDataSerializers.STRING) return "";
        if (s == EntityDataSerializers.BOOLEAN) return false;
        if (s == EntityDataSerializers.COMPONENT) return Component.empty();
        if (s == EntityDataSerializers.OPTIONAL_COMPONENT || s == EntityDataSerializers.OPTIONAL_UUID || s == EntityDataSerializers.OPTIONAL_BLOCK_POS
                || s == EntityDataSerializers.OPTIONAL_BLOCK_STATE || s == EntityDataSerializers.OPTIONAL_UNSIGNED_INT) return Optional.empty();
        if (s == EntityDataSerializers.ITEM_STACK) return ItemStack.EMPTY;
        if (s == EntityDataSerializers.BLOCK_POS) return BlockPos.ZERO;
        if (s == EntityDataSerializers.BLOCK_STATE) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        if (s == EntityDataSerializers.ROTATIONS) return new net.minecraft.core.Rotations(0, 0, 0);
        if (s == EntityDataSerializers.DIRECTION) return net.minecraft.core.Direction.DOWN;
        if (s == EntityDataSerializers.VECTOR3) return new org.joml.Vector3f();
        if (s == EntityDataSerializers.QUATERNION) return new org.joml.Quaternionf();
        if (s == EntityDataSerializers.POSE) return net.minecraft.world.entity.Pose.STANDING;
        if (s == EntityDataSerializers.COMPOUND_TAG) return new net.minecraft.nbt.CompoundTag();
        return null;
    }
}
