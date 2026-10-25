package legacyconnector.v1_12_2.net.minecraftforge.fml.common.asm.transformers.deobf;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.commons.Remapper;

/**
 * The SRG name tables of the Forge runtime. Translated mods already use MCP names, so every table is empty and every mapping is the
 * identity; mods that read the tables (CodeChickenLib's ObfMapping) find them present.
 */
public class FMLDeobfuscatingRemapper extends Remapper {
    public static final FMLDeobfuscatingRemapper INSTANCE = new FMLDeobfuscatingRemapper();

    private final Map<String, Map<String, String>> rawFieldMaps = new HashMap<>();
    private final Map<String, Map<String, String>> rawMethodMaps = new HashMap<>();
    private final Map<String, Map<String, String>> fieldNameMaps = new HashMap<>();
    private final Map<String, Map<String, String>> methodNameMaps = new HashMap<>();
    private final Set<String> classNameBiMap = new HashSet<>();

    public void setupLoadOnly(String deobfFileName, boolean loadAll) {
    }

    public void setup(java.io.File mcDir, Object classLoader, String deobfFileName) {
    }

    public boolean isRemappedClass(String className) {
        return false;
    }

    public String getStaticFieldType(String oldType, String fieldName, String newType, String newFieldName) {
        return null;
    }

    public Set<String> getObfedClasses() {
        return Collections.emptySet();
    }

    @Override
    public String map(String typeName) {
        return typeName;
    }

    public String unmap(String typeName) {
        return typeName;
    }

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        return name;
    }

    @Override
    public String mapMethodName(String owner, String name, String desc) {
        return name;
    }

    public String mapMethodDesc(String desc) {
        return desc;
    }

    public String getFieldType(String owner, String name) {
        return null;
    }
}
