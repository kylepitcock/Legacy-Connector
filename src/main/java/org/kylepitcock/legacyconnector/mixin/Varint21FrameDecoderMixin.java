package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.network.Varint21FrameDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Network frames may be longer than 2 MiB: the length prefix is a plain VarInt, vanilla only refuses to read more than 3 of its bytes.
 * Legacy content makes some configuration-phase packets (registries, tags) larger than that; see {@link Varint21LengthFieldPrependerMixin}.
 */
@Mixin(Varint21FrameDecoder.class)
public abstract class Varint21FrameDecoderMixin {
    @ModifyConstant(method = "copyVarint", constant = @Constant(intValue = 3), require = 0)
    private static int legacyconnector$lengthBytes(int max) {
        return 5;
    }

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 3), require = 0)
    private static int legacyconnector$helperSize(int size) {
        return 5;
    }
}
