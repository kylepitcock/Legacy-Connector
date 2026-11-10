package legacyconnector.vb1_7_3.net.minecraft.world.dimension;

public class NetherDimension extends Dimension {
    public NetherDimension(){id=-1;evaporatesWater=true;hasNoSky=true;hasCeiling=true;isNether=true;}
    @Override public boolean hasWorldSpawn(){return false;}
    @Override public boolean hasGround(){return false;}
    @Override public float getTimeOfDay(long time,float partial){return .5F;}
}
