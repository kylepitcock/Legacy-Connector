package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.network.Varint21LengthFieldPrepender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Lets a packet longer than 2 MiB go out ("Packet too large: size 2974722 is over 8" kicked a player joining a server whose pack, with the
 * legacy content, had a configuration-phase packet that size). The receiving end needs {@link Varint21FrameDecoderMixin}, which this mod
 * applies on both sides.
 */
@Mixin(Varint21LengthFieldPrepender.class)
public abstract class Varint21LengthFieldPrependerMixin {
    @ModifyConstant(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Lio/netty/buffer/ByteBuf;)V",
            constant = @Constant(intValue = 3), require = 0)
    private int legacyconnector$maxLengthBytes(int max) {
        return 5;
    }
}
