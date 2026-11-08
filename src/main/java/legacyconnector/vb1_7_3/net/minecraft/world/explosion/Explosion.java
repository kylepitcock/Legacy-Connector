package legacyconnector.vb1_7_3.net.minecraft.world.explosion;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Result of the native explosion engine, retaining the Beta return object's identity and fields. */
public class Explosion {
    public boolean fire;
    public double x,y,z;
    public Entity source;
    public float power;
    public java.util.Set<net.minecraft.core.BlockPos> damagedBlocks=new java.util.HashSet<>();
    public final net.minecraft.world.level.Explosion mcc$modern;
    public Explosion(World world,Entity source,double x,double y,double z,float power,boolean fire,net.minecraft.world.level.Explosion modern){
        this.source=source;this.x=x;this.y=y;this.z=z;this.power=power;this.fire=fire;mcc$modern=modern;
        if(modern!=null)damagedBlocks.addAll(modern.getToBlow());
    }
}
