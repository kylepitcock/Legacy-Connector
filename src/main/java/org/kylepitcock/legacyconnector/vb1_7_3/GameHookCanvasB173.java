package org.kylepitcock.legacyconnector.vb1_7_3;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** One coordinate system and a balanced pose stack for an entire remote original HUD frame. */
public final class GameHookCanvasB173 {
    public record Scale(float x,float y){}
    private GameHookCanvasB173(){}
    public static Scale scale(int sourceWidth,int sourceHeight,int targetWidth,int targetHeight){
        return new Scale((float)Math.max(1,targetWidth)/Math.max(1,sourceWidth),(float)Math.max(1,targetHeight)/Math.max(1,sourceHeight));
    }
    public static boolean balanced(CompoundTag frame){
        var commands=frame.getList("commands",Tag.TAG_COMPOUND);if(commands.size()>GameHookHudB173.MAX_COMMANDS)return false;
        int depth=0;
        for(Tag raw:commands){String operation=((CompoundTag)raw).getString("op");
            if(operation.equals("push"))depth++;
            else if(operation.equals("pop")&&--depth<0)return false;
        }
        return depth==0;
    }
}
