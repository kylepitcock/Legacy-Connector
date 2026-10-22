package legacyconnector.v1_7_10.net.minecraft.world.gen.structure;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.slf4j.LoggerFactory;

/** Names for structure and structure piece classes, so saved structures can be read back. */
public class MapGenStructureIO {
    private static final Map<String, Class> field_143040_a = new HashMap<>();
    private static final Map<Class, String> field_143038_b = new HashMap<>();
    private static final Map<String, Class> field_143039_c = new HashMap<>();
    private static final Map<Class, String> field_143037_d = new HashMap<>();

    public static synchronized void registerStructure(Class cls, String name) {
        field_143040_a.put(name, cls);
        field_143038_b.put(cls, name);
    }

    public static synchronized void registerStructureComponent(Class cls, String name) {
        field_143039_c.put(name, cls);
        field_143037_d.put(cls, name);
    }

    public static synchronized String func_143033_a(StructureStart start) {
        return field_143038_b.get(start.getClass());
    }

    public static synchronized String func_143036_a(StructureComponent component) {
        return field_143037_d.get(component.getClass());
    }

    public static StructureStart func_143035_a(NBTTagCompound tag, World world) {
        StructureStart start = null;
        try {
            Class cls;
            synchronized (MapGenStructureIO.class) {
                cls = field_143040_a.get(tag.getString("id"));
            }
            if (cls != null) start = (StructureStart) cls.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | RuntimeException e) {
            LoggerFactory.getLogger("LegacyConnector/Structures").warn("Failed Start with id {}", tag.getString("id"), e);
        }
        if (start != null) start.func_143020_a(world, tag);
        return start;
    }

    public static StructureComponent func_143032_b(NBTTagCompound tag, World world) {
        StructureComponent component = null;
        try {
            Class cls;
            synchronized (MapGenStructureIO.class) {
                cls = field_143039_c.get(tag.getString("id"));
            }
            if (cls != null) {
                var ctor = cls.getDeclaredConstructor();
                ctor.setAccessible(true);
                component = (StructureComponent) ctor.newInstance();
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LoggerFactory.getLogger("LegacyConnector/Structures").warn("Failed Piece with id {}", tag.getString("id"), e);
        }
        if (component != null) component.func_143009_a(world, tag);
        return component;
    }
}
