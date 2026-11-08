package legacyconnector.vb1_7_3.net.minecraft.src;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** ModLoader's config annotation: a static field of a mod class read from and written to {@code config/<mod class>.cfg}. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MLProp {
    String name() default "";

    String info() default "";

    double min() default Double.NEGATIVE_INFINITY;

    double max() default Double.POSITIVE_INFINITY;
}
