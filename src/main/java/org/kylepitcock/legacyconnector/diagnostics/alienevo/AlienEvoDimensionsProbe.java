package org.kylepitcock.legacyconnector.diagnostics.alienevo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
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
                + " dimensionsEyeHeight=" + dimensions.eyeHeight()
                + " actualEyeHeight=" + player.getEyeHeight()
                + " bb=" + player.getBbWidth() + "x" + player.getBbHeight()
                + " aabb=[" + box.minX + "," + box.minY + "," + box.minZ
                + " .. " + box.maxX + "," + box.maxY + "," + box.maxZ + "]"
                + " alienEvo=" + alienEvoScales(player)
                + " sizeAbility=" + activeSizeAbility(player)
                + " pehkui=" + pehkuiScales(player);
    }

    /**
     * Reads AlienEvo's own scale state rather than Pehkui's independent scale
     * registry. This is essential because dev.24 stores alienevo:size values
     * in AlienScaleData even when Pehkui is installed.
     */
    private static String alienEvoScales(Entity entity) {
        try {
            ClassLoader loader = modClassLoader(entity);
            Class<?> alienScale = Class.forName("net.habb.alienevo.scale.AlienScale", true, loader);
            Object data = alienScale.getMethod("peek", Entity.class).invoke(null, entity);
            if (data == null) {
                return "data=absent effective={base=1.0,hitboxWidth=1.0,hitboxHeight=1.0,"
                        + "eyeHeight=1.0,modelWidth=1.0,modelHeight=1.0}";
            }

            StringBuilder out = new StringBuilder("data=present effective={");
            String[] getters = {
                    "base", "hitboxWidth", "hitboxHeight", "eyeHeight", "modelWidth", "modelHeight"
            };
            for (int i = 0; i < getters.length; i++) {
                if (i > 0) out.append(',');
                out.append(getters[i]).append('=').append(invokeNoArgs(data, getters[i]));
            }
            out.append("} channels={");

            Class<?> channelType = Class.forName("net.habb.alienevo.scale.ScaleChannel", true, loader);
            Method getChannel = data.getClass().getMethod("getChannel", channelType);
            Method getTarget = data.getClass().getMethod("getTarget", channelType);
            String[] channels = {
                    "OVERALL", "ABILITY", "HITBOX_WIDTH", "HITBOX_HEIGHT",
                    "EYE_HEIGHT", "MODEL_WIDTH", "MODEL_HEIGHT"
            };
            for (int i = 0; i < channels.length; i++) {
                if (i > 0) out.append(';');
                Object channel = channelType.getField(channels[i]).get(null);
                out.append(channels[i])
                        .append("{current=").append(getChannel.invoke(data, channel))
                        .append(",target=").append(getTarget.invoke(data, channel)).append('}');
            }
            return out.append('}').toString();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            return "unavailable:" + error.getClass().getSimpleName() + ":" + error.getMessage();
        }
    }

    /**
     * Captures the actually enabled alienevo:size entries and their configured
     * values, so a missing AlienScaleData can be separated from an ability
     * that never became active or whose properties failed to parse.
     */
    private static String activeSizeAbility(LivingEntity entity) {
        try {
            ClassLoader loader = modClassLoader(entity);
            Class<?> abilityType = Class.forName("net.threetag.palladium.power.ability.Ability", true, loader);
            Class<?> propertyType = Class.forName("net.threetag.palladium.util.property.PalladiumProperty", true, loader);
            Class<?> abilities = Class.forName("net.habb.alienevo.ability.EvoAbilities", true, loader);
            Object sizeSupplier = abilities.getField("ALIEN_SIZE").get(null);
            Object sizeAbility = sizeSupplier.getClass().getMethod("get").invoke(sizeSupplier);
            Class<?> abilityUtil = Class.forName("net.threetag.palladium.power.ability.AbilityUtil", true, loader);
            Method getEnabledEntries = abilityUtil.getMethod("getEnabledEntries", LivingEntity.class, abilityType);
            Object result = getEnabledEntries.invoke(null, entity, sizeAbility);
            if (!(result instanceof Collection<?> entries) || entries.isEmpty()) return "[]";

            Class<?> sizeAbilityClass = Class.forName("net.habb.alienevo.ability.AlienSizeAbility", true, loader);
            Class<?> abilityInstanceType =
                    Class.forName("net.threetag.palladium.power.ability.AbilityInstance", true, loader);
            Method getProperty = abilityInstanceType.getMethod("getProperty", propertyType);
            Method getReference = abilityInstanceType.getMethod("getReference");
            String[] properties = {
                    "SCALE", "HITBOX_WIDTH", "HITBOX_HEIGHT", "EYE_HEIGHT", "MODEL_WIDTH", "MODEL_HEIGHT"
            };
            StringBuilder out = new StringBuilder("[");
            int index = 0;
            for (Object entry : entries) {
                if (index++ > 0) out.append(';');
                Object entryId = entry.getClass().getField("id").get(entry);
                Object reference = getReference.invoke(entry);
                out.append("powerEntry=").append(reference)
                        .append(",id=").append(entryId).append('{');
                for (int i = 0; i < properties.length; i++) {
                    if (i > 0) out.append(',');
                    String name = properties[i];
                    Field propertyField = sizeAbilityClass.getField(name);
                    Object property = propertyField.get(null);
                    out.append(name.toLowerCase()).append('=').append(getProperty.invoke(entry, property));
                }
                out.append('}');
            }
            return out.append(']').toString();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            return "unavailable:" + error.getClass().getSimpleName() + ":" + error.getMessage();
        }
    }

    private static Object invokeNoArgs(Object target, String methodName) throws ReflectiveOperationException {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    private static ClassLoader modClassLoader(Entity entity) {
        ClassLoader loader = entity.getClass().getClassLoader();
        return loader != null ? loader : AlienEvoDimensionsProbe.class.getClassLoader();
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
