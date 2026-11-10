package legacyconnector.vb1_7_3.net.minecraft.client.render.entity;

import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LightningEntity;
import legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator;
import org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173;

/** Beta seeded eight-segment bolt and its two shorter branches, in four translucent shells. */
public class LightningEntityRenderer extends EntityRenderer {
    @Override public void render(Entity entity,double x,double y,double z,float yaw,float partial){render((LightningEntity)entity,x,y,z,yaw,partial);}
    public void render(LightningEntity entity,double x,double y,double z,float yaw,float partial){
        RendererContextB173.enable(3553,false);RendererContextB173.enable(2896,false);
        RendererContextB173.enable(3042,true);RendererContextB173.blendFunc(770,1);
        try{
            var t=Tessellator.INSTANCE;double[] xs=new double[8],zs=new double[8];double dx=0,dz=0;
            var shape=new Random(entity.seed);
            for(int i=7;i>=0;i--){xs[i]=dx;zs[i]=dz;dx+=shape.nextInt(11)-5;dz+=shape.nextInt(11)-5;}
            for(int shell=0;shell<4;shell++){
                var random=new Random(entity.seed);
                for(int branch=0;branch<3;branch++){
                    int top=branch==0?7:7-branch,bottom=branch==0?0:top-2;
                    double xx=xs[top]-dx,zz=zs[top]-dz;
                    for(int segment=top;segment>=bottom;segment--){
                        double previousX=xx,previousZ=zz;int spread=branch==0?11:31;
                        xx+=random.nextInt(spread)-(spread-1)/2;zz+=random.nextInt(spread)-(spread-1)/2;
                        double lower=.1+shell*.2,upper=lower;
                        if(branch==0){lower*=segment*.1+1;upper*=(segment-1)*.1+1;}
                        t.start(5);t.color(.45F,.45F,.5F,.3F);
                        for(int side=0;side<5;side++){
                            double lx=x+.5-lower,lz=z+.5-lower,ux=x+.5-upper,uz=z+.5-upper;
                            if(side==1||side==2){lx+=2*lower;ux+=2*upper;}
                            if(side==2||side==3){lz+=2*lower;uz+=2*upper;}
                            t.vertex(ux+xx,y+segment*16,uz+zz);
                            t.vertex(lx+previousX,y+(segment+1)*16,lz+previousZ);
                        }t.draw();
                    }
                }
            }
        }finally{RendererContextB173.enable(3042,false);RendererContextB173.enable(2896,true);RendererContextB173.enable(3553,true);}
    }
}
