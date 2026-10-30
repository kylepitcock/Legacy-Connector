package legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler;

import org.kylepitcock.legacyconnector.core.event.GenericEventType;

/** An event with a type parameter that handlers filter on. */
public class GenericEvent<T> extends Event implements GenericEventType {
    private final Class<T> type;

    public GenericEvent(Class<T> type) {
        this.type = type;
    }

    /** The type parameter (Forge declares it as a {@link java.lang.reflect.Type}, which is what mods' calls name). */
    public java.lang.reflect.Type getGenericType() {
        return type;
    }

    @Override
    public Class<?> mcc$genericType() {
        return type;
    }
}
