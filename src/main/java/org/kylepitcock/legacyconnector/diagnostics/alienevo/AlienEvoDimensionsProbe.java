package org.kylepitcock.legacyconnector.diagnostics.alienevo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Non-mixin utility for opt-in-by-presence AlienEvo/Pehkui sizing diagnostics. */
public final class AlienEvoDimensionsProbe {
    private static final Logger LOGGER = LoggerFactory.getLogger("LegacyConnector/AlienEvoDimensionsProbe");
    private static final ConcurrentHashMap<String, String> LAST_PLAYER_SNAPSHOTS = new ConcurrentHashMap<>();

    private AlienEvoDimensionsProbe() {}

    public static void record(Player player, String phase) {
        if (!ModList.get().isLoaded("alienevo")) return;
        LOGGER.info("[AlienEvoDimensionsProbe] phase={} player={} uuid={} {}",
                phase, player.getName().getString(), player.getUUID(), snapshot(player));
    }

    public static void sample(Player player) {
        if (!ModList.get().isLoaded("alienevo")) return;
        String key = player.getUUID() + (player.level().isClientSide ? ":client" : ":server");
        String state = snapshot(player);
        String previous = LAST_PLAYER_SNAPSHOTS.put(key, state);
        if (!state.equals(previous)) {
            LOGGER.info("[AlienEvoDimensionsProbe] phase=tick-change player={} uuid={} side={} {}",
                    player.getName().getString(), player.getUUID(),
                    player.level().isClientSide ? "client" : "server", state);
        }
    }

    private static String snapshot(Player player) {
        EntityDimensions dimensions = player.getDimensions(player.getPose());
        AABB box = player.getBoundingBox();
        return "side=" + (player.level().isClientSide ? "client" : "server")
                + " pose=" + player.getPose()
                + " pos=" + player.position()
                + " dimensions=" + dimensions.width() + "x" + dimensions.height()
                + " eyeHeight=" + dimensions.eyeHeight()
                + " bb=" + player.getBbWidth() + "x" + player.getBbHeight()
                + " aabb=[" + box.minX + "," + box.minY + "," + box.minZ
                + " .. " + box.maxX + "," + box.maxY + "," + box.maxZ + "]"
                + " pehkui=" + pehkuiScales(player);
    }

    private static String pehkuiScales(Entity entity) {
        try {
            ClassLoader loader = Entity.class.getClassLoader();
            Class<?> scaleTypes = Class.forName("virtuoel.pehkui.api.ScaleTypes", true, loader);
            String[] names = {
                    "BASE", "WIDTH", "HEIGHT", "HITBOX_WIDTH", "HITBOX_HEIGHT", "MODEL_WIDTH", "MODEL_HEIGHT"
            };
            StringBuilder out = new StringBuilder();
            for (String name : names) {
                if (out.length() > 0) out.append(';');
                Field typeField = scaleTypes.getField(name);
                Object scaleType = typeField.get(null);
                Method getScaleData = scaleType.getClass().getMethod("getScaleData", Entity.class);
                Object data = getScaleData.invoke(scaleType, entity);
                if (data == null) {
                    out.append(name).append("=null");
                    continue;
                }
                Method getScale = data.getClass().getMethod("getScale");
                Method getBaseScale = data.getClass().getMethod("getBaseScale");
                Method getTargetScale = data.getClass().getMethod("getTargetScale");
                out.append(name)
                        .append("{effective=").append(getScale.invoke(data))
                        .append(",base=").append(getBaseScale.invoke(data))
                        .append(",target=").append(getTargetScale.invoke(data))
                        .append('}');
            }
            return out.toString();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            return "unavailable:" + error.getClass().getSimpleName() + ":" + error.getMessage();
        }
    }
}
