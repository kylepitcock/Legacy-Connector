package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.MappedRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A Forge 1.20.1 mod may construct items and blocks while it is constructed (static initializers such as AE2's {@code AEItems}) and register them
 * later; Forge's registries were open at that time. NeoForge 1.21.1 constructs mods with the item/block registries frozen, where vanilla's
 * {@code createIntrusiveHolder} throws "Registry is already frozen". The holder map still exists (NeoForge keeps it), the registration that follows
 * takes the holder out of it, so only the frozen check is skipped; an object that is never registered still fails at the next freeze as before.
 */
@Mixin(MappedRegistry.class)
public abstract class MappedRegistryIntrusiveHolderMixin {
    @Redirect(method = "createIntrusiveHolder", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/MappedRegistry;validateWrite()V"), require = 0)
    private void legacyconnector$allowFrozenIntrusiveHolder(MappedRegistry<?> self) {
        // intentionally empty
    }
}
