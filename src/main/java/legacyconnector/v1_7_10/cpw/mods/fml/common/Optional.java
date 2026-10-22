package legacyconnector.v1_7_10.cpw.mods.fml.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Soft-dependency markers. Legacy FML stripped the annotated interface/method when the named mod was absent;
 * the loader performs the same stripping at translation time when the referenced classes cannot be found.
 */
public final class Optional {
    private Optional() {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface InterfaceList {
        Interface[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Interface {
        String iface();

        String modid();

        boolean striprefs() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Method {
        String modid();
    }
}
