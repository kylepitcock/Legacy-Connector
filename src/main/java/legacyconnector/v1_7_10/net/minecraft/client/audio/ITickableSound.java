package legacyconnector.v1_7_10.net.minecraft.client.audio;

/** A sound that updates every tick (follows an entity, fades) until it reports it is done. */
public interface ITickableSound extends ISound, legacyconnector.v1_7_10.net.minecraft.server.gui.IUpdatePlayerListBox {
    boolean isDonePlaying();

    void update();
}
