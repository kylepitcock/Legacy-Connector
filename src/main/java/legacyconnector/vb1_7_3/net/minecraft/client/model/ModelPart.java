package legacyconnector.vb1_7_3.net.minecraft.client.model;

import java.util.ArrayList;
import java.util.List;
import org.kylepitcock.legacyconnector.vb1_7_3.ModelGeometryB173;

/** Beta model part, preserving original model construction and animation fields. */
public class ModelPart {
    public float pivotX,pivotY,pivotZ,pitch,yaw,roll;
    public boolean mirror,hidden;
    public boolean visible=true;
    private final int u,v;
    private final List<ModelGeometryB173.Cuboid> cuboids=new ArrayList<>();
    public ModelPart(){this(0,0);}
    public ModelPart(int u,int v){this.u=u;this.v=v;}
    public void addCuboid(float x,float y,float z,int width,int height,int depth){addCuboid(x,y,z,width,height,depth,0);}
    public void addCuboid(float x,float y,float z,int width,int height,int depth,float grow){cuboids.clear();cuboids.add(new ModelGeometryB173.Cuboid(u,v,x,y,z,width,height,depth,grow,mirror));}
    public void setPivot(float x,float y,float z){pivotX=x;pivotY=y;pivotZ=z;}
    public List<ModelGeometryB173.Cuboid> mcc$cuboids(){return List.copyOf(cuboids);}
    public void render(float scale){draw(scale,false);}
    public void renderForceTransform(float scale){draw(scale,true);}
    private void draw(float scale,boolean force){
        if(hidden||!visible)return;
        var matrix=ModelGeometryB173.transform(pivotX,pivotY,pivotZ,pitch,yaw,roll,scale,force);
        if(org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173.active()){
            org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173.draw(new ModelGeometryB173.Draw(mcc$cuboids(),matrix,scale));return;
        }
        var draw=new ModelGeometryB173.Draw(mcc$cuboids(),matrix,scale);
        if(ModelGeometryB173.emit(draw))return;
        if(net.neoforged.fml.loading.FMLEnvironment.dist.isClient())org.kylepitcock.legacyconnector.vb1_7_3.client.ModelPartsB173.draw(draw);
    }
    public void transform(float scale){
        if(hidden||!visible)return;
        if(org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173.active()){
            org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173.transform(ModelGeometryB173.transform(pivotX,pivotY,pivotZ,pitch,yaw,roll,scale,false));return;
        }
        if(net.neoforged.fml.loading.FMLEnvironment.dist.isClient())org.kylepitcock.legacyconnector.vb1_7_3.client.ModelPartsB173.transform(ModelGeometryB173.transform(pivotX,pivotY,pivotZ,pitch,yaw,roll,scale,false));
    }
}
