package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.nbt.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class GameHookCanvasB173Test {
    private static CompoundTag frame(String... operations){
        var frame=new CompoundTag();var commands=new ListTag();for(String operation:operations){var command=new CompoundTag();command.putString("op",operation);commands.add(command);}frame.put("commands",commands);return frame;
    }
    @Test void canvasScalesPrimitivePositionsAndNestedTranslationsExactlyOnce(){
        // Beta minimum GUI height keeps this window at 640x360; the native GUI may use 320x180.
        var scale=GameHookCanvasB173.scale(640,360,320,180);assertEquals(.5F,scale.x());assertEquals(.5F,scale.y());
        var canvas=new Matrix4f().scale(scale.x(),scale.y(),1);
        assertEquals(new Vector3f(5,10,0),canvas.transformPosition(new Vector3f(10,20,0)),"blit/fill/quad positions share the canvas");
        var translated=new Matrix4f(canvas).translate(20,30,0);
        assertEquals(new Vector3f(15,25,0),translated.transformPosition(new Vector3f(10,20,0)),"captured translation and text coordinates scale together");
        assertEquals(new Vector3f(10,15,0),translated.transformPosition(new Vector3f(0,0,0)),"translation is not left in native coordinates");
    }
    @Test void rejectsUnderflowUnclosedAndOversizedPoseCommandsBeforeReplay(){
        assertTrue(GameHookCanvasB173.balanced(frame()));assertTrue(GameHookCanvasB173.balanced(frame("push","fill","push","text","pop","quad","pop")));
        assertFalse(GameHookCanvasB173.balanced(frame("pop")));assertFalse(GameHookCanvasB173.balanced(frame("push","blit")));
        assertFalse(GameHookCanvasB173.balanced(frame("pop","push")));
        String[] oversized=new String[GameHookHudB173.MAX_COMMANDS+1];java.util.Arrays.fill(oversized,"fill");assertFalse(GameHookCanvasB173.balanced(frame(oversized)));
    }
    @Test void absentCanvasDimensionsRemainFinite(){
        var scale=GameHookCanvasB173.scale(0,-1,320,180);assertEquals(320,scale.x());assertEquals(180,scale.y());assertTrue(Float.isFinite(scale.x()+scale.y()));
    }
}
