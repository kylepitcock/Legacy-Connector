package org.kylepitcock.legacyconnector.vb1_7_3.client;

import java.util.function.Function;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173;

/** Local sprite UVs must address the stitched, animated frame rather than its source PNG strip. */
final class SpriteQuadBindingB173 {
    record Bounds(float u0,float u1,float v0,float v1){}
    record Binding(ResourceLocation texture,Matrix4f matrix){}
    private SpriteQuadBindingB173(){}
    static boolean normalized(java.util.List<RendererContextB173.Vertex> vertices,Matrix4f matrix){
        var uv=new Vector4f();
        for(var vertex:vertices){
            // Match RenderContext.textured: float inputs, homogeneous transform, no perspective divide.
            matrix.transform(uv.set((float)vertex.u(),(float)vertex.v(),0,1));
            if(!Float.isFinite(uv.x)||!Float.isFinite(uv.y)||uv.x<0||uv.x>1||uv.y<0||uv.y>1)return false;
        }
        return true;
    }
    static Binding resolve(ResourceLocation texture,Matrix4f legacyMatrix,boolean textured,Function<ResourceLocation,Bounds> sprites){
        String path=texture.getPath();
        if(textured&&path.endsWith(".png")&&(path.startsWith("textures/block/")||path.startsWith("textures/item/"))){
            var id=ResourceLocation.fromNamespaceAndPath(texture.getNamespace(),path.substring(9,path.length()-4));
            var bounds=sprites.apply(id);
            if(bounds!=null){
                // Column vectors: transform old local UVs first, then place them in the atlas cell.
                var matrix=new Matrix4f().translation(bounds.u0,bounds.v0,0)
                        .scale(bounds.u1-bounds.u0,bounds.v1-bounds.v0,1).mul(legacyMatrix);
                return new Binding(TextureAtlas.LOCATION_BLOCKS,matrix);
            }
        }
        return new Binding(texture,new Matrix4f(legacyMatrix));
    }
}
