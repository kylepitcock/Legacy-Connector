package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class TessellatorLinesB173Test {
    private record Segment(RendererContextB173.Vertex a,RendererContextB173.Vertex b,Matrix4f transform,RendererContextB173.Style style){}
    private static List<Segment> capture(int mode){
        var lines=new ArrayList<Segment>();
        try(var scope=RendererContextB173.open(path->net.minecraft.resources.ResourceLocation.withDefaultNamespace("missingno"),(draw,texture)->fail("lines must not become model quads"),new RendererContextB173.Extras(){
            @Override public void line(RendererContextB173.Vertex a,RendererContextB173.Vertex b,Matrix4f transform,RendererContextB173.Style style){lines.add(new Segment(a,b,transform,style));}
            @Override public void quad(List<RendererContextB173.Vertex> vertices,Matrix4f transform,net.minecraft.resources.ResourceLocation texture,RendererContextB173.Style style){fail("line primitive must not use placeholder quads");}
        })){
            RendererContextB173.translate(10,20,30);RendererContextB173.enable(3553,false);RendererContextB173.enable(2896,false);
            var t=Tessellator.INSTANCE;t.start(mode);t.setOffset(2,3,4);
            t.color(255,0,0,255);t.vertex(0,0,0);t.color(0,255,0,128);t.vertex(1,1,0);t.color(0,0,255,255);t.vertex(2,0,0);t.vertex(3,1,0);t.draw();
        }
        return lines;
    }
    @Test void lineStripRetainsContinuityEndpointColorsTransformAndUntexturedStyle(){
        var lines=capture(3);assertEquals(3,lines.size());
        for(int i=0;i<2;i++)assertEquals(lines.get(i).b,lines.get(i+1).a);
        assertEquals(0xFFFF0000,lines.getFirst().a.argb());assertEquals(0x8000FF00,lines.getFirst().b.argb());assertEquals(0xFF0000FF,lines.getLast().b.argb());
        var p=lines.getFirst().transform.transformPosition(new Vector3f((float)lines.getFirst().a.x(),(float)lines.getFirst().a.y(),(float)lines.getFirst().a.z()));
        assertEquals(new Vector3f(12,23,34),p);assertFalse(lines.getFirst().style.texture2d());assertFalse(lines.getFirst().style.lighting());
    }
    @Test void independentLinesAndClosedLoopKeepTheirOwnPrimitiveTopology(){
        var pairs=capture(1);assertEquals(2,pairs.size());assertNotEquals(pairs.getFirst().b,pairs.getLast().a);
        var loop=capture(2);assertEquals(4,loop.size());assertEquals(loop.getFirst().a,loop.getLast().b);
    }
}
