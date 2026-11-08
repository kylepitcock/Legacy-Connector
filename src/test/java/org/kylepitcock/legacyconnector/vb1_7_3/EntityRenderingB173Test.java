package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class EntityRenderingB173Test {
    private static RendererContextB173.Vertex v(double u,double v){return new RendererContextB173.Vertex(0,0,0,u,v,-1,0,1,0);}
    @Test void remapsAtlasTileAndPreservesCornerOrder(){
        var sheet=ResourceLocation.fromNamespaceAndPath("legacyconnector","b173/sheets/items");
        var q=EntityRenderingB173.resolveQuad(List.of(v(0,0),v(1./16,0),v(1./16,1./16),v(0,1./16)),sheet);
        assertEquals(AssetsB173.particleSprite(2,0),q.texture());
        assertEquals(0,q.vertices().get(0).u());assertEquals(1,q.vertices().get(1).u());
        assertEquals(1,q.vertices().get(2).v());assertEquals(0,q.vertices().get(3).u());
    }
    @Test void rejectsUnimplementedCrossCellSampling(){
        var sheet=ResourceLocation.fromNamespaceAndPath("legacyconnector","b173/sheets/items");
        assertThrows(IllegalArgumentException.class,()->EntityRenderingB173.resolveQuad(List.of(v(0,0),v(2./16,0),v(2./16,1./16),v(0,1./16)),sheet));
    }
}
