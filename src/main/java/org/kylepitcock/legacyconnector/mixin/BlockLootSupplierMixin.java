package org.kylepitcock.legacyconnector.mixin;

import java.util.function.Supplier;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Forge 1.20.1 kept a block's loot table as a {@code Supplier<ResourceLocation>} ({@code BlockBehaviour(.Properties).lootTableSupplier}); 1.21.1 types it
 * {@code Supplier<ResourceKey<LootTable>>}. Mods set the field through their own mixin accessor or reflection (SuperMartijn642 Core Lib, Integrated
 * Dynamics) and erasure lets the write through; the cast in {@code getLootTable} then failed in the registry freeze with an anonymous "uncaught parallel
 * processing error". A location the supplier returns is wrapped into the loot table key.
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockLootSupplierMixin {
    @WrapOperation(method = "getLootTable", at = @At(value = "INVOKE", target = "Ljava/util/function/Supplier;get()Ljava/lang/Object;"), require = 0)
    private Object legacyconnector$lootKey(Supplier<?> supplier, Operation<Object> original) {
        Object v = original.call(supplier);
        return v instanceof ResourceLocation rl ? ResourceKey.create(Registries.LOOT_TABLE, rl) : v;
    }
}
