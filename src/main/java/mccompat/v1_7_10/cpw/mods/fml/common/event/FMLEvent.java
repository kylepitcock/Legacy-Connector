package mccompat.v1_7_10.cpw.mods.fml.common.event;

public class FMLEvent {
    public final String getEventType() {
        return getClass().getSimpleName();
    }

    public String description() {
        String n = getClass().getSimpleName();
        return n.substring("FML".length(), n.length() - "Event".length());
    }
}
