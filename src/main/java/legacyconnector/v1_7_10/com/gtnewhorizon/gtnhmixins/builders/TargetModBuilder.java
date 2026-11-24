package legacyconnector.v1_7_10.com.gtnewhorizon.gtnhmixins.builders;

import java.util.function.Predicate;

/**
 * GTNH Mixins' {@code TargetModBuilder}: what identifies a target mod. Presence is decided by the mod id (a loaded legacy mod) or by the target
 * class being loadable; the ASM based tests of the original (class node, annotation, core mod class) cannot be answered here and count as absent.
 */
public class TargetModBuilder implements ITargetMod {
    private String modId;
    private String targetClass;
    private String coreModClass;
    private Predicate<String> versionTest;

    public TargetModBuilder() {}

    public TargetModBuilder setCoreModClass(String coreModClass) {
        this.coreModClass = coreModClass;
        return this;
    }

    public TargetModBuilder setModId(String modId) {
        this.modId = modId;
        return this;
    }

    public TargetModBuilder setTargetClass(String targetClass) {
        this.targetClass = targetClass;
        return this;
    }

    public TargetModBuilder setClassNodeTest(Predicate<?> test) {
        return this;
    }

    public TargetModBuilder testModID(String id) {
        this.modId = id;
        return this;
    }

    public TargetModBuilder testModVersion(String id, String version) {
        this.modId = id;
        this.versionTest = version == null ? null : version::equals;
        return this;
    }

    public TargetModBuilder testModVersion(String id, Predicate<String> test) {
        this.modId = id;
        this.versionTest = test;
        return this;
    }

    public TargetModBuilder testModAnnotation(Predicate<String> id, Predicate<String> name, Predicate<String> version) {
        return this;
    }

    @Override
    public TargetModBuilder getBuilder() {
        return this;
    }

    /** Whether the described mod is installed. */
    public boolean isPresent() {
        if (modId != null && !modId.isEmpty()) {
            if (!legacyconnector.v1_7_10.cpw.mods.fml.common.Loader.isModLoaded(modId)) return false;
            if (versionTest == null) return true;
            var container = legacyconnector.v1_7_10.cpw.mods.fml.common.Loader.instance().getIndexedModList().get(modId);
            return container != null && versionTest.test(container.getVersion());
        }
        if (targetClass != null) {
            try {
                Class.forName(targetClass, false, TargetModBuilder.class.getClassLoader());
                return true;
            } catch (ClassNotFoundException | LinkageError e) {
                return false;
            }
        }
        return false;
    }
}
