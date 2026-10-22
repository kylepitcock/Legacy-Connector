package legacyconnector.v1_7_10.cpw.mods.fml.common.asm.transformers.deobf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * FML's SRG -> MCP name map. Translated mods already use deobfuscated names, so every lookup answers with its
 * input; the tables exist (empty) for libraries that read them reflectively to build their own remappers.
 */
public class FMLDeobfuscatingRemapper {
    public static final FMLDeobfuscatingRemapper INSTANCE = new FMLDeobfuscatingRemapper();

    private Map<String, Map<String, String>> rawFieldMaps = new HashMap<>();
    private Map<String, Map<String, String>> rawMethodMaps = new HashMap<>();
    private Map<String, Map<String, String>> fieldNameMaps = new HashMap<>();
    private Map<String, Map<String, String>> methodNameMaps = new HashMap<>();
    private Set<String> negativeCacheMethods = new HashSet<>();
    private Set<String> negativeCacheFields = new HashSet<>();

    public void setup(java.io.File mcDir, Object classLoader, String deobfFileName) {
    }

    public void setupLoadOnly(String deobfFileName, boolean loadAll) {
    }

    public boolean isRemappedClass(String className) {
        return false;
    }

    public String map(String typeName) {
        return typeName;
    }

    public String unmap(String typeName) {
        return typeName;
    }

    public String mapFieldName(String owner, String name, String desc) {
        return name;
    }

    public String mapMethodName(String owner, String name, String desc) {
        return name;
    }

    public String mapMethodDesc(String desc) {
        return desc;
    }

    public String mapDesc(String desc) {
        return desc;
    }

    public String mapType(String type) {
        return type;
    }

    public String getStaticFieldType(String oldType, String oldName, String newType, String newName) {
        return null;
    }

    public Set<String> getObfedClasses() {
        return new HashSet<>();
    }
}
