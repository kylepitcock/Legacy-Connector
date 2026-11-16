package legacyconnector.v1_6_4.net.minecraft.src.forge;

/** Forge 3.x: a block or item that draws from its own sprite sheet; {@code Block.getTextureFile} is the same method, which is what the layer reads. */
public interface ITextureProvider {
    String getTextureFile();
}
