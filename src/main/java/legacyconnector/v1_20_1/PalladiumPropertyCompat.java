package legacyconnector.v1_20_1;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Function;

/**
 * Backwards-compatible aliases for Palladium's property type lookup.
 *
 * <p>Palladium 4.5.10 registers the canonical {@code integer} property
 * factory, while older addon scripts also use {@code int}. Registering the
 * alias at integration setup keeps those scripts working without editing
 * each ability definition.</p>
 */
public final class PalladiumPropertyCompat {
    private static final String PROPERTY_LOOKUP =
            "net.threetag.palladium.util.property.PalladiumPropertyLookup";
    private static final System.Logger LOGGER = System.getLogger("LegacyConnector/PalladiumPropertyCompat");
    private static final String PROBE_KEY = "__legacyconnector_property_alias_probe__";

    private PalladiumPropertyCompat() {
    }

    /**
     * Installs legacy aliases before addon-pack ability scripts are evaluated.
     * Existing aliases are preserved; missing canonical types fail closed.
     */
    public static void registerLegacyTypeAliases(ClassLoader loader) {
        Objects.requireNonNull(loader, "Palladium class loader");
        try {
            Class<?> lookupClass = Class.forName(PROPERTY_LOOKUP, true, loader);
            boolean added = registerAlias(lookupClass, "int", "integer");
            if (added) {
                LOGGER.log(System.Logger.Level.INFO,
                        "Registered Palladium property type alias ''int'' → ''integer'' for legacy addon scripts");
            }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(
                    "Could not register legacy Palladium property type aliases against the inspected lookup API",
                    unwrap(error));
        }
    }

    /**
     * Shared, independently testable alias registration. The registry exposes
     * public static get(type, key) and register(type, factory) methods.
     *
     * @return true when a new alias was installed, false when it already existed
     */
    static synchronized boolean registerAlias(Class<?> lookupClass, String alias, String canonicalType)
            throws ReflectiveOperationException {
        Objects.requireNonNull(lookupClass, "Palladium property lookup class");
        Objects.requireNonNull(alias, "property type alias");
        Objects.requireNonNull(canonicalType, "canonical property type");

        Method get = lookupClass.getMethod("get", String.class, String.class);
        Object existing = get.invoke(null, alias, PROBE_KEY);
        if (existing != null) {
            return false;
        }

        Object canonicalProbe = get.invoke(null, canonicalType, PROBE_KEY);
        if (canonicalProbe == null) {
            throw new IllegalStateException(
                    "Palladium canonical property type '" + canonicalType + "' is unavailable");
        }

        Method register = lookupClass.getMethod("register", String.class, Function.class);
        Function<String, Object> aliasFactory = propertyKey -> {
            try {
                return get.invoke(null, canonicalType, propertyKey);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException(
                        "Palladium property alias '" + alias + "' could not delegate to '" + canonicalType + "'",
                        unwrap(error));
            }
        };
        register.invoke(null, alias, aliasFactory);

        if (get.invoke(null, alias, PROBE_KEY) == null) {
            throw new IllegalStateException("Palladium property type alias '" + alias + "' did not register");
        }
        return true;
    }

    private static Throwable unwrap(Throwable error) {
        if (error instanceof InvocationTargetException invocation && invocation.getCause() != null) {
            return invocation.getCause();
        }
        return error;
    }
}
