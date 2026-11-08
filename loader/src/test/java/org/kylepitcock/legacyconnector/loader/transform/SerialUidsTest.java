package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

class SerialUidsTest {
    /** Serializable through its superclass only, no declared serialVersionUID. */
    public static class Base implements Serializable {
        int a;
        public void f() {}
    }

    public static class Child extends Base {
        String b;
    }

    static class Plain {
        int c;
    }

    private static byte[] bytes(Class<?> c) throws Exception {
        try (InputStream in = c.getClassLoader().getResourceAsStream(c.getName().replace('.', '/') + ".class")) {
            return in.readAllBytes();
        }
    }

    /** What the translation does to a class: it adds members. */
    private static byte[] translate(byte[] original, Set<String> serializable) {
        ClassWriter cw = new ClassWriter(0);
        ClassVisitor adder = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public void visitEnd() {
                visitField(Opcodes.ACC_PUBLIC, "mcc$extra", "I", null, null).visitEnd();
                super.visitEnd();
            }
        };
        new ClassReader(original).accept(SerialUids.head(serializable, original, adder), 0);
        return cw.toByteArray();
    }

    private static final class Loader extends ClassLoader {
        Loader() {
            super(SerialUidsTest.class.getClassLoader());
        }

        Class<?> define(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

    @Test
    void serialVersionUidSurvivesTranslation() throws Exception {
        Map<String, byte[]> jar = Map.of(
                Base.class.getName().replace('.', '/') + ".class", bytes(Base.class),
                Child.class.getName().replace('.', '/') + ".class", bytes(Child.class),
                Plain.class.getName().replace('.', '/') + ".class", bytes(Plain.class));
        Set<String> ser = SerialUids.serializable(jar);
        assertTrue(ser.contains(Base.class.getName().replace('.', '/')));
        assertTrue(ser.contains(Child.class.getName().replace('.', '/')), "serializable through a superclass");
        assertFalse(ser.contains(Plain.class.getName().replace('.', '/')));
        for (Class<?> c : new Class<?>[] {Base.class, Child.class}) {
            long original = ObjectStreamClass.lookup(c).getSerialVersionUID();
            byte[] translated = translate(bytes(c), ser);
            Class<?> loaded = new Loader().define(c.getName(), translated);
            assertNotNull(loaded.getDeclaredField("mcc$extra"));
            assertEquals(original, ObjectStreamClass.lookup(loaded).getSerialVersionUID(), c.getSimpleName());
        }
    }
}
