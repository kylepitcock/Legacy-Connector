package legacyconnector.v1_12_2.net.minecraftforge.fml.common.event;

public class FMLEvent {
    public final String getEventType() {
        return getClass().getSimpleName();
    }

    public String description() {
        String n = getClass().getSimpleName();
        return n.substring("FML".length(), n.length() - "Event".length());
    }
}
