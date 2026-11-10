package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen;

/** Original backgrounds may choose their dimensions while drawing, after the menu-open packet was written. */
public record LegacyMenuGeometryB173(int width,int height,int left,int top) {
    public static LegacyMenuGeometryB173 afterDraw(HandledScreen original,int screenWidth,int screenHeight,Runnable draw){
        draw.run();
        int width=original.mcc$backgroundWidth(),height=original.mcc$backgroundHeight();
        return new LegacyMenuGeometryB173(width,height,(screenWidth-width)/2,(screenHeight-height)/2);
    }
}
