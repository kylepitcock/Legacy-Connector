package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 1.20.1 accepted any object as an argument of a translatable component (it was printed with {@code toString()}: a ResourceLocation, a BlockPos...); 1.21.1
 * throws unless it is a Component, Number, Boolean or String. Other objects are turned into their string, which is what 1.20.1 showed.
 */
@Mixin(TranslatableContents.class)
public abstract class TranslatableArgumentsMixin {
    @ModifyVariable(method = "<init>(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private static Object[] legacyconnector$stringifyArguments(Object[] args) {
        if (args == null) return null;
        Object[] out = null;
        for (int i = 0; i < args.length; i++) {
            Object a = args[i];
            if (a != null && !TranslatableContents.isAllowedPrimitiveArgument(a) && !(a instanceof net.minecraft.network.chat.Component)) {
                if (out == null) out = args.clone();
                out[i] = String.valueOf(a);
            }
        }
        return out == null ? args : out;
    }
}
