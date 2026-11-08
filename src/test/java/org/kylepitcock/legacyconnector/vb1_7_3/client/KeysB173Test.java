package org.kylepitcock.legacyconnector.vb1_7_3.client;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

class KeysB173Test {
    @Test void controlCharactersReachLegacyScreensWithoutTurningAltGrIntoClipboardInput(){
        assertEquals(22,KeysB173.character(GLFW.GLFW_KEY_V,GLFW.GLFW_MOD_CONTROL));
        assertEquals(1,KeysB173.character(GLFW.GLFW_KEY_A,GLFW.GLFW_MOD_CONTROL));
        assertEquals(0,KeysB173.character(GLFW.GLFW_KEY_V,GLFW.GLFW_MOD_CONTROL|GLFW.GLFW_MOD_ALT));
        assertEquals('\r',KeysB173.character(GLFW.GLFW_KEY_ENTER,0));
        assertFalse(KeysB173.types(GLFW.GLFW_KEY_V,GLFW.GLFW_MOD_CONTROL));
        assertTrue(KeysB173.types(GLFW.GLFW_KEY_V,0));
    }
}
