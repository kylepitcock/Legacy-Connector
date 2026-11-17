package legacyconnector.v1_12_2.net.minecraftforge.fml.common.discovery.asm;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Type;

/**
 * The scanned form of an annotation: its kind, type, the member it sits on and its values (arrays and nested annotations
 * included), as FML's class parser filled it. {@link ASMModParser} builds them from a class file.
 */
public class ModAnnotation {
    public enum AnnotationType {
        CLASS, FIELD, METHOD, SUBTYPE
    }

    /** An enum value inside scanned annotation data: the descriptor of the enum class and the constant name. */
    public static class EnumHolder {
        private final String desc;
        private final String value;

        public EnumHolder(String desc, String value) {
            this.desc = desc;
            this.value = value;
        }

        public String getDesc() {
            return desc;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return desc + "." + value;
        }
    }

    AnnotationType type;
    Type asmType;
    String member;
    Map<String, Object> values = Maps.newLinkedHashMap();
    private ArrayList<Object> arrayList;
    private String arrayName;
    private ModAnnotation parent;

    public ModAnnotation(AnnotationType type, Type asmType, String member) {
        this.type = type;
        this.asmType = asmType;
        this.member = member;
    }

    public ModAnnotation(AnnotationType type, Type asmType, ModAnnotation parent, String member) {
        this.type = type;
        this.asmType = asmType;
        this.parent = parent;
        this.member = member;
    }

    @Override
    public String toString() {
        return type + " " + asmType.getClassName() + " " + member + " " + values;
    }

    public AnnotationType getType() {
        return type;
    }

    public Type getASMType() {
        return asmType;
    }

    public String getMember() {
        return member;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void addProperty(String key, Object value) {
        if (arrayList != null) {
            arrayList.add(value);
        } else {
            values.put(key, value);
        }
    }

    public void addEnumProperty(String key, String enumName, String value) {
        values.put(key, new EnumHolder(enumName, value));
    }

    public void addArray(String name) {
        arrayList = Lists.newArrayList();
        arrayName = name;
    }

    public void addChildAnnotation(String name, String desc) {
        ModAnnotation child = new ModAnnotation(AnnotationType.SUBTYPE, Type.getType(desc), this, name);
        values.put(name, child.values);
    }

    public ModAnnotation addChildAnnotation(String desc) {
        ModAnnotation child = new ModAnnotation(AnnotationType.SUBTYPE, Type.getType(desc), this, "");
        if (arrayList != null) arrayList.add(child.values);
        return child;
    }

    /** Ends the array being filled: its elements become the value of the name it was started with. */
    public void endArray() {
        if (arrayList != null) {
            values.put(arrayName, arrayList);
            arrayList = null;
            arrayName = null;
        }
    }

    /** The values of every nested annotation of this one, in the order they were read. */
    public List<Object> mcc$array() {
        return arrayList;
    }
}
