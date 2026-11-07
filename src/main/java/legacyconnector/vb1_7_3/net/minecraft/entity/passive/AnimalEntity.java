package legacyconnector.vb1_7_3.net.minecraft.entity.passive;

import legacyconnector.vb1_7_3.net.minecraft.entity.PathAwareEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta animals share creature lifecycle; no modern animal goals or breeding are installed. */
public abstract class AnimalEntity extends PathAwareEntity {
    public AnimalEntity(World world){super(world);}
    @Override protected float getPathfindingFavor(int x,int y,int z){return world.getBlockId(x,y-1,z)==2?10F:world.getLightLevel(x,y,z)-.5F;}
    @Override public boolean canSpawn(){int x=(int)Math.floor(this.x),y=(int)Math.floor(boundingBox.minY),z=(int)Math.floor(this.z);return world.getBlockId(x,y-1,z)==2&&world.getLightLevel(x,y,z)>8&&super.canSpawn();}
}
