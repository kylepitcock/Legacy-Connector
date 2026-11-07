package legacyconnector.v1_12_2.mcp.mobius.waila.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks an {@link IWailaPlugin}: Waila (HWYLA) instantiated every annotated class and called {@code register}. The value is an optional required mod id. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WailaPlugin {
    String value() default "";
}
