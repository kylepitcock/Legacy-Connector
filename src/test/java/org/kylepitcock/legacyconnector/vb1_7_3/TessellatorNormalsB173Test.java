package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator;
import legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class TessellatorNormalsB173Test {
    private record Draw(List<RendererContextB173.Vertex> vertices,Matrix4f matrix){}
    private static RendererContextB173.Scope open(List<Draw> draws){
        return RendererContextB173.open(path->ResourceLocation.withDefaultNamespace("missingno"),(draw,texture)->fail(),new RendererContextB173.Extras(){
            @Override public void quad(List<RendererContextB173.Vertex> vertices,Matrix4f transform,ResourceLocation texture,RendererContextB173.Style style){draws.add(new Draw(vertices,transform));}
        });
    }
    private static void quad(){var t=Tessellator.INSTANCE;t.vertex(0,0,0);t.vertex(1,0,0);t.vertex(1,1,0);t.vertex(0,1,0);}
    private static void normal(RendererContextB173.Vertex vertex,float x,float y,float z){assertEquals(x,vertex.nx());assertEquals(y,vertex.ny());assertEquals(z,vertex.nz());}
    @Test void absentNormalArrayUsesCurrentGlNormalAtDrawTimeAndDefaultsToPositiveZ(){
        var draws=new ArrayList<Draw>();try(var scope=open(draws)){
            var t=Tessellator.INSTANCE;t.startQuads();quad();t.draw();
            for(var vertex:draws.getFirst().vertices())normal(vertex,0,0,1);
            GL11.glNormal3f(-.25F,0,0);t.startQuads();quad();GL11.glNormal3f(.5F,0,0);t.draw();
            for(var vertex:draws.getLast().vertices())normal(vertex,.5F,0,0);
            t.startQuads();quad();GL11.glNormal3f(0,0,-1);t.draw();
            for(var vertex:draws.getLast().vertices())normal(vertex,0,0,-1);
        }
    }
    @Test void explicitTessellatorNormalsAreCapturedPerVertexAndNeverReplaceGlCurrentNormal(){
        var draws=new ArrayList<Draw>();try(var scope=open(draws)){
            var t=Tessellator.INSTANCE;GL11.glNormal3f(0,0,-1);t.startQuads();
            t.normal(1,0,0);t.vertex(0,0,0);t.vertex(1,0,0);t.normal(0,1,0);t.vertex(1,1,0);t.vertex(0,1,0);
            GL11.glNormal3f(-1,0,0);t.draw();var vertices=draws.getFirst().vertices();
            normal(vertices.get(0),1,0,0);normal(vertices.get(1),1,0,0);normal(vertices.get(2),0,1,0);normal(vertices.get(3),0,1,0);
            t.startQuads();quad();t.draw();for(var vertex:draws.getLast().vertices())normal(vertex,-1,0,0);
        }
    }
    @Test void currentAttributesRestoreWithCurrentBitButMatricesNeverSaveTheNormal(){
        var draws=new ArrayList<Draw>();try(var scope=open(draws)){
            GL11.glNormal3f(1,0,0);GL11.glPushMatrix();GL11.glNormal3f(0,1,0);GL11.glPopMatrix();
            assertEquals(new RendererContextB173.Normal(0,1,0),RendererContextB173.normal());
            GL11.glColor4f(1,0,0,1);GL11.glPushAttrib(1);GL11.glNormal3f(0,0,-1);GL11.glColor4f(0,1,0,1);GL11.glTranslatef(3,4,5);GL11.glPopAttrib();
            assertEquals(new RendererContextB173.Normal(0,1,0),RendererContextB173.normal());assertEquals(0xffff0000,RendererContextB173.argb());
            assertEquals(new Vector3f(3,4,5),RendererContextB173.matrix().getTranslation(new Vector3f()));
            GL11.glPushAttrib(8192);GL11.glNormal3f(1,0,0);GL11.glPopAttrib();assertEquals(new RendererContextB173.Normal(1,0,0),RendererContextB173.normal());
            try(var nested=open(new ArrayList<>())){assertEquals(new RendererContextB173.Normal(0,0,1),RendererContextB173.normal());GL11.glNormal3f(0,-1,0);}
            assertEquals(new RendererContextB173.Normal(1,0,0),RendererContextB173.normal());
        }
    }
    @Test void normalStaysObjectSpaceUntilDrawAndUsesInverseTransposeInsteadOfPositionTransform(){
        var draws=new ArrayList<Draw>();try(var scope=open(draws)){
            GL11.glNormal3f(1,0,0);GL11.glRotatef(90,0,0,1);GL11.glScalef(2,1,3);GL11.glTranslatef(17,18,19);
            Tessellator.INSTANCE.startQuads();quad();Tessellator.INSTANCE.draw();
        }
        var draw=draws.getFirst();normal(draw.vertices().getFirst(),1,0,0);
        var transformed=draw.matrix().normal(new Matrix3f()).transform(new Vector3f(1,0,0));
        assertEquals(0,transformed.x,1E-6);assertEquals(.5F,transformed.y,1E-6);assertEquals(0,transformed.z,1E-6);
    }
}
