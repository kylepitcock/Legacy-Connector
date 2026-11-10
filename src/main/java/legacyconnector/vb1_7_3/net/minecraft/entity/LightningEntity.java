package legacyconnector.vb1_7_3.net.minecraft.entity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
/** Weather baseline is entered only by an original super.tick call. */
public class LightningEntity extends Entity {
    public long seed;
    public LightningEntity(World world,double x,double y,double z){super(world);setPosition(x,y,z);seed=random.nextLong();}
    @Override public void tick(){if(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Lightning host)host.mcc$baseTick();else super.tick();}
}
