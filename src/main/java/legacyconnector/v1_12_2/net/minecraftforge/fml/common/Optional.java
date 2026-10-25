package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Soft dependencies: members and interfaces that exist only when another mod is loaded (stripped by the loader otherwise). */
public final class Optional {
    private Optional() {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface Interface {
        String iface();

        String modid();

        boolean striprefs() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface InterfaceList {
        Interface[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    public @interface Method {
        String modid();
    }
}
