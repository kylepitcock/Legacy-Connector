package legacyconnector.vb1_7_3.net.minecraft.src.forge;

/** Forge 1.0: a block or item whose sprite indices point into its own 256x256 sheet instead of terrain.png / gui/items.png. */
public interface ITextureProvider {
    String getTextureFile();
}
