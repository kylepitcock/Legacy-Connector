package legacyconnector.v1_20_1.net.minecraftforge.common.capabilities;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Forge 1.20.1 marker: register this interface as a capability. A capability exists here as soon as it is named, so nothing is done. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoRegisterCapability {
}
