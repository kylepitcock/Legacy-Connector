package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.List;
import java.util.function.Consumer;
import org.joml.Matrix4f;

/** Common model data; no client classes or graphics context needed to execute original model callbacks. */
public final class ModelGeometryB173 {
    private ModelGeometryB173() {}
    public record Cuboid(int u,int v,float x,float y,float z,int width,int height,int depth,float grow,boolean mirror) {
        /** Beta's six texture rectangles in pixel coordinates (west, east, down, up, north, south). */
        public List<Uv> faces() {
            int w=width,h=height,d=depth;
            return List.of(new Uv(u+d+w,v+d,u+d+w+d,v+d+h),new Uv(u,v+d,u+d,v+d+h),
                new Uv(u+d,v,u+d+w,v+d),new Uv(u+d+w,v,u+d+w+w,v+d),
                new Uv(u+d,v+d,u+d+w,v+d+h),new Uv(u+d+w+d,v+d,u+d+w+d+w,v+d+h));
        }
    }
    public record Vertex(float x,float y,float z,float u,float v) {}
    public static List<List<Vertex>> vertices(Cuboid c) {
        float x1=c.x-c.grow,y1=c.y-c.grow,z1=c.z-c.grow;
        float x2=c.x+c.width+c.grow,y2=c.y+c.height+c.grow,z2=c.z+c.depth+c.grow;
        if(c.mirror){float t=x1;x1=x2;x2=t;}
        float[][] p={{x1,y1,z1},{x2,y1,z1},{x2,y2,z1},{x1,y2,z1},{x1,y1,z2},{x2,y1,z2},{x2,y2,z2},{x1,y2,z2}};
        int[][] indices={{5,1,2,6},{0,4,7,3},{5,4,0,1},{2,3,7,6},{1,0,3,2},{4,5,6,7}};
        var result=new java.util.ArrayList<List<Vertex>>();
        for(int face=0;face<6;face++){
            Uv uv=c.faces().get(face);float u1=(uv.u1+.1F)/64,u2=(uv.u2-.1F)/64,v1=(uv.v1+.1F)/32,v2=(uv.v2-.1F)/32;
            float[][] tex={{u2,v1},{u1,v1},{u1,v2},{u2,v2}};
            var corners=new java.util.ArrayList<Vertex>();
            for(int i=0;i<4;i++){float[] pos=p[indices[face][i]];corners.add(new Vertex(pos[0],pos[1],pos[2],tex[i][0],tex[i][1]));}
            if(c.mirror)java.util.Collections.reverse(corners);
            result.add(List.copyOf(corners));
        }
        return List.copyOf(result);
    }
    public record Uv(float u1,float v1,float u2,float v2) {}
    public record Draw(List<Cuboid> cuboids,Matrix4f transform,float scale) {}
    private static final ThreadLocal<Consumer<Draw>> CAPTURE=new ThreadLocal<>();
    public static void capture(Consumer<Draw> consumer,Runnable callback) {
        Consumer<Draw> previous=CAPTURE.get();CAPTURE.set(consumer);
        try {callback.run();} finally {if(previous==null)CAPTURE.remove();else CAPTURE.set(previous);}
    }
    public static boolean emit(Draw draw) {
        Consumer<Draw> consumer=CAPTURE.get();if(consumer==null)return false;consumer.accept(draw);return true;
    }
    public static Matrix4f transform(float x,float y,float z,float pitch,float yaw,float roll,float scale,boolean force) {
        Matrix4f m=new Matrix4f().translation(x*scale,y*scale,z*scale);
        return force?m.rotateY(yaw).rotateX(pitch).rotateZ(roll):m.rotateZ(roll).rotateY(yaw).rotateX(pitch);
    }
}
