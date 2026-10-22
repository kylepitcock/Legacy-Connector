package legacyconnector.v1_7_10.cpw.mods.fml.common.discovery;

import java.io.File;
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
    private final Map<String, Set<ModCandidate>> byPackage = new HashMap<>();

    public static synchronized ASMDataTable mcc$get() {
        if (instance == null) instance = new ASMDataTable(true);
        return instance;
    }

    public ASMDataTable() {
    }

    private ASMDataTable(boolean scan) {
        for (var scanData : ModList.get().getAllScanData()) {
            File file = null;
            try {
                if (!scanData.getIModInfoData().isEmpty()) file = scanData.getIModInfoData().get(0).getFile().getFilePath().toFile();
            } catch (RuntimeException ignored) {
                // file system without java.io.File paths
            }
            ModCandidate candidate = new ModCandidate(file);
            for (var c : scanData.getClasses()) candidate.mcc$addClass(c.clazz().getInternalName());
            for (String pkg : candidate.getContainedPackages()) byPackage.computeIfAbsent(pkg, k -> new LinkedHashSet<>()).add(candidate);
            for (var a : scanData.getAnnotations()) {
                String annotation = a.annotationType().getClassName();
                String className = a.clazz().getClassName();
                ASMData data = new ASMData(candidate, annotation, className, a.memberName(), new HashMap<>(a.annotationData()));
                byAnnotation.computeIfAbsent(annotation, k -> new LinkedHashSet<>()).add(data);
            }
        }
    }

    public Set<ASMData> getAll(String annotation) {
        Set<ASMData> out = new LinkedHashSet<>(byAnnotation.getOrDefault(annotation, Set.of()));
        if (!annotation.startsWith("legacyconnector.")) out.addAll(byAnnotation.getOrDefault("legacyconnector.v1_7_10." + annotation, Set.of()));
        else out.addAll(byAnnotation.getOrDefault(annotation.substring("legacyconnector.v1_7_10.".length()), Set.of()));
        return out;
    }

    public com.google.common.collect.SetMultimap<String, ASMData> getAnnotationsFor(legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer container) {
        com.google.common.collect.SetMultimap<String, ASMData> out = com.google.common.collect.LinkedHashMultimap.create();
        File source = container == null ? null : container.getSource();
        if (source == null) return out;
        for (var e : byAnnotation.entrySet()) {
            for (ASMData d : e.getValue()) {
                if (d.getCandidate() != null && source.equals(d.getCandidate().getModContainer())) out.put(e.getKey(), d);
            }
        }
        return out;
    }

    public Set<ModCandidate> getCandidatesFor(String pkg) {
        return new LinkedHashSet<>(byPackage.getOrDefault(pkg, Set.of()));
    }

    public void addASMData(ModCandidate candidate, String annotation, String className, String objectName, Map<String, Object> info) {
        byAnnotation.computeIfAbsent(annotation, k -> new LinkedHashSet<>()).add(new ASMData(candidate, annotation, className, objectName, info));
    }

    public static final class ASMData implements Cloneable {
        private final ModCandidate candidate;
        private final String annotationName;
        private final String className;
        private final String objectName;
        private final Map<String, Object> annotationInfo;

        public ASMData(ModCandidate candidate, String annotationName, String className, String objectName, Map<String, Object> info) {
            this.candidate = candidate;
            this.annotationName = annotationName;
            this.className = className;
            this.objectName = objectName;
            this.annotationInfo = info;
        }

        public ModCandidate getCandidate() {
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
