package org.kylepitcock.legacyconnector.mixin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
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
 * Temporary diagnostics for AlienEvo player-form sizing. Captures the dimensions
 * Minecraft is asked to use before/after each dimensions refresh, plus effective
 * Pehkui model and hitbox scales. It intentionally does not alter any values.
 */
@Mixin(Entity.class)
public abstract class AlienEvoDimensionsProbeMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("LegacyConnector/AlienEvoDimensionsProbe");

    @Inject(method = "refreshDimensions", at = @At("HEAD"), require = 0)
    private void legacyconnector$beforeRefreshDimensions(CallbackInfo ci) {
        legacyconnector$recordDimensions("before");
    }

    @Inject(method = "refreshDimensions", at = @At("TAIL"), require = 0)
    private void legacyconnector$afterRefreshDimensions(CallbackInfo ci) {
        legacyconnector$recordDimensions("after");
    }

    private void legacyconnector$recordDimensions(String phase) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof Player player) || !ModList.get().isLoaded("alienevo")) return;

        EntityDimensions dimensions = player.getDimensions(player.getPose());
        AABB box = player.getBoundingBox();
        LOGGER.info(
                "[AlienEvoDimensionsProbe] phase={} player={} uuid={} pose={} pos={} dimensions(width={},height={},eyeHeight={}) "
                        + "bbWidth={} bbHeight={} aabb=[{}..{}] pehkui={}",
                phase, player.getName().getString(), player.getUUID(), player.getPose(), player.position(),
                dimensions.width(), dimensions.height(), dimensions.eyeHeight(),
                player.getBbWidth(), player.getBbHeight(),
                new net.minecraft.world.phys.Vec3(box.minX, box.minY, box.minZ),
                new net.minecraft.world.phys.Vec3(box.maxX, box.maxY, box.maxZ),
                legacyconnector$pehkuiScales(player));
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
