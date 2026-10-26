package legacyconnector.v1_12_2.net.minecraftforge.fml.common.event;

import legacyconnector.v1_12_2.net.minecraftforge.fml.common.Loader;

public class FMLPostInitializationEvent extends FMLStateEvent {
    @Override
    public legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState getModState() {
        return legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState.POSTINITIALIZED;
    }

    public Object buildSoftDependProxy(String modId, String className) {
        return buildSoftDependProxy(modId, className, new Object[0]).orElse(null);
    }

    /** An instance of {@code className} built with these arguments when {@code modId} is loaded, else empty. */
    public java.util.Optional<?> buildSoftDependProxy(String modId, String className, Object... arguments) {
        if (!Loader.isModLoaded(modId)) return java.util.Optional.empty();
        try {
            Class<?> clz = Class.forName(className, true, Loader.instance().getModClassLoader());
            Class<?>[] types = new Class<?>[arguments.length];
            for (int i = 0; i < arguments.length; i++) types[i] = arguments[i] == null ? Object.class : arguments[i].getClass();
            for (var ctor : clz.getConstructors()) {
                if (ctor.getParameterCount() != arguments.length) continue;
                boolean fits = true;
                Class<?>[] params = ctor.getParameterTypes();
                for (int i = 0; i < params.length && fits; i++) fits = arguments[i] == null ? !params[i].isPrimitive() : wrap(params[i]).isInstance(arguments[i]);
                if (fits) return java.util.Optional.of(ctor.newInstance(arguments));
            }
            return java.util.Optional.of(clz.getConstructor(types).newInstance(arguments));
        } catch (Exception | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/1.12.2").warn("Cannot build the soft dependency proxy {} for {}: {}", className, modId, e.toString());
            return java.util.Optional.empty();
        }
    }

    private static Class<?> wrap(Class<?> c) {
        if (!c.isPrimitive()) return c;
        return java.lang.invoke.MethodType.methodType(c).wrap().returnType();
    }
}
