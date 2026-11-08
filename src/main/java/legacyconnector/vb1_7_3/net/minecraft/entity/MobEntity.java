package legacyconnector.vb1_7_3.net.minecraft.entity;

import legacyconnector.vb1_7_3.net.minecraft.entity.ai.pathing.Path;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost;

/** Beta creature superclass shared by animals, hostile mobs and water creatures. */
public abstract class MobEntity extends LivingEntity {
    public Entity target;
    protected Path path;
    protected boolean movementBlocked;
    public MobEntity(World world){super(world);}
    @Override protected void tickLiving(){if(mcc$modern instanceof B173LivingHost host)host.baseAI();}
    protected boolean isMovementBlocked(){return false;}
    public final boolean mcc$isMovementBlocked(){return isMovementBlocked();}
    public void mcc$setMovementBlocked(boolean blocked){movementBlocked=blocked;}
    protected void pathingUpdate(){
        float best=-Float.MAX_VALUE;int bestX=0,bestY=0,bestZ=0;boolean found=false;
        for(int i=0;i<10;i++){int px=(int)Math.floor(x+random.nextInt(13)-6),py=(int)Math.floor(y+random.nextInt(7)-3),pz=(int)Math.floor(z+random.nextInt(13)-6);float favor=getPathfindingFavor(px,py,pz);if(favor>best){best=favor;bestX=px;bestY=py;bestZ=pz;found=true;}}
        if(found)setPath(world.findPath(this,bestX,bestY,bestZ,10));
    }
    public final void mcc$pathingUpdate(){pathingUpdate();}
    protected Entity getTargetInRange(){return null;}
    protected void attack(Entity target,float distance){}
    protected void resetAttack(Entity target,float distance){}
    protected float getPathfindingFavor(int x,int y,int z){return 0;}
    public final Entity mcc$targetInRange(){return getTargetInRange();}
    public final void mcc$attack(Entity entity,float distance){attack(entity,distance);}
    public final void mcc$resetAttack(Entity entity,float distance){resetAttack(entity,distance);}
    public final float mcc$pathFavor(int x,int y,int z){return getPathfindingFavor(x,y,z);}
    @Override public boolean canSpawn(){return super.canSpawn()&&getPathfindingFavor((int)Math.floor(x),(int)Math.floor(boundingBox.minY),(int)Math.floor(z))>=0;}
    public void setPath(Path path){this.path=path;if(mcc$modern instanceof B173LivingHost host)host.baseSetPath(path);}
    public boolean hasPath(){return path!=null&&!path.isFinished();}
    public Path mcc$path(){return path;}
    public Entity getTarget(){return target;}
    public void setTarget(Entity target){this.target=target;}
}
