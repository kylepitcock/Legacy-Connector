package legacyconnector.vb1_7_3.net.minecraft.src;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

public interface IInterceptBlockSet {
    boolean canIntercept(World world,Loc position,int blockId);
    int intercept(World world,Loc position,int blockId);
}
