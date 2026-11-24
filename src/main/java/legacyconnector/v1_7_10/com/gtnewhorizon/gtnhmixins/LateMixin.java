package legacyconnector.v1_7_10.com.gtnewhorizon.gtnhmixins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** GTNH Mixins' marker annotation on a class that implements {@link ILateMixinLoader}. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LateMixin {
}
