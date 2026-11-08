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
    @Test void bothSheetPathSpellingsResolveOriginalFireTiles(){
        for(String path:List.of("/terrain.png","terrain.png")){
            var texture=EntityRenderingB173.texture(null,path);
            var q=EntityRenderingB173.resolveQuad(List.of(v(15./16,1./16),v(1,1./16),v(1,2./16),v(15./16,2./16)),texture);
            assertEquals(ResourceLocation.withDefaultNamespace("textures/block/fire_0.png"),q.texture());
            assertEquals(0,q.vertices().get(0).u());assertEquals(1,q.vertices().get(1).u());assertEquals(1,q.vertices().get(2).v());
        }
        assertEquals(ResourceLocation.withDefaultNamespace("textures/block/fire_1.png"),AssetsB173.particleSprite(1,47));
    }
    @Test void originalDefaultBlockTextureMetadataIsComplete()throws Exception{
        var ids=new java.util.HashSet<Integer>();
        try(var in=getClass().getResourceAsStream("/legacyconnector/b1.7.3/vanilla-block-texture-indices.tsv")){
            assertNotNull(in);
            for(String line:new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split("\n")){
                if(line.isBlank()||line.startsWith("#"))continue;
                var fields=line.trim().split("\t");assertTrue(ids.add(Integer.parseInt(fields[0])));
                assertEquals(Integer.parseInt(fields[1]),VanillaB173.textureIndex(Integer.parseInt(fields[0])));
            }
        }
        assertEquals(java.util.stream.IntStream.rangeClosed(1,96).boxed().collect(java.util.stream.Collectors.toSet()),ids);
        for(int id=1;id<=96;id++)assertTrue(VanillaB173.textureIndex(id)>=0&&VanillaB173.textureIndex(id)<256);
        assertEquals(1,VanillaB173.textureIndex(1));assertEquals(31,VanillaB173.textureIndex(51));
    }
}
