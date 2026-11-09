package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import legacyconnector.vb1_7_3.net.minecraft.entity.ai.pathing.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import org.junit.jupiter.api.Test;

class PathB173Test {
    private static net.minecraft.world.level.pathfinder.Path nativePath(boolean reached,Node... nodes){
        return new net.minecraft.world.level.pathfinder.Path(List.of(nodes),new BlockPos(8,64,0),reached);
    }
    @Test void unreachedStartOnlyMeansNoLegacyPath(){
        var nativePath=nativePath(false,new Node(0,64,0));
        assertNull(Path.mcc$wrap(nativePath));assertEquals(0,nativePath.getNextNodeIndex());
        assertNull(Path.mcc$wrap(nativePath),"reusing the native result must retain Beta null semantics");
    }
    @Test void reachedSingletonRemainsAValidPath(){
        var nativePath=nativePath(true,new Node(8,64,0));var wrapped=Path.mcc$wrap(nativePath);
        assertNotNull(wrapped);assertEquals(1,wrapped.length);assertEquals(8,wrapped.getEnd().x);assertFalse(wrapped.isFinished());
        assertSame(nativePath,wrapped.mcc$modern());
    }
    @Test void longerPartialPathRetainsItsCursorAndIdentity(){
        var nativePath=nativePath(false,new Node(0,64,0),new Node(1,64,0),new Node(2,64,0));nativePath.setNextNodeIndex(1);
        var wrapped=Path.mcc$wrap(nativePath);assertNotNull(wrapped);assertEquals(3,wrapped.length);assertEquals(1,wrapped.currentNodeIndex);
        assertFalse(wrapped.isFinished());assertSame(nativePath,wrapped.mcc$modern());assertEquals(1,nativePath.getNextNodeIndex());
        wrapped.next();assertEquals(2,nativePath.getNextNodeIndex());
    }
    @Test void completedNativeCursorIsPreserved(){
        var nativePath=nativePath(true,new Node(0,64,0),new Node(8,64,0));nativePath.setNextNodeIndex(2);
        var wrapped=Path.mcc$wrap(nativePath);assertNotNull(wrapped);assertTrue(wrapped.isFinished());assertEquals(2,wrapped.currentNodeIndex);
        assertSame(nativePath,wrapped.mcc$modern());assertEquals(2,nativePath.getNextNodeIndex());
    }
    @Test void nullAndEmptyNativeResultsHaveNoLegacyRoute(){
        assertNull(Path.mcc$wrap(null));assertNull(Path.mcc$wrap(nativePath(false)));assertNull(Path.mcc$wrap(nativePath(true)));
    }
}
