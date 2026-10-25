package legacyconnector.v1_12_2.net.minecraftforge.common.capabilities;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a static field (or method) that receives the capability of the given type once it is registered. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface CapabilityInject {
    Class<?> value();
}
