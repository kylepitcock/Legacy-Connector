package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.MappedRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A Forge 1.20.1 mod may construct items and blocks while it is constructed (static initializers such as AE2's {@code AEItems}) and register them
 * later; Forge's registries were open at that time. NeoForge 1.21.1 constructs mods with the item/block registries frozen, where vanilla's
 * {@code createIntrusiveHolder} throws "Registry is already frozen". The holder map still exists (NeoForge keeps it), the registration that follows
 * takes the holder out of it, so only the frozen check is skipped, and only when the construction comes from a translated legacy jar: everything else
 * keeps vanilla's check (the stack is only inspected on the frozen path).
 */
@Mixin(MappedRegistry.class)
public abstract class MappedRegistryIntrusiveHolderMixin {
    @WrapOperation(method = "createIntrusiveHolder", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/MappedRegistry;validateWrite()V"), require = 0)
    private void legacyconnector$validateUnlessLegacyConstruction(MappedRegistry<?> self, Operation<Void> original) {
        if (self.frozen && org.kylepitcock.legacyconnector.util.LegacyCallers.calledFromTranslatedJar()) {
            return;
        }
        original.call(self);
    }
}
