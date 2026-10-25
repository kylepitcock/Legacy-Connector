package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {
    String modid();

    String name() default "";

    String version() default "";

    String dependencies() default "";

    boolean useMetadata() default false;

    String acceptedMinecraftVersions() default "";

    String acceptableRemoteVersions() default "";

    String acceptableSaveVersions() default "";

    String certificateFingerprint() default "";

    String modLanguage() default "java";

    String guiFactory() default "";

    CustomProperty[] customProperties() default {};

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface EventHandler {
    }

    /** Registers the static {@code @SubscribeEvent} methods of the annotated class on the Forge event bus. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface EventBusSubscriber {
        legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side[] value() default {
                legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side.CLIENT, legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side.SERVER};

        String modid() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Instance {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Metadata {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface CustomProperty {
        String k();

        String v();
    }
}
