package legacyconnector.v1_7_10.com.gtnewhorizons.angelica.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Angelica's marker on an {@code ISimpleBlockRenderingHandler} that may run on the chunk builder threads. The renderer replacement is an engine mod
 * the layer never loads; the annotation is kept so the mods that carry it link (the layer's own chunk building decides how handlers run).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ThreadSafeISBRH {
    boolean perThread();
}
