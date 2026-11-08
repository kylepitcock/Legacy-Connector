package org.kylepitcock.legacyconnector.vb1_7_3.client;

import com.mojang.blaze3d.vertex.PoseStack;
import org.kylepitcock.legacyconnector.vb1_7_3.B173EntityHost;
import org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173;

/** Beta bw projects shadows from legacy y + x_(), without moving the model or ground vertices. */
public final class ShadowOffsetB173 {
    private static final ThreadLocal<Double> CURRENT=ThreadLocal.withInitial(()->0D);
    private ShadowOffsetB173(){}
    public static double offset(legacyconnector.vb1_7_3.net.minecraft.entity.Entity original){
        try(var readOnly=EntityViewsB173.readOnly()){
            return (double)original.standingEyeHeight+original.getShadowRadius();
        }
    }
    public static double sourceY(double nativeY){return nativeY+CURRENT.get();}
    public static void render(net.minecraft.world.entity.Entity entity,PoseStack pose,Runnable draw){
        if(!(entity instanceof B173EntityHost host)||host.legacy()==null){draw.run();return;}
        double shift=offset(host.legacy()),previous=CURRENT.get();
        pose.pushPose();CURRENT.set(shift);
        try{pose.translate(0,shift,0);draw.run();}
        finally{CURRENT.set(previous);pose.popPose();}
    }
}
