package org.kylepitcock.legacyconnector.vb1_7_3.client;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.kylepitcock.legacyconnector.vb1_7_3.ModelGeometryB173;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/** Beta UVs differ from modern cubes; share the established quad emitter, using exact common Beta geometry. */
public final class ModelPartsB173 {
    private ModelPartsB173() {}
    public static void transform(Matrix4f matrix){var pose=RenderContext.pose();if(pose!=null)pose.mulPose(matrix);}
    public static void draw(ModelGeometryB173.Draw draw){
        var pose=RenderContext.pose();if(pose==null)return;
        pose.pushPose();
        try {
            pose.mulPose(draw.transform());
            float scale=draw.scale();
            for(var box:draw.cuboids())for(var face:ModelGeometryB173.vertices(box)){
                var a=face.get(0);var b=face.get(1);var c=face.get(2);
                Vector3f normal=new Vector3f(b.x()-a.x(),b.y()-a.y(),b.z()-a.z()).cross(c.x()-a.x(),c.y()-a.y(),c.z()-a.z());
                if(normal.lengthSquared()>0)normal.normalize();
                double[][] vertices=new double[4][7];
                for(int i=0;i<4;i++){var v=face.get(i);vertices[i]=new double[]{v.x()*scale,v.y()*scale,v.z()*scale,v.u(),v.v(),RenderContext.argb(),RenderContext.vertexLight()};}
                RenderContext.quad(vertices,normal.x,normal.y,normal.z);
            }
        } finally {pose.popPose();}
    }
}
