package legacyconnector.v1_20_1.net.minecraftforge.registries;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Forge 1.20.1's {@code @ObjectHolder} (NeoForge 1.21.1 removed it): a static field receives the registry entry of its name once the registry holds it
 * ({@code ObjectHolders1201} injects after every registry event). On a field {@link #value()} is the entry name; on a class it is the namespace and the
 * public static fields are looked up under their lower-cased names.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface ObjectHolder {
    /** Registry name ({@code minecraft:block}, a mod's own registry); empty = chosen from the field type. */
    String registryName() default "";

    String value() default "";
}
