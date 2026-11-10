package legacyconnector.vb1_7_3.net.minecraft.src;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;

/** PlayerAPI's client player extension. Boolean lifecycle results indicate replacement of vanilla behavior. */
public class PlayerBase {
    public final ClientPlayerEntity player;
    public PlayerBase(ClientPlayerEntity player){this.player=player;}
    public void playerInit(){}
    public boolean onLivingUpdate(){return false;}
    public boolean writeEntityToNBT(NbtCompound tag){return false;}
    public boolean readEntityFromNBT(NbtCompound tag){return false;}
    public boolean setEntityDead(){return false;}
    public boolean heal(int amount){return false;}
    public boolean attackEntityFrom(Entity source,int amount){return false;}
    public double getDistanceSq(double x,double y,double z,double distance){return distance;}
    public boolean isInWater(boolean value){return value;}
    public float getCurrentPlayerStrVsBlock(Block block,float strength){return strength;}
}
