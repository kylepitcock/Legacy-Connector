package org.kylepitcock.legacyconnector.compat.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Point;
import legacyconnector.v1_7_10.codechicken.lib.gui.GuiDraw;
import org.junit.jupiter.api.Test;

class GuiDrawMouseTest {
    @Test
    void callbackReadsRecipeMouseAndCannotChangeItThroughReturnedPoint() {
        GuiDraw.mcc$withMousePosition(new Point(300, 200), () -> {
            Point recipeMouse = GuiDraw.mcc$withMousePosition(new Point(120, 36), () -> {
                Point mouse = GuiDraw.getMousePosition();
                mouse.translate(-5, -16);
                assertEquals(new Point(120, 36), GuiDraw.getMousePosition());
                return mouse;
            });
            assertEquals(new Point(115, 20), recipeMouse);
            assertEquals(new Point(300, 200), GuiDraw.getMousePosition());
            return null;
        });
    }

    @Test
    void failingCallbackRestoresPreviousMousePosition() {
        GuiDraw.mcc$withMousePosition(new Point(300, 200), () -> {
            assertThrows(IllegalStateException.class, () -> GuiDraw.mcc$withMousePosition(new Point(1, 2), () -> {
                assertEquals(new Point(1, 2), GuiDraw.getMousePosition());
                throw new IllegalStateException("Legacy callback failed");
            }));
            assertEquals(new Point(300, 200), GuiDraw.getMousePosition());
            return null;
        });
    }

}