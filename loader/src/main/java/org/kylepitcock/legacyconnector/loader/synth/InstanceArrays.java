package org.kylepitcock.legacyconnector.loader.synth;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Vanilla instance fields that hold arrays the class creates with a fixed size in its constructor (EntityDragon's
 * {@code ringBuffer} is {@code new double[64][3]}): a synthesized stand-in creates them at that size, so subclasses in
 * mods that copy vanilla's logic index them instead of failing on an empty array. Table: {@code instance-arrays.txt},
 * lines of {@code owner name descriptor dim[,dim...]}.
 */
final class InstanceArrays {
    private static final Map<LegacyVersion, Map<String, int[]>> BY_KEY = new HashMap<>();

    private InstanceArrays() {}

    private static synchronized Map<String, int[]> load(LegacyVersion version) {
        Map<String, int[]> map = BY_KEY.get(version);
        if (map != null) return map;
        map = new HashMap<>();
        try (InputStream in = InstanceArrays.class.getClassLoader().getResourceAsStream("legacyconnector/legacy/" + version.id() + "/instance-arrays.txt")) {
            if (in != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String line; (line = r.readLine()) != null; ) {
                    String[] p = line.split(" ");
                    if (p.length != 4) continue;
                    String[] d = p[3].split(",");
                    int[] dims = new int[d.length];
                    for (int i = 0; i < d.length; i++) dims[i] = Integer.parseInt(d[i]);
                    map.put(p[0] + "." + p[1] + ":" + p[2], dims);
                }
            }
        } catch (Exception e) {
            // no table
        }
        BY_KEY.put(version, map);
        return map;
    }

    /** Pushes a sized array for the field; false when the table has no entry (the caller pushes the default). */
    static boolean push(MethodVisitor mv, LegacyVersion version, String legacyOwner, String field, String desc) {
        int[] dims = load(version).get(legacyOwner.replace('.', '/') + "." + field + ":" + desc);
        if (dims == null) return false;
        Type type = Type.getType(desc);
        if (dims.length == 1) {
            mv.visitLdcInsn(dims[0]);
            Type element = type.getElementType();
            if (type.getDimensions() > 1) mv.visitTypeInsn(Opcodes.ANEWARRAY, desc.substring(1));
            else if (element.getSort() == Type.OBJECT) mv.visitTypeInsn(Opcodes.ANEWARRAY, element.getInternalName());
            else mv.visitIntInsn(Opcodes.NEWARRAY, switch (element.getSort()) {
                case Type.BOOLEAN -> Opcodes.T_BOOLEAN;
                case Type.BYTE -> Opcodes.T_BYTE;
                case Type.CHAR -> Opcodes.T_CHAR;
                case Type.SHORT -> Opcodes.T_SHORT;
                case Type.INT -> Opcodes.T_INT;
                case Type.LONG -> Opcodes.T_LONG;
                case Type.FLOAT -> Opcodes.T_FLOAT;
                default -> Opcodes.T_DOUBLE;
            });
            return true;
        }
        for (int d : dims) mv.visitLdcInsn(d);
        mv.visitMultiANewArrayInsn(desc, dims.length);
        return true;
    }
}
