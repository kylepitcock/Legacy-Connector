package legacyconnector.v1_6_4.net.minecraft.src;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** ModLoader's configuration annotation on a {@code mod_*} class's public static fields (the layer keeps the field's initial value). */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MLProp {
    String info() default "";

    String name() default "";

    double min() default Double.NEGATIVE_INFINITY;

    double max() default Double.POSITIVE_INFINITY;
}
