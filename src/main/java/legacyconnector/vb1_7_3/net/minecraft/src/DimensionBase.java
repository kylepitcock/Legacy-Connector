package legacyconnector.vb1_7_3.net.minecraft.src;

import java.util.ArrayList;
import java.util.LinkedList;
import legacyconnector.vb1_7_3.net.minecraft.world.dimension.Dimension;
import legacyconnector.vb1_7_3.net.minecraft.world.dimension.PortalForcer;
import org.kylepitcock.legacyconnector.vb1_7_3.DimensionsB173;

/** Shared dimension registrations and original travel entrypoints. */
public class DimensionBase {
    public static ArrayList<DimensionBase> list=new ArrayList<>();
    public static LinkedList<Integer> order=new LinkedList<>();
    public final int number;
    public final Class<? extends Dimension> worldProvider;
    public final Class<? extends PortalForcer> teleporter;
    public String name="Dimension",soundTrigger="portal.trigger",soundTravel="portal.travel";
    static {
        var overworld=new DimensionBase(0,legacyconnector.vb1_7_3.net.minecraft.world.dimension.OverworldDimension.class,null);overworld.name="Overworld";
        var nether=new DimensionBase(-1,legacyconnector.vb1_7_3.net.minecraft.world.dimension.NetherDimension.class,PortalForcer.class){
            @Override public Loc getDistanceScale(Loc position,boolean leaving){double scale=leaving?8:.125;return position.multiply(scale,1,scale);}
        };nether.name="Nether";
    }
    public DimensionBase(int number,Class<? extends Dimension> provider,Class<? extends PortalForcer> teleporter){
        this.number=number;this.worldProvider=provider;this.teleporter=teleporter;
        list.add(this);DimensionsB173.register(this);
    }
    public static DimensionBase getDimByNumber(int number){for(var value:list)if(value.number==number)return value;return null;}
    public static DimensionBase getDimByProvider(Class<? extends Dimension> provider){for(var value:list)if(value.worldProvider==provider)return value;return null;}
    public Dimension getWorldProvider(){return instantiate(worldProvider);}
    public PortalForcer getTeleporter(){return instantiate(teleporter);}
    private static <T> T instantiate(Class<? extends T> type){
        if(type==null)return null;
        try{
            var constructor=type.getDeclaredConstructor();
            if(!constructor.canAccess(null))constructor.setAccessible(true);
            return constructor.newInstance();
        }
        catch(java.lang.reflect.InvocationTargetException error){
            if(error.getCause() instanceof RuntimeException cause)throw cause;
            if(error.getCause() instanceof Error cause)throw cause;
            throw new IllegalStateException("Dimension factory failed",error.getCause());
        }catch(ReflectiveOperationException error){return null;}
    }
    public Loc getDistanceScale(Loc position,boolean leaving){return position;}
    public static void usePortal(int number){DimensionsB173.usePortal(number);}
    public static void respawn(boolean force,int number){DimensionsB173.respawn(force,number);}
}
