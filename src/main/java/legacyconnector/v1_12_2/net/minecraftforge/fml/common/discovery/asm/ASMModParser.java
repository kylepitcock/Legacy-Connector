package legacyconnector.v1_12_2.net.minecraftforge.fml.common.discovery.asm;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.discovery.ASMDataTable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.discovery.ModCandidate;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * FML's class-file scanner: reads the type, super type, class version and every annotation of a class (on the class, its fields and
 * its methods, with arrays, enum values and nested annotations) into {@link ModAnnotation}s, which {@link #sendToTable} files in an
 * {@link ASMDataTable}. The game's own scan data is what the layer's table is built from; this parser serves the mods that scan class
 * files themselves (a mod's plugin finding its own annotated classes in a jar).
 */
public class ASMModParser {
    private Type asmType;
    private int classVersion;
    private Type asmSuperType;
    private final LinkedList<ModAnnotation> annotations = new LinkedList<>();
    private String baseModProperties;

    public ASMModParser(InputStream stream) throws IOException {
        ClassReader reader = new ClassReader(stream);
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                beginNewTypeName(name, version, superName);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                startClassAnnotation(desc);
                return annotationVisitor(annotations.getFirst());
            }

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                return new FieldVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String annotationDesc, boolean visible) {
                        startFieldAnnotation(name, annotationDesc);
                        return annotationVisitor(annotations.getFirst());
                    }
                };
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String annotationDesc, boolean visible) {
                        startMethodAnnotation(name, desc, annotationDesc);
                        return annotationVisitor(annotations.getFirst());
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    /** Fills the values of one annotation (arrays and nested annotations are read recursively). */
    private static AnnotationVisitor annotationVisitor(ModAnnotation target) {
        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public void visit(String name, Object value) {
                target.addProperty(name, value);
            }

            @Override
            public void visitEnum(String name, String desc, String value) {
                target.addEnumProperty(name, desc, value);
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                target.addArray(name);
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(String n, Object value) {
                        target.addProperty(null, value);
                    }

                    @Override
                    public void visitEnum(String n, String desc, String value) {
                        target.mcc$array().add(new ModAnnotation.EnumHolder(desc, value));
                    }

                    @Override
                    public AnnotationVisitor visitAnnotation(String n, String desc) {
                        return nested(target.addChildAnnotation(desc));
                    }

                    @Override
                    public void visitEnd() {
                        target.endArray();
                    }
                };
            }

            @Override
            public AnnotationVisitor visitAnnotation(String name, String desc) {
                ModAnnotation child = new ModAnnotation(ModAnnotation.AnnotationType.SUBTYPE, Type.getType(desc), name);
                target.getValues().put(name, child.getValues());
                return nested(child);
            }
        };
    }

    private static AnnotationVisitor nested(ModAnnotation child) {
        return annotationVisitor(child);
    }

    public void beginNewTypeName(String typeQName, int classVersion, String superClassQName) {
        this.asmType = Type.getObjectType(typeQName);
        this.classVersion = classVersion;
        this.asmSuperType = superClassQName == null ? null : Type.getObjectType(superClassQName);
    }

    public void startClassAnnotation(String annotationName) {
        annotations.addFirst(new ModAnnotation(ModAnnotation.AnnotationType.CLASS, Type.getType(annotationName), asmType.getClassName()));
    }

    public void startFieldAnnotation(String fieldName, String annotationName) {
        annotations.addFirst(new ModAnnotation(ModAnnotation.AnnotationType.FIELD, Type.getType(annotationName), fieldName));
    }

    public void startMethodAnnotation(String methodName, String methodDescriptor, String annotationName) {
        annotations.addFirst(new ModAnnotation(ModAnnotation.AnnotationType.METHOD, Type.getType(annotationName), methodName + methodDescriptor));
    }

    public void addAnnotationProperty(String key, Object value) {
        annotations.getFirst().addProperty(key, value);
    }

    public void addAnnotationEnumProperty(String name, String desc, String value) {
        annotations.getFirst().addEnumProperty(name, desc, value);
    }

    public Type getASMType() {
        return asmType;
    }

    public int getClassVersion() {
        return classVersion;
    }

    public Type getASMSuperType() {
        return asmSuperType;
    }

    public LinkedList<ModAnnotation> getAnnotations() {
        return annotations;
    }

    /** Whether the class extends ModLoader's {@code BaseMod} (or a type already known to). */
    public boolean isBaseMod(List<String> rememberedTypes) {
        if (asmSuperType == null) return false;
        return asmSuperType.equals(Type.getType("LBaseMod;")) || asmSuperType.equals(Type.getType("Lnet/minecraft/src/BaseMod;"))
                || (rememberedTypes != null && rememberedTypes.contains(asmSuperType.getClassName()));
    }

    public void setBaseModProperties(String foundProperties) {
        this.baseModProperties = foundProperties;
    }

    public String getBaseModProperties() {
        return baseModProperties;
    }

    /** Files every scanned annotation in the table, as the game's own scan data is. */
    public void sendToTable(ASMDataTable table, ModCandidate candidate) {
        for (ModAnnotation ma : annotations) {
            Map<String, Object> values = new LinkedHashMap<>(ma.getValues());
            table.addASMData(candidate, ma.getASMType().getClassName(), asmType.getClassName(), ma.getMember(), values);
        }
    }

    @Override
    public String toString() {
        return "ASMModParser{" + asmType + " : " + asmSuperType + " v" + classVersion + " " + new ArrayList<>(annotations) + "}";
    }
}
