package legacyconnector.vb1_7_3.net.minecraft.world.dimension;

public class Dimension {
    public legacyconnector.vb1_7_3.net.minecraft.world.World world;
    public legacyconnector.vb1_7_3.net.minecraft.world.biome.source.BiomeSource biomeSource;
    public boolean evaporatesWater;
    public boolean hasNoSky;
    public boolean hasCeiling,isNether;
    public float[] lightLevelToLuminance=new float[16];
    protected float[] backgroundColor=new float[4];
    public int id;

    public final void setWorld(legacyconnector.vb1_7_3.net.minecraft.world.World world){
        this.world=world;initBiomeSource();initBrightnessTable();
    }
    protected void initBiomeSource(){biomeSource=new legacyconnector.vb1_7_3.net.minecraft.world.biome.source.BiomeSource(world);}
    protected void initBrightnessTable(){
        for(int i=0;i<16;i++){float darkness=1-i/15F;lightLevelToLuminance[i]=(1-darkness)/(darkness*3+1)*.95F+.05F;}
    }
    public legacyconnector.vb1_7_3.net.minecraft.world.chunk.ChunkSource createChunkGenerator(){return null;}
    public boolean isValidSpawnPoint(int x,int z){return world!=null&&world.getTopY(x,z)>0;}
    public float getTimeOfDay(long time,float partial){
        float phase=((time%24000)+partial)/24000F-.25F;
        if(phase<0)phase++;if(phase>1)phase--;
        float linear=phase;phase=1-(float)((Math.cos(phase*Math.PI)+1)/2);return linear+(phase-linear)/3;
    }
    public float[] getBackgroundColor(float time,float partial){return null;}
    public legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d getFogColor(float time,float partial){
        float daylight=Math.max(0,Math.min(1,(float)Math.cos(time*Math.PI*2)*2+.5F));
        return legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d.create(.7529412F*(daylight*.94F+.06F),.8470588F*(daylight*.94F+.06F),daylight*.91F+.09F);
    }
    public boolean hasWorldSpawn(){return true;}
    public boolean hasGround(){return true;}
    public float getCloudHeight(){return 108;}
    public static Dimension fromId(int id){
        var dimension=legacyconnector.vb1_7_3.net.minecraft.src.DimensionBase.getDimByNumber(id);
        return dimension==null?null:dimension.getWorldProvider();
    }
}
