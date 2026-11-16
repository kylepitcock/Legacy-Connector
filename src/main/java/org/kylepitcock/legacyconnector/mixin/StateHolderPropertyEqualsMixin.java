package org.kylepitcock.legacyconnector.mixin;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.20.1 looked block state properties up by equality; 1.21.1 looks them up by identity ({@code Reference2ObjectArrayMap}). A mod that declares an equal
 * property twice (a subclass re-declares {@code FACING_TD = DirectionProperty.create("facing", NORTH, EAST)} next to its parent's, and its constructor sets
 * the one the parent's state definition does not contain) failed with "Cannot set property ... as it does not exist". The lookups here fall back to an
 * equal property when the identical one is absent, so vanilla's fast path is unchanged.
 */
@Mixin(StateHolder.class)
public abstract class StateHolderPropertyEqualsMixin {
    @Redirect(method = {"getValue", "getOptionalValue", "setValue", "trySetValue"}, require = 0,
            at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/Reference2ObjectArrayMap;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object legacyconnector$getByEquals(Reference2ObjectArrayMap<Property<?>, Comparable<?>> values, Object property) {
        Object v = values.get(property);
        if (v != null || !(property instanceof Property<?> p)) return v;
        for (Property<?> k : values.keySet()) if (k.equals(p)) return values.get(k);
        return null;
    }

    @Redirect(method = "hasProperty", require = 0,
            at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/Reference2ObjectArrayMap;containsKey(Ljava/lang/Object;)Z"))
    private boolean legacyconnector$hasByEquals(Reference2ObjectArrayMap<Property<?>, Comparable<?>> values, Object property) {
        if (values.containsKey(property)) return true;
        if (!(property instanceof Property<?> p)) return false;
        for (Property<?> k : values.keySet()) if (k.equals(p)) return true;
        return false;
    }
}
