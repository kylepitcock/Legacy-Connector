package mccompat.v1_7_10.cpw.mods.fml.common.discovery;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.ModList;

/**
 * FML's annotation index over all mods, built from NeoForge's scan data. Legacy annotation names are accepted both
 * as written by the mod ("cpw.mods.fml.common.Mod$Instance") and as relocated into the shim.
 */
public class ASMDataTable {
    private static ASMDataTable instance;
    private final Map<String, Set<ASMData>> byAnnotation = new HashMap<>();
    private final Map<String, Set<ASMData>> byPackage = new HashMap<>();

    public static synchronized ASMDataTable mcc$get() {
        if (instance == null) instance = new ASMDataTable(true);
        return instance;
    }

    public ASMDataTable() {
    }

    private ASMDataTable(boolean scan) {
        for (var scanData : ModList.get().getAllScanData()) {
            for (var a : scanData.getAnnotations()) {
                String annotation = a.annotationType().getClassName();
                String className = a.clazz().getClassName();
                ASMData data = new ASMData(null, annotation, className, a.memberName(), new HashMap<>(a.annotationData()));
                byAnnotation.computeIfAbsent(annotation, k -> new LinkedHashSet<>()).add(data);
                int dot = className.lastIndexOf('.');
                byPackage.computeIfAbsent(dot < 0 ? "" : className.substring(0, dot), k -> new LinkedHashSet<>()).add(data);
            }
        }
    }

    public Set<ASMData> getAll(String annotation) {
        Set<ASMData> out = new LinkedHashSet<>(byAnnotation.getOrDefault(annotation, Set.of()));
        if (!annotation.startsWith("mccompat.")) out.addAll(byAnnotation.getOrDefault("mccompat.v1_7_10." + annotation, Set.of()));
        else out.addAll(byAnnotation.getOrDefault(annotation.substring("mccompat.v1_7_10.".length()), Set.of()));
        return out;
    }

    public Set<ASMData> getAnnotationsFor(Object container) {
        return new HashSet<>();
    }

    public Set<ASMData> getCandidatesFor(String pkg) {
        return new LinkedHashSet<>(byPackage.getOrDefault(pkg, Set.of()));
    }

    public void addASMData(Object candidate, String annotation, String className, String objectName, Map<String, Object> info) {
        byAnnotation.computeIfAbsent(annotation, k -> new LinkedHashSet<>()).add(new ASMData(candidate, annotation, className, objectName, info));
    }

    public static final class ASMData implements Cloneable {
        private final Object candidate;
        private final String annotationName;
        private final String className;
        private final String objectName;
        private final Map<String, Object> annotationInfo;

        public ASMData(Object candidate, String annotationName, String className, String objectName, Map<String, Object> info) {
            this.candidate = candidate;
            this.annotationName = annotationName;
            this.className = className;
            this.objectName = objectName;
            this.annotationInfo = info;
        }

        public Object getCandidate() {
            return candidate;
        }

        public String getAnnotationName() {
            return annotationName;
        }

        public String getClassName() {
            return className;
        }

        public String getObjectName() {
            return objectName;
        }

        public Map<String, Object> getAnnotationInfo() {
            return annotationInfo;
        }

        public ASMData copy(Map<String, Object> newInfo) {
            return new ASMData(candidate, annotationName, className, objectName, newInfo);
        }
    }
}
