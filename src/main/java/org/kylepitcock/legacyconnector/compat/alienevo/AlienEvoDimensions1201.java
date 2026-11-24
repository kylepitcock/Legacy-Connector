package org.kylepitcock.legacyconnector.compat.alienevo;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridges AlienEvo's scale channels into the 1.21.1 LivingEntity dimension
 * calculation. AlienEvo dev.24's own dimension mixins target Entity and
 * Player.getDimensions, but 1.21.1 Player inherits LivingEntity's final
 * getDimensions implementation instead.
 *
 * <p>No AlienEvo classes are linked at compile time: the purchased dev build
 * is optional and is discovered reflectively at runtime.</p>
 */
public final class AlienEvoDimensions1201 {
    private static final Logger LOGGER = LoggerFactory.getLogger("LegacyConnector/AlienEvoDimensions1201");
    private static final AtomicBoolean REFLECTION_FAILURE_LOGGED = new AtomicBoolean();
    private static volatile ScaleAccess SCALE_ACCESS;

    private AlienEvoDimensions1201() {}

    /**
     * Applies the per-entity AlienEvo hitbox width, hitbox height and eye
     * height channels to the dimensions returned by 1.21.1 LivingEntity.
     */
    public static EntityDimensions apply(Entity entity, EntityDimensions original) {
        if (entity == null || original == null || !ModList.get().isLoaded("alienevo")) {
            return original;
        }

        try {
            ScaleAccess access = scaleAccess(entity.getClass().getClassLoader());
            if (access.peek.invoke(null, entity) == null) {
                return original;
            }

            float widthScale = ((Number) access.hitboxWidth.invoke(null, entity)).floatValue();
            float heightScale = ((Number) access.hitboxHeight.invoke(null, entity)).floatValue();
            float eyeHeightScale = ((Number) access.eyeHeight.invoke(null, entity)).floatValue();
            return applyScales(original, widthScale, heightScale, eyeHeightScale);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            if (REFLECTION_FAILURE_LOGGED.compareAndSet(false, true)) {
                LOGGER.warn("Could not read AlienEvo scale channels for the 1.21.1 LivingEntity dimensions adapter; "
                        + "leaving vanilla dimensions unchanged", error);
            }
            return original;
        }
    }

    /**
     * Pure scaling operation kept package-private so the channel arithmetic
     * can be regression-tested without loading AlienEvo.
     *
     * <p>EntityDimensions.scale(width,height) scales its embedded eye height
     * by the hitbox-height factor. AlienEvo has an independent EYE_HEIGHT
     * channel, so replace that value with original.eyeHeight * eyeHeightScale
     * after scaling the collision box.</p>
     */
    static EntityDimensions applyScales(
            EntityDimensions original, float widthScale, float heightScale, float eyeHeightScale) {
        if (original == null
                || !validScale(widthScale)
                || !validScale(heightScale)
                || !validScale(eyeHeightScale)) {
            return original;
        }

        if (widthScale == 1.0F && heightScale == 1.0F && eyeHeightScale == 1.0F) {
            return original;
        }

        return original.scale(widthScale, heightScale)
                .withEyeHeight(original.eyeHeight() * eyeHeightScale);
    }

    private static boolean validScale(float scale) {
        return Float.isFinite(scale) && scale > 0.0F;
    }

    private static ScaleAccess scaleAccess(ClassLoader entityClassLoader) throws ReflectiveOperationException {
        ClassLoader loader = entityClassLoader != null ? entityClassLoader : Entity.class.getClassLoader();
        ScaleAccess current = SCALE_ACCESS;
        if (current != null && current.loader == loader) {
            return current;
        }

        synchronized (AlienEvoDimensions1201.class) {
            current = SCALE_ACCESS;
            if (current != null && current.loader == loader) {
                return current;
            }

            Class<?> alienScale = Class.forName(
                    "net.habb.alienevo.scale.AlienScale", true, loader);
            current = new ScaleAccess(
                    loader,
                    alienScale.getMethod("peek", Entity.class),
                    alienScale.getMethod("getHitboxWidthScale", Entity.class),
                    alienScale.getMethod("getHitboxHeightScale", Entity.class),
                    alienScale.getMethod("getEyeHeightScale", Entity.class));
            SCALE_ACCESS = current;
            return current;
        }
    }

    private static final class ScaleAccess {
        private final ClassLoader loader;
        private final Method peek;
        private final Method hitboxWidth;
        private final Method hitboxHeight;
        private final Method eyeHeight;

        private ScaleAccess(
                ClassLoader loader, Method peek, Method hitboxWidth, Method hitboxHeight, Method eyeHeight) {
            this.loader = loader;
            this.peek = peek;
            this.hitboxWidth = hitboxWidth;
            this.hitboxHeight = hitboxHeight;
            this.eyeHeight = eyeHeight;
        }
    }
}
