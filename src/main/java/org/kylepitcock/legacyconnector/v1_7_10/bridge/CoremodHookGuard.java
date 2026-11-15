package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What a coremod hook's own first instructions demand of one of its parameters before it does anything else: {@code if (entity == null) return false;}
 * and {@code if (!(entity instanceof X)) return false;}, however the compiler arranged the jumps. A place that would build legacy views of blocks, worlds
 * and boxes for every call (a collision shape for every block of the game) asks the guard first, on the entity alone, and skips the call when the hook
 * would return false at once. Only hooks answering a boolean where false means "nothing done" have one; a hook that does not start like that has none.
 */
public final class CoremodHookGuard {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Coremods");

    private final boolean nullFails;
    private final List<Class<?>> mustBe;

    CoremodHookGuard(boolean nullFails, List<Class<?>> mustBe) {
        this.nullFails = nullFails;
        this.mustBe = mustBe;
    }

    /** Whether the hook can do anything for this value (a legacy view or null). */
    public boolean allows(Object view) {
        if (view == null) return !nullFails && mustBe.isEmpty(); // instanceof is false for null, too
        for (Class<?> c : mustBe) if (!c.isInstance(view)) return false;
        return true;
    }

    boolean nullFails() {
        return nullFails;
    }

    List<Class<?>> mustBe() {
        return mustBe;
    }

    /** The guard of {@code method}'s parameter number {@code param}, or null (no guard found, the class is not readable, the hook is no boolean one). */
    public static CoremodHookGuard of(Class<?> owner, Method method, int param) {
        if (method.getReturnType() != boolean.class || "false".equals(System.getProperty("legacyconnector.coremodGuards"))) return null; // the property switches the guards off for timing
        Class<?>[] types = method.getParameterTypes();
        int slot = 0;
        for (int i = 0; i < param; i++) slot += types[i] == long.class || types[i] == double.class ? 2 : 1;
        ClassLoader loader = owner.getClassLoader();
        try (InputStream in = loader == null ? null : loader.getResourceAsStream(owner.getName().replace('.', '/') + ".class")) {
            if (in == null) return null;
            String name = method.getName();
            String desc = Type.getMethodDescriptor(method);
            List<Object[]> code = new ArrayList<>(); // {opcode, var or null, class name or null, label or null}
            Map<Label, Integer> labels = new HashMap<>();
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String n, String d, String signature, String[] exceptions) {
                    if (!n.equals(name) || !d.equals(desc)) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitLabel(Label label) {
                            labels.put(label, code.size());
                        }

                        @Override
                        public void visitVarInsn(int opcode, int var) {
                            add(opcode, var, null, null);
                        }

                        @Override
                        public void visitTypeInsn(int opcode, String type) {
                            add(opcode, null, type, null);
                        }

                        @Override
                        public void visitJumpInsn(int opcode, Label label) {
                            add(opcode, null, null, label);
                        }

                        @Override
                        public void visitInsn(int opcode) {
                            add(opcode, null, null, null);
                        }

                        private void add(int opcode, Integer var, String type, Label label) {
                            if (code.size() < 4000) code.add(new Object[] {opcode, var, type, label});
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            boolean nullFails = false;
            List<Class<?>> mustBe = new ArrayList<>();
            int at = 0;
            while (at + 1 < code.size()) {
                if (op(code, at) != Opcodes.ALOAD || !Integer.valueOf(slot).equals(code.get(at)[1])) break;
                int next = op(code, at + 1);
                int length; // the instructions of the check
                if (next == Opcodes.IFNULL && returnsFalse(code, labels, label(code, at + 1))) { // ifnull -> return false
                    nullFails = true;
                    length = 2;
                } else if (next == Opcodes.IFNONNULL && returnsFalseAt(code, at + 2)) { // ifnonnull -> go on; else return false
                    nullFails = true;
                    length = 4;
                } else if (next == Opcodes.INSTANCEOF && at + 2 < code.size() && op(code, at + 2) == Opcodes.IFEQ && returnsFalse(code, labels, label(code, at + 2))) {
                    mustBe.add(Class.forName(((String) code.get(at + 1)[2]).replace('/', '.'), false, loader)); // instanceof; ifeq -> return false
                    length = 3;
                } else if (next == Opcodes.INSTANCEOF && at + 2 < code.size() && op(code, at + 2) == Opcodes.IFNE && returnsFalseAt(code, at + 3)) {
                    mustBe.add(Class.forName(((String) code.get(at + 1)[2]).replace('/', '.'), false, loader)); // instanceof; ifne -> go on; else return false
                    length = 5;
                } else {
                    break;
                }
                // the next check follows only where the "go on" jump lands right behind this one
                at += length;
                if (length >= 4) {
                    Label go = label(code, at - length + (length == 4 ? 1 : 2));
                    Integer lands = labels.get(go);
                    if (lands == null || lands != at) break;
                }
            }
            if (!nullFails && mustBe.isEmpty()) return null;
            LOG.debug("Coremod hook {}.{}: entity guard nullFails={} mustBe={}", owner.getName(), name, nullFails, mustBe);
            return new CoremodHookGuard(nullFails, mustBe);
        } catch (IOException | ClassNotFoundException | LinkageError | RuntimeException e) {
            LOG.debug("The first instructions of coremod hook {}.{} could not be read: {}", owner.getName(), method.getName(), e.toString());
            return null;
        }
    }

    private static int op(List<Object[]> code, int at) {
        return (int) code.get(at)[0];
    }

    private static Label label(List<Object[]> code, int at) {
        return (Label) code.get(at)[3];
    }

    /** Whether the code at the label is {@code return false}. */
    private static boolean returnsFalse(List<Object[]> code, Map<Label, Integer> labels, Label label) {
        Integer at = labels.get(label);
        return at != null && returnsFalseAt(code, at);
    }

    private static boolean returnsFalseAt(List<Object[]> code, int at) {
        return at + 1 < code.size() && op(code, at) == Opcodes.ICONST_0 && op(code, at + 1) == Opcodes.IRETURN;
    }
}
