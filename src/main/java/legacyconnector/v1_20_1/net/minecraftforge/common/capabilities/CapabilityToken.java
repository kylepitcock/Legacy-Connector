package legacyconnector.v1_20_1.net.minecraftforge.common.capabilities;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * {@code new CapabilityToken<IMyCap>(){}}: names a capability interface by its generic argument. Forge rewrote the subclass at load time; here the
 * name is read from the anonymous subclass's generic superclass.
 */
public abstract class CapabilityToken<T> {
    protected String getType() {
        Type superclass = getClass().getGenericSuperclass();
        if (superclass instanceof ParameterizedType p && p.getActualTypeArguments().length == 1) {
            Type arg = p.getActualTypeArguments()[0];
            if (arg instanceof Class<?> c) return c.getName();
            if (arg instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> c) return c.getName();
            return arg.getTypeName();
        }
        throw new IllegalStateException("CapabilityToken subclass without a type argument: " + getClass());
    }
}
