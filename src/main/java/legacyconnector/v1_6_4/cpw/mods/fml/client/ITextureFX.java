package legacyconnector.v1_6_4.cpw.mods.fml.client;

/** FML 1.4.7's extension of an animated tile for texture packs of other resolutions. */
public interface ITextureFX {
    void onTextureDimensionsUpdate(int width, int height);

    void setErrored(boolean errored);

    boolean getErrored();
}
