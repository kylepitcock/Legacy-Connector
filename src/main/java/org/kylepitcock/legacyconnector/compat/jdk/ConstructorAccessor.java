package org.kylepitcock.legacyconnector.compat.jdk;

import java.lang.reflect.InvocationTargetException;

/**
 * Replacement for {@code sun.reflect.ConstructorAccessor} (removed from the JDK): what {@link ReflectionFactory#newConstructorAccessor} returns.
 * Legacy code used it to construct enum constants, which {@code Constructor.newInstance} refuses.
 */
public interface ConstructorAccessor {
    Object newInstance(Object[] args) throws InstantiationException, IllegalArgumentException, InvocationTargetException;
}
