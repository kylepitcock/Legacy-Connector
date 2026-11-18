package org.kylepitcock.legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.ItemStackNbt;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.MigrateContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LegacyItemStackNbt1201Test {
    @BeforeAll
    static void bootstrapMinecraft() throws Exception {
        var dist = net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");
        dist.setAccessible(true);
        dist.set(null, net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(
                java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @BeforeEach
    void provideRegistryContext() {
        // Unit tests have no running server/client from which MigrateContext can obtain registries.
        MigrateContext.remember(RegistryAccess.EMPTY);
    }

    @Test
    void absentCustomDataMatchesLegacyNullTagBehavior() {
        ItemStack stack = new ItemStack(Items.STONE);

        assertFalse(LegacyItemStackNbt1201.hasNBT(stack));
        assertNull(LegacyItemStackNbt1201.getNbt(stack));
    }

    @Test
    void setNbtStoresTheCustomDataComponentAndCopiesTheInput() {
        ItemStack stack = new ItemStack(Items.STONE);
        CompoundTag input = new CompoundTag();
        input.putString("storedGlowColor1", "4b73d4");

        LegacyItemStackNbt1201.setNbt(stack, input);
        input.putString("storedGlowColor1", "external-mutation");

        assertTrue(LegacyItemStackNbt1201.hasNBT(stack));
        assertNotNull(stack.get(DataComponents.CUSTOM_DATA));
        assertEquals("4b73d4", LegacyItemStackNbt1201.getNbt(stack).getString("storedGlowColor1"));
    }

    @Test
    void getNbtRetainsLegacyLiveCompoundMutationBehavior() {
        ItemStack stack = new ItemStack(Items.STONE);
        CompoundTag input = new CompoundTag();
        input.putString("storedCode", "000000");
        LegacyItemStackNbt1201.setNbt(stack, input);

        CompoundTag liveTag = LegacyItemStackNbt1201.getNbt(stack);
        liveTag.putString("storedCode", "123456");

        assertEquals("123456", LegacyItemStackNbt1201.getNbt(stack).getString("storedCode"));
    }

    @Test
    void bucketTagReconstructsLegacyRootFromComponentsWithoutMutatingStack() {
        ItemStack stack = new ItemStack(Items.COD_BUCKET);

        CompoundTag residual = new CompoundTag();
        residual.putString("moddedRootKey", "kept");
        residual.putInt("Age", 2);
        CompoundTag staleEntityTag = new CompoundTag();
        staleEntityTag.putString("stale", "must be replaced");
        residual.put("EntityTag", staleEntityTag);
        CompoundTag display = new CompoundTag();
        display.putString("LoreMarker", "preserved");
        display.putString("Name", "{\"text\":\"stale name\"}");
        residual.put("display", display);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(residual));

        CompoundTag bucketData = new CompoundTag();
        bucketData.putInt("Age", 25);
        bucketData.putInt("BucketVariantTag", 1234);
        bucketData.putString("bucketModKey", "component");
        stack.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(bucketData));

        CompoundTag entityData = new CompoundTag();
        entityData.putString("id", "minecraft:cod");
        entityData.putString("componentOnly", "entity data");
        stack.set(DataComponents.ENTITY_DATA, CustomData.of(entityData));

        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Bucket Cod"));

        CompoundTag legacy = ItemStackNbt.getBucketTag(stack, RegistryAccess.EMPTY);

        assertEquals("kept", legacy.getString("moddedRootKey"));
        assertEquals(25, legacy.getInt("Age"));
        assertEquals(1234, legacy.getInt("BucketVariantTag"));
        assertEquals("component", legacy.getString("bucketModKey"));
        assertEquals("minecraft:cod", legacy.getCompound("EntityTag").getString("id"));
        assertEquals("entity data", legacy.getCompound("EntityTag").getString("componentOnly"));
        assertFalse(legacy.getCompound("EntityTag").contains("stale"));
        assertEquals("preserved", legacy.getCompound("display").getString("LoreMarker"));
        assertTrue(legacy.getCompound("display").getString("Name").contains("Bucket Cod"));

        // Projection is read-only: neither source CUSTOM_DATA nor dedicated components are changed.
        assertEquals(2, stack.get(DataComponents.CUSTOM_DATA).copyTag().getInt("Age"));
        assertEquals(25, stack.get(DataComponents.BUCKET_ENTITY_DATA).copyTag().getInt("Age"));
        assertEquals("must be replaced", stack.get(DataComponents.CUSTOM_DATA).copyTag()
                .getCompound("EntityTag").getString("stale"));
    }

    @Test
    void spawnTagRetainsNullSemanticsWhenNoLegacyRootDataExists() {
        ItemStack plain = new ItemStack(Items.STONE);
        assertNull(ItemStackNbt.getSpawnTag(plain, RegistryAccess.EMPTY));

        ItemStack ordinaryName = new ItemStack(Items.STONE);
        ordinaryName.set(DataComponents.ITEM_NAME, Component.literal("not a legacy custom name"));
        assertNull(ItemStackNbt.getSpawnTag(ordinaryName, RegistryAccess.EMPTY));
    }

    @Test
    void spawnTagIsPresentWhenOnlyDedicatedEntityComponentsExist() {
        ItemStack bucketOnly = new ItemStack(Items.COD_BUCKET);
        CompoundTag bucket = new CompoundTag();
        bucket.putInt("Age", 42);
        bucketOnly.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(bucket));
        CompoundTag projectedBucket = ItemStackNbt.getSpawnTag(bucketOnly, RegistryAccess.EMPTY);
        assertNotNull(projectedBucket);
        assertEquals(42, projectedBucket.getInt("Age"));

        ItemStack entityOnly = new ItemStack(Items.COD_BUCKET);
        CompoundTag entity = new CompoundTag();
        entity.putString("id", "minecraft:cod");
        entityOnly.set(DataComponents.ENTITY_DATA, CustomData.of(entity));
        CompoundTag projectedEntity = ItemStackNbt.getSpawnTag(entityOnly, RegistryAccess.EMPTY);
        assertNotNull(projectedEntity);
        assertEquals("minecraft:cod", projectedEntity.getCompound("EntityTag").getString("id"));
    }

    @Test
    void spawnTagProjectsBucketEntityDataEntityDataAndLegacyRootWithoutMutatingStack() {
        ItemStack stack = new ItemStack(Items.COD_BUCKET);

        CompoundTag residual = new CompoundTag();
        residual.putString("spawnRootKey", "retained");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(residual));

        CompoundTag bucket = new CompoundTag();
        bucket.putInt("Age", 12);
        bucket.putString("bucketOnly", "flattened");
        stack.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(bucket));

        CompoundTag entity = new CompoundTag();
        entity.putString("id", "minecraft:cod");
        entity.putInt("Variant", 7);
        stack.set(DataComponents.ENTITY_DATA, CustomData.of(entity));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Named Cod"));

        CompoundTag legacy = ItemStackNbt.getSpawnTag(stack, RegistryAccess.EMPTY);

        assertNotNull(legacy);
        assertEquals("retained", legacy.getString("spawnRootKey"));
        assertEquals(12, legacy.getInt("Age"));
        assertEquals("flattened", legacy.getString("bucketOnly"));
        assertEquals("minecraft:cod", legacy.getCompound("EntityTag").getString("id"));
        assertEquals(7, legacy.getCompound("EntityTag").getInt("Variant"));
        assertTrue(legacy.getCompound("display").getString("Name").contains("Named Cod"));

        // The projection is detached and all source components remain untouched.
        legacy.putString("spawnRootKey", "mutated projection");
        assertEquals("retained", stack.get(DataComponents.CUSTOM_DATA).copyTag().getString("spawnRootKey"));
        assertEquals(12, stack.get(DataComponents.BUCKET_ENTITY_DATA).copyTag().getInt("Age"));
        assertEquals("minecraft:cod", stack.get(DataComponents.ENTITY_DATA).copyTag().getString("id"));
    }

    @Test
    void bucketTagPreservesLegacyBucketFieldsInResidualCustomData() {
        ItemStack stack = new ItemStack(Items.AXOLOTL_BUCKET);
        CompoundTag residual = new CompoundTag();
        residual.putInt("Variant", 3);
        residual.putInt("Age", -24000);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(residual));

        CompoundTag legacy = ItemStackNbt.getBucketTag(stack, RegistryAccess.EMPTY);

        assertEquals(3, legacy.getInt("Variant"));
        assertEquals(-24000, legacy.getInt("Age"));
        assertEquals(3, stack.get(DataComponents.CUSTOM_DATA).copyTag().getInt("Variant"));
        assertEquals(-24000, stack.get(DataComponents.CUSTOM_DATA).copyTag().getInt("Age"));
    }

    @Test
    void bucketTagDoesNotTurnItemNameIntoLegacyCustomName() {
        ItemStack stack = new ItemStack(Items.COD_BUCKET);
        stack.set(DataComponents.ITEM_NAME, Component.literal("ordinary item name"));

        CompoundTag legacy = ItemStackNbt.getBucketTag(stack, RegistryAccess.EMPTY);

        assertFalse(legacy.contains("display"));
    }

    @Test
    void assigningNullRemovesTheCustomDataComponent() {
        ItemStack stack = new ItemStack(Items.STONE);
        CompoundTag input = new CompoundTag();
        input.putInt("test", 1);
        LegacyItemStackNbt1201.setNbt(stack, input);
        assertTrue(LegacyItemStackNbt1201.hasNBT(stack));

        LegacyItemStackNbt1201.setNbt(stack, null);

        assertFalse(LegacyItemStackNbt1201.hasNBT(stack));
        assertNull(stack.get(DataComponents.CUSTOM_DATA));
        assertNull(LegacyItemStackNbt1201.getNbt(stack));
    }
}
