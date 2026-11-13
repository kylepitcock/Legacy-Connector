package org.kylepitcock.legacyconnector.v1_7_10.entity;

import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntitySizes1710Test {
    private static float width;
    private static float height;
    private static float eye;

    @Test
    void commonEntryPointDoesNotLinkOptionalPehkuiApi() throws IOException {
        try (var in = EntitySizes1710.class.getResourceAsStream("EntitySizes1710.class")) {
            assertTrue(in != null, "compiled EntitySizes1710 class is available");
            String constantPool = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
            assertFalse(constantPool.contains("virtuoel/pehkui/api/"), constantPool);
            assertTrue(constantPool.contains("PehkuiEntitySizes1710"), constantPool);
        }
    }

    @Test
    void invokesOptionalSizeAndEyeMethodsWithTheirActualArities() throws Throwable {
        var lookup = MethodHandles.lookup();
        var size = lookup.findStatic(EntitySizes1710Test.class, "captureSize",
                MethodType.methodType(void.class, Entity.class, float.class, float.class));
        var eyeHeight = lookup.findStatic(EntitySizes1710Test.class, "captureEye",
                MethodType.methodType(void.class, Entity.class, float.class));

        EntitySizes1710.invokeSetSize(size, null, 2.0F, 3.0F);
        EntitySizes1710.invokeSetEyeHeight(eyeHeight, null, 0.75F);

        assertEquals(2.0F, width, 0.0F);
        assertEquals(3.0F, height, 0.0F);
        assertEquals(0.75F, eye, 0.0F);
    }

    private static void captureSize(Entity ignored, float newWidth, float newHeight) {
        width = newWidth;
        height = newHeight;
    }

    private static void captureEye(Entity ignored, float newEye) {
        eye = newEye;
    }
}
