package legacyconnector.v1_12_2.vazkii.patchouli.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a field of a custom component that a book variable ("#name" in the component json) fills (Patchouli 1.12 API). */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface VariableHolder {
}
