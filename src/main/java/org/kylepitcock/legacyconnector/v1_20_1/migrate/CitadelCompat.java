package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.lang.reflect.Method;
import legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.Event$Result;
import net.neoforged.neoforge.common.util.TriState;

/**
 * Citadel 2.x for 1.20.1 against Citadel 2.7 for 1.21.1: its events carry a {@code TriState} result instead of Forge's {@code Event.Result}.
 * Citadel is not a compile dependency of the layer, so the events arrive as {@code Object}.
 */
public final class CitadelCompat {
    private CitadelCompat() {
    }

    public static void setResult(Object event, Event$Result result) {
        TriState state = switch (result) {
            case ALLOW -> TriState.TRUE;
            case DENY -> TriState.FALSE;
            default -> TriState.DEFAULT;
        };
        try {
            Method m = event.getClass().getMethod("setResult", TriState.class);
            m.invoke(event, state);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Citadel event " + event.getClass().getName() + " has no setResult(TriState)", e);
        }
    }

    public static Event$Result getResult(Object event) {
        try {
            Object state = event.getClass().getMethod("getResult").invoke(event);
            if (state == TriState.TRUE) return Event$Result.ALLOW;
            if (state == TriState.FALSE) return Event$Result.DENY;
            return Event$Result.DEFAULT;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Citadel event " + event.getClass().getName() + " has no getResult()", e);
        }
    }
}
