package legacyconnector.v1_7_10.cpw.mods.fml.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a package (its package-info) as an API another mod provides; mods read it back to check for the API and its
 * version (Forestry's ModUtil.isAPILoaded). The loader counts {@link #provides} as a present mod.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
public @interface API {
    String owner();

    String provides();

    String apiVersion();
}
