package legacyconnector.vb1_7_3.net.minecraft.entity;

import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost;

/** Beta creature callbacks, with original target/attack decisions retained by the neutral host. */
public abstract class PathAwareEntity extends LivingEntity {
    public Entity target;
    public PathAwareEntity(World world){super(world);}
    @Override protected void tickLiving(){if(mcc$modern instanceof B173LivingHost host)host.baseAI();}
    protected Entity getTargetInRange(){return null;}
    protected void attack(Entity target,float distance){}
    protected float getPathfindingFavor(int x,int y,int z){return 0;}
    public final Entity mcc$targetInRange(){return getTargetInRange();}
    public final void mcc$attack(Entity entity,float distance){attack(entity,distance);}
    public final float mcc$pathFavor(int x,int y,int z){return getPathfindingFavor(x,y,z);}
    public boolean canSpawn(){return mcc$modern instanceof B173LivingHost host&&host.baseCanSpawn();}
    protected boolean canDespawn(){return true;}
    public int getLimitPerChunk(){return 4;}
}
