package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * 1.20.1 looked block state properties up by equality; 1.21.1 looks them up by identity ({@code Reference2ObjectArrayMap}). A mod that declares an equal
 * property twice (a subclass re-declares {@code FACING_TD = DirectionProperty.create("facing", NORTH, EAST)} next to its parent's, and its constructor sets
 * the one the parent's state definition does not contain) failed with "Cannot set property ... as it does not exist". The hooks here sit on the paths
 * where the identical property was not found and retry with an equal one, so vanilla's fast path is unchanged.
 *
 * They never name the type of the {@code values} map: FerriteCore retypes that field to {@code Reference2ObjectMap}, and a redirect handler taking the
 * array map failed verification at bootstrap ("Bad type on operand stack" in {@code StateHolder.getValue}) in every pack that has it.
 */
@Mixin(StateHolder.class)
public abstract class StateHolderPropertyEqualsMixin {
    /** The property of this holder that equals the given one without being it, or null (also when the identical one is present). */
    @SuppressWarnings("rawtypes")
    private Property legacyconnector$equalProperty(Property<?> property) {
        if (property == null) return null;
        Property found = null;
        for (Property<?> k : ((StateHolder<?, ?>) (Object) this).getProperties()) {
            if (k == property) return null;
            if (found == null && k.equals(property)) found = k;
        }
        return found;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "getValue", require = 0, cancellable = true, at = @At(value = "NEW", target = "java/lang/IllegalArgumentException"))
    private void legacyconnector$getByEquals(Property property, CallbackInfoReturnable<Comparable> cir) {
        Property k = legacyconnector$equalProperty(property);
        if (k != null) cir.setReturnValue(((StateHolder) (Object) this).getValue(k));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "setValue", require = 0, cancellable = true, at = @At(value = "NEW", target = "java/lang/IllegalArgumentException"))
    private void legacyconnector$setByEquals(Property property, Comparable value, CallbackInfoReturnable<Object> cir) {
        Property k = legacyconnector$equalProperty(property);
        if (k != null) cir.setReturnValue(((StateHolder) (Object) this).setValue(k, value));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "getOptionalValue", require = 0, cancellable = true, at = @At("RETURN"))
    private void legacyconnector$getOptionalByEquals(Property property, CallbackInfoReturnable<Optional> cir) {
        Optional r = cir.getReturnValue();
        if (r != null && r.isPresent()) return;
        Property k = legacyconnector$equalProperty(property);
        if (k != null) cir.setReturnValue(((StateHolder) (Object) this).getOptionalValue(k));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "trySetValue", require = 0, cancellable = true, at = @At("RETURN"))
    private void legacyconnector$trySetByEquals(Property property, Comparable value, CallbackInfoReturnable<Object> cir) {
        if (cir.getReturnValue() != (Object) this) return;
        Property k = legacyconnector$equalProperty(property);
        if (k != null) cir.setReturnValue(((StateHolder) (Object) this).trySetValue(k, value));
    }

    @Inject(method = "hasProperty", require = 0, cancellable = true, at = @At("RETURN"))
    private void legacyconnector$hasByEquals(Property<?> property, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && legacyconnector$equalProperty(property) != null) cir.setReturnValue(true);
    }
}
