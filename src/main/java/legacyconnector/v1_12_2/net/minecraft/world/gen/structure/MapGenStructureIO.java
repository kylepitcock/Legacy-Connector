package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** The names structures and their pieces are saved under, and their reconstruction from saved data. */
public class MapGenStructureIO {
    private static final Map<String, Class<? extends StructureStart>> START_NAME_TO_CLASS = new ConcurrentHashMap<>();
    private static final Map<Class<? extends StructureStart>, String> START_CLASS_TO_NAME = new ConcurrentHashMap<>();
    private static final Map<String, Class<? extends StructureComponent>> COMPONENT_NAME_TO_CLASS = new ConcurrentHashMap<>();
    private static final Map<Class<? extends StructureComponent>, String> COMPONENT_CLASS_TO_NAME = new ConcurrentHashMap<>();

    static {
        // vanilla's pieces the shim builds (as 1.12's MapGenStructureIO registered them), so saved structures read back
        registerStructure(StructureOceanMonument.StartMonument.class, "Monument");
        ComponentScatteredFeaturePieces.registerScatteredFeaturePieces();
        StructureOceanMonumentPieces.registerOceanMonumentPieces();
        StructureVillagePieces.registerVillagePieces();
        registerStructureComponent(org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernPiece1122.class, "MCCModern");
    }

    public static void registerStructure(Class<? extends StructureStart> startClass, String structureName) {
        if (startClass == null || structureName == null) return;
        START_NAME_TO_CLASS.put(structureName, startClass);
        START_CLASS_TO_NAME.put(startClass, structureName);
    }

    public static void registerStructureComponent(Class<? extends StructureComponent> componentClass, String componentName) {
        if (componentClass == null || componentName == null) return;
        COMPONENT_NAME_TO_CLASS.put(componentName, componentClass);
        COMPONENT_CLASS_TO_NAME.put(componentClass, componentName);
    }

    public static String getStructureStartName(StructureStart start) {
        String name = START_CLASS_TO_NAME.get(start.getClass());
        return name != null ? name : start.getClass().getName();
    }

    public static String getStructureComponentName(StructureComponent component) {
        String name = COMPONENT_CLASS_TO_NAME.get(component.getClass());
        return name != null ? name : component.getClass().getName();
    }

    public static StructureStart getStructureStart(NBTTagCompound tagCompound, World worldIn) {
        Class<? extends StructureStart> type = START_NAME_TO_CLASS.get(tagCompound.getString("id"));
        StructureStart start = instantiate(type);
        if (start != null) start.readStructureComponentsFromNBT(worldIn, tagCompound);
        return start;
    }

    public static StructureComponent getStructureComponent(NBTTagCompound tagCompound, World worldIn) {
        Class<? extends StructureComponent> type = COMPONENT_NAME_TO_CLASS.get(tagCompound.getString("id"));
        StructureComponent c = instantiate(type);
        if (c != null) c.readStructureBaseNBT(worldIn, tagCompound);
        return c;
    }

    private static <T> T instantiate(Class<T> type) {
        if (type == null) return null;
        try {
            var ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Structures").warn("Cannot create structure part {}: {}", type.getName(), e.toString());
            return null;
        }
    }
}
