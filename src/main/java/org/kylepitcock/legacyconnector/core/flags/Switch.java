package org.kylepitcock.legacyconnector.core.flags;

/**
 * One diagnostic or test switch: a JVM system property ({@code -Dname=value}) the layer reads. Every switch is declared once, in
 * {@link Flags} (or in a {@code compat} package for the ones that name a mod), with its type, default, category and description;
 * {@code docs/wiki/Diagnostic-Switches.md} is generated from those declarations and a unit test keeps call sites on the registry.
 *
 * <p>The accessors have the semantics of the JDK calls they replace and read the property on every call: a site that wants the value
 * once keeps it in a {@code static final}, a site that must see a change made at run time (a script, a test) calls the accessor again.
 */
public interface Switch {
    /** How the value is read. FLAG: true only for the text "true" (Boolean.getBoolean); TOGGLE: on unless the text is "false". */
    enum Type { FLAG, TOGGLE, INT, LONG, STRING, PATH }

    /** What the switch is for. INTERNAL ones are set by the loader for the runtime and are not meant to be given by hand. */
    enum Category { DEBUG, TEST, PERF, COMPAT, WORKAROUND, INTERNAL, TOOL }

    /** Full property name, for example {@code legacyconnector.entitySelfTest}. */
    String property();

    Type type();

    /** Default as shown in the documentation: "false", "24", "unset" (no default value, the feature is off), or a path. */
    String defaultText();

    Category category();

    String description();

    /** {@code Boolean.getBoolean}: true only when the property is the text "true". */
    default boolean on() {
        return Boolean.getBoolean(property());
    }

    /** {@code System.getProperty}: the value, or null when unset. */
    default String get() {
        return System.getProperty(property());
    }

    default String get(String def) {
        return System.getProperty(property(), def);
    }

    /** {@code Integer.getInteger}: null when unset or not a number. */
    default Integer getInt() {
        return Integer.getInteger(property());
    }

    default int getInt(int def) {
        return Integer.getInteger(property(), def);
    }

    default long getLong(long def) {
        return Long.getLong(property(), def);
    }

    default void set(String value) {
        System.setProperty(property(), value);
    }

    default void clear() {
        System.clearProperty(property());
    }

    /** The value when the switch is set and differs from its default, else null. */
    default String nonDefaultValue() {
        String v = System.getProperty(property());
        return v == null || v.equals(defaultText()) ? null : v;
    }
}
