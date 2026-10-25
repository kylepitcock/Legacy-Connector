package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.lang.reflect.Field;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The game darkens everything below the horizon (a dark disc under the sky, and the sky colour fading to black) whenever the camera
 * is lower than a fixed height of 63 blocks, unless the world is flat. Legacy mod dimensions have their ground at all sorts of
 * heights (Galacticraft's moons, Mystcraft ages), so the dark band showed up at a certain height. Their client levels are marked
 * flat, which moves the horizon to the bottom of the world.
 */
public final class LegacyHorizon {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Horizon");
    private static boolean failed;

    private LegacyHorizon() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener((LevelEvent.Load e) -> {
            if (e.getLevel() instanceof ClientLevel level && isLegacyDimension(level)) markFlat(level);
        });
    }

    private static boolean isLegacyDimension(ClientLevel level) {
        var id = level.dimension().location();
        return id.getNamespace().equals("legacyconnector") && id.getPath().startsWith("dim_");
    }

    private static void markFlat(ClientLevel level) {
        if (failed) return;
        try {
            Field dataField = ClientLevel.class.getDeclaredField("clientLevelData");
            dataField.setAccessible(true);
            Object data = dataField.get(level);
            Field flat = data.getClass().getDeclaredField("isFlat");
            flat.setAccessible(true);
            flat.setBoolean(data, true);
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            LOG.warn("Could not mark the client level of {} flat; the dark band at the horizon stays: {}", level.dimension().location(), e.toString());
        }
    }
}
