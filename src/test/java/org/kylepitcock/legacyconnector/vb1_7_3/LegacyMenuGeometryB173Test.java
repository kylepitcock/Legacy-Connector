package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.junit.jupiter.api.Test;

class LegacyMenuGeometryB173Test {
    private static final class LateSized extends HandledScreen {
        int draws;
        LateSized(){super(null);}
        @Override public void drawBackground(float partial){draws++;backgroundWidth=256;backgroundHeight=195;}
    }
    @Test void lateOriginalDimensionsAreKnownBeforeNativeSlotOriginIsCaptured(){
        var gui=new LateSized();gui.width=640;gui.height=360;
        assertEquals(176,gui.mcc$backgroundWidth());assertEquals(166,gui.mcc$backgroundHeight());
        var geometry=LegacyMenuGeometryB173.afterDraw(gui,640,360,()->gui.render(0,0,0));
        assertEquals(1,gui.draws);assertEquals(new LegacyMenuGeometryB173(256,195,192,82),geometry);
        // Original slot (48,170) stays unchanged; the native item and hitbox share the drawn panel's origin.
        assertEquals(240,geometry.left()+48);assertEquals(252,geometry.top()+170);
        assertNotEquals((640-176)/2+48,geometry.left()+48);
    }
    @Test void eachFrameDrawsOnceAndOddScreenSizesUseOriginalIntegerCentering(){
        var gui=new LateSized();gui.width=641;gui.height=362;
        var first=LegacyMenuGeometryB173.afterDraw(gui,641,362,()->gui.render(0,0,0));
        var second=LegacyMenuGeometryB173.afterDraw(gui,641,362,()->gui.render(0,0,0));
        assertEquals(2,gui.draws);assertEquals(first,second);
        assertEquals(192,second.left());assertEquals(83,second.top());
    }
}
