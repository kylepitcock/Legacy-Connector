package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.SlimeEntityModel;
import legacyconnector.vb1_7_3.net.minecraft.entity.passive.SheepEntity;
import org.junit.jupiter.api.Test;

class SlimeModelB173Test {
    @Test void outerAndInnerModelsCaptureOriginalShellBodyEyesAndMouth(){
        var outer=new java.util.ArrayList<ModelGeometryB173.Draw>();
        ModelGeometryB173.capture(outer::add,()->new SlimeEntityModel(0).render(0,0,0,0,0,1F/16));
        assertEquals(1,outer.size());var shell=outer.getFirst().cuboids().getFirst();assertEquals(8,shell.width());assertEquals(-4,shell.x());assertEquals(16,shell.y());
        var inner=new java.util.ArrayList<ModelGeometryB173.Draw>();
        ModelGeometryB173.capture(inner::add,()->new SlimeEntityModel(16).render(0,0,0,0,0,1F/16));
        assertEquals(4,inner.size());var body=inner.getFirst().cuboids().getFirst();assertEquals(6,body.width());assertEquals(16,body.v());
        var right=inner.get(1).cuboids().getFirst();assertEquals(-3.25F,right.x());assertEquals(32,right.u());
        var left=inner.get(2).cuboids().getFirst();assertEquals(1.25F,left.x());assertEquals(4,left.v());
        var mouth=inner.get(3).cuboids().getFirst();assertEquals(1,mouth.width());assertEquals(21,mouth.y());assertEquals(8,mouth.v());
    }
    @Test void sheepPaletteRetainsAll16OriginalColorSlots(){
        assertEquals(16,SheepEntity.COLORS.length);
        for(var color:SheepEntity.COLORS){assertEquals(3,color.length);for(float channel:color)assertTrue(channel>=0&&channel<=1);}
        assertArrayEquals(new float[]{1,1,1},SheepEntity.COLORS[0]);assertArrayEquals(new float[]{.5F,.4F,.3F},SheepEntity.COLORS[12]);
        assertArrayEquals(new float[]{.8F,.3F,.3F},SheepEntity.COLORS[14]);assertArrayEquals(new float[]{.1F,.1F,.1F},SheepEntity.COLORS[15]);
    }
}
