package legacyconnector.vb1_7_3.net.minecraft.src.forge;

/** Forge 1.0: a block drawn in the render passes it accepts (0 opaque, 1 translucent). */
public interface IMultipassRender {
    boolean canRenderInPass(int pass);
}
