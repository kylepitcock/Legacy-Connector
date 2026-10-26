package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a package as an API another mod provides (see the 1.7.10 one); the loader counts {@link #provides} as a mod. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
public @interface API {
    String owner();

    String provides();

    String apiVersion();
}
