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
    public net.minecraft.world.level.Explosion mcc$modern;
    private World world;
    private boolean detonated;
    public Explosion(World world,Entity source,double x,double y,double z,float power,boolean fire,net.minecraft.world.level.Explosion modern){
        this.world=world;this.source=source;this.x=x;this.y=y;this.z=z;this.power=power;this.fire=fire;mcc$modern=modern;detonated=modern!=null;
        if(modern!=null)damagedBlocks.addAll(modern.getToBlow());
    }
    /** Beta's {@code new Explosion(world, entity, x, y, z, size)}: an explosion a mod runs itself with {@link #doExplosionA} and {@link #doExplosionB}. */
    public Explosion(World world,Entity source,double x,double y,double z,float power){
        this(world,source,x,y,z,power,false,null);
    }
    /** Beta's first phase (collecting the blocks and hurting entities): the native explosion does both when it detonates in {@link #doExplosionB}. */
    public void doExplosionA(){}
    /** Beta's second phase (destroying blocks, particles, sound): detonates the native explosion once. */
    public void doExplosionB(boolean particles){
        if(detonated||world==null)return;
        detonated=true;
        var done=world.createExplosion(source,x,y,z,power,fire);
        if(done!=null&&done.mcc$modern!=null){mcc$modern=done.mcc$modern;damagedBlocks.addAll(done.damagedBlocks);}
    }
    /** Beta's {@code playExplosionSound}: the explosion's own sound at its centre. */
    public void playExplosionSound(boolean big){
        if(world!=null)world.playSound(source,"random.explode",4.0F,(1.0F+(world.random.nextFloat()-world.random.nextFloat())*0.2F)*0.7F);
    }
}
