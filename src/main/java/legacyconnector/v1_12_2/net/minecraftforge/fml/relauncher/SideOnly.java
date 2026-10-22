package legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Members marked for the other physical side are stripped by the loader when the jar is translated. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PACKAGE})
public @interface SideOnly {
    Side value();
}
