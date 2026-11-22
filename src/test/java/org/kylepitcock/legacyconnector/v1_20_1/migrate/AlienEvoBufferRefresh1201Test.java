package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;

class AlienEvoBufferRefresh1201Test {
    @Test
    void preservesUnknownCustomVertexConsumers() {
        VertexConsumer custom = (VertexConsumer) Proxy.newProxyInstance(
                AlienEvoBufferRefresh1201Test.class.getClassLoader(),
                new Class<?>[] {VertexConsumer.class},
                (proxy, method, args) -> {
                    if (method.getReturnType() == VertexConsumer.class) return proxy;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == float.class) return 0.0f;
                    if (method.getReturnType() == double.class) return 0.0d;
                    return null;
                });

        assertFalse(AlienEvoBufferRefresh1201.needsRefresh(custom));
    }

    @Test
    void treatsNullAsNotRefreshable() {
        assertFalse(AlienEvoBufferRefresh1201.needsRefresh(null));
    }
}
