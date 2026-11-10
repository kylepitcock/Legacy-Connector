package legacyconnector.vb1_7_3.net.minecraft.src;

import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.vb1_7_3.BlockInterceptionB173;

/** Shared registrations for original block-write and reach query callbacks. */
public final class SAPI {
    public SAPI(){}
    public static void interceptAdd(IInterceptBlockSet interceptor){BlockInterceptionB173.register(interceptor);}
    public static int interceptBlockSet(World world,Loc position,int id){return BlockInterceptionB173.intercept(world,position,id);}
    /** Original server-side drop helper emits one independent pickup per input item. */
    public static void drop(World world,Loc position,legacyconnector.vb1_7_3.net.minecraft.item.ItemStack stack){
        if(world.isRemote)return;
        for(int i=0;i<stack.count;i++){
            float spread=.7F;
            double dx=world.random.nextFloat()*spread+(1-spread)*.5D;
            double dy=world.random.nextFloat()*spread+(1-spread)*.5D;
            double dz=world.random.nextFloat()*spread+(1-spread)*.5D;
            var copy=stack.copy();copy.count=1;
            var item=new legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity(world,position.x()+dx,position.y()+dy,position.z()+dz,copy);
            item.pickupDelay=10;world.spawnEntity(item);
        }
    }
    public static void reachAdd(IReach reach){org.kylepitcock.legacyconnector.vb1_7_3.ReachB173.register(reach);}
    public static void reachAdd(IReachBlock reach){org.kylepitcock.legacyconnector.vb1_7_3.ReachB173.register(reach);}
    public static void reachAdd(IReachEntity reach){org.kylepitcock.legacyconnector.vb1_7_3.ReachB173.register(reach);}
    private static legacyconnector.vb1_7_3.net.minecraft.item.ItemStack held(){
        var mc=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance();
        var player=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext();
        if(player==null)player=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$get$player(mc);
        return player==null?null:player.inventory.getSelectedItem();
    }
    public static float reachGet(){return org.kylepitcock.legacyconnector.vb1_7_3.ReachB173.query(held(),true);}
    public static float reachGetBlock(){return org.kylepitcock.legacyconnector.vb1_7_3.ReachB173.query(held(),true);}
    public static float reachGetEntity(){return org.kylepitcock.legacyconnector.vb1_7_3.ReachB173.query(held(),false);}
}
