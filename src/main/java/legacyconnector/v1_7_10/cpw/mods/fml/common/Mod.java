package legacyconnector.v1_7_10.cpw.mods.fml.common;

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

    /** Forge 1.6.4's per-event annotation (1.7 replaced all of them with {@link EventHandler}); the platform dispatches it like one. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface PreInit {
    }

    /** Forge 1.6.4's per-event annotation (1.7 replaced all of them with {@link EventHandler}); the platform dispatches it like one. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Init {
    }

    /** Forge 1.6.4's per-event annotation (1.7 replaced all of them with {@link EventHandler}); the platform dispatches it like one. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface PostInit {
    }

    /** Forge 1.6.4's per-event annotation (1.7 replaced all of them with {@link EventHandler}); the platform dispatches it like one. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface ServerAboutToStart {
    }

    /** Forge 1.6.4's per-event annotation (1.7 replaced all of them with {@link EventHandler}); the platform dispatches it like one. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface ServerStarting {
    }

    /** Forge 1.6.4's per-event annotation (1.7 replaced all of them with {@link EventHandler}); the platform dispatches it like one. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface ServerStarted {
    }

    /** Forge 1.6.4's per-event annotation (1.7 replaced all of them with {@link EventHandler}); the platform dispatches it like one. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface ServerStopping {
    }

    /** Forge 1.6.4's per-event annotation (1.7 replaced all of them with {@link EventHandler}); the platform dispatches it like one. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface ServerStopped {
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
