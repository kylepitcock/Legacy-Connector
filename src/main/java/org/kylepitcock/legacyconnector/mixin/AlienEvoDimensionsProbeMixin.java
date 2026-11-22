package org.kylepitcock.legacyconnector.mixin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Temporary diagnostics for AlienEvo player-form sizing. Captures dimensions
 * before/after refreshDimensions and samples only changed player sizing state
 * at tick tail. Pehkui is queried reflectively so it remains an optional link.
 * This mixin never changes dimensions or scales.
 */
@Mixin(Entity.class)
public abstract class AlienEvoDimensionsProbeMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("LegacyConnector/AlienEvoDimensionsProbe");
    private static final ConcurrentHashMap<UUID, String> LAST_PLAYER_SNAPSHOTS = new ConcurrentHashMap<>();

    @Inject(method = "refreshDimensions", at = @At("HEAD"), require = 0)
    private void legacyconnector$beforeRefreshDimensions(CallbackInfo ci) {
        legacyconnector$recordDimensions("before");
    }

    @Inject(method = "refreshDimensions", at = @At("TAIL"), require = 0)
    private void legacyconnector$afterRefreshDimensions(CallbackInfo ci) {
        legacyconnector$recordDimensions("after");
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void legacyconnector$sampleChangedPlayerSize(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof Player player) || !ModList.get().isLoaded("alienevo")) return;
        String snapshot = legacyconnector$snapshot(player);
        String previous = LAST_PLAYER_SNAPSHOTS.put(player.getUUID(), snapshot);
        if (!snapshot.equals(previous)) {
            LOGGER.info("[AlienEvoDimensionsProbe] phase=tick-change player={} uuid={} {}",
                    player.getName().getString(), player.getUUID(), snapshot);
        }
    }

    private void legacyconnector$recordDimensions(String phase) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof Player player) || !ModList.get().isLoaded("alienevo")) return;
        LOGGER.info("[AlienEvoDimensionsProbe] phase={} player={} uuid={} {}",
                phase, player.getName().getString(), player.getUUID(), legacyconnector$snapshot(player));
    }

    private static String legacyconnector$snapshot(Player player) {
        EntityDimensions dimensions = player.getDimensions(player.getPose());
        AABB box = player.getBoundingBox();
        return "pose=" + player.getPose()
                + " pos=" + player.position()
                + " dimensions=" + dimensions.width() + "x" + dimensions.height()
                + " eyeHeight=" + dimensions.eyeHeight()
                + " bb=" + player.getBbWidth() + "x" + player.getBbHeight()
                + " aabb=[" + box.minX + "," + box.minY + "," + box.minZ
                + " .. " + box.maxX + "," + box.maxY + "," + box.maxZ + "]"
                + " pehkui=" + legacyconnector$pehkuiScales(player);
    }

    private static String legacyconnector$pehkuiScales(Entity entity) {
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
