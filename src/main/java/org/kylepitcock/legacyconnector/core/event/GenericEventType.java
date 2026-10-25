package org.kylepitcock.legacyconnector.core.event;

/** An event with a type parameter that handlers filter on (1.12's {@code RegistryEvent.Register<Block>}): handlers see it only for their own type. */
public interface GenericEventType {
    Class<?> mcc$genericType();
}
