package org.kylepitcock.legacyconnector.mixin;

import com.google.common.cache.LoadingCache;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.DataResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla's {@code EncoderCache} (enchantments, lore, tool rules... : every component registered with {@code cacheEncoding()}) keeps the result of an
 * encode by (codec, value, ops), a FAILED result included. NeoForge makes holders of the same key equal across registries, and the ops of one
 * registry access are equal, so one failed encode of a stack that still carried a holder of another registry set (an earlier world of the same
 * game session, the client's registries) is served again for every later stack with the same enchantments of the right registry set: the save of
 * a plain, valid enchanted hoe then failed with "Element ... is not valid in current registry set" (issue 113, after the second world load). A failure
 * is never kept, so the next encode looks at the holders again.
 */
@Mixin(targets = "net.minecraft.util.EncoderCache$2")
public abstract class EncoderCacheErrorsMixin {
    @WrapOperation(method = "encode", at = @At(value = "INVOKE", target = "Lcom/google/common/cache/LoadingCache;getUnchecked(Ljava/lang/Object;)Ljava/lang/Object;"), require = 0)
    private Object legacyconnector$doNotKeepFailures(LoadingCache<Object, Object> cache, Object key, Operation<Object> original) {
        Object result = original.call(cache, key);
        if (result instanceof DataResult<?> r && r.isError()) cache.invalidate(key);
        return result;
    }
}
