package org.kylepitcock.legacyconnector.compat.jdk;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.lwjgl.opengl.GL20;

/** Replacements for library APIs (LWJGL 2, old authlib) that legacy mods call and that no longer exist. */
public final class LibraryCompat {
    private LibraryCompat() {}

    /** LWJGL 2 took the source as raw bytes; LWJGL 3 wants a CharSequence. */
    public static void glShaderSource(int shader, ByteBuffer source) {
        ByteBuffer copy = source.duplicate();
        byte[] bytes = new byte[copy.remaining()];
        copy.get(bytes);
        GL20.glShaderSource(shader, new String(bytes, StandardCharsets.UTF_8));
    }

    public static boolean isComplete(GameProfile profile) {
        return profile.getId() != null && profile.getName() != null && !profile.getName().isBlank();
    }

    /** Properties are filled by the modern skin pipeline; legacy callers just get the profile back. */
    public static GameProfile fillProfileProperties(MinecraftSessionService service, GameProfile profile, boolean secure) {
        return profile;
    }

    // ASM 5 never validated descriptors (anything unknown parsed as a method type); ASM 9 throws. Legacy mods that
    // pass class names or field descriptors to these keep working with the old leniency.
    public static org.objectweb.asm.Type asmGetType(String descriptor) {
        try {
            return org.objectweb.asm.Type.getType(descriptor);
        } catch (RuntimeException e) {
            // Callers hand over internal names ("a/b/C"): treat them as such.
            return org.objectweb.asm.Type.getObjectType(descriptor);
        }
    }

    public static org.objectweb.asm.Type asmGetReturnType(String descriptor) {
        try {
            return org.objectweb.asm.Type.getReturnType(descriptor);
        } catch (RuntimeException e) {
            return asmGetType(descriptor);
        }
    }

    public static org.objectweb.asm.Type[] asmGetArgumentTypes(String descriptor) {
        try {
            return org.objectweb.asm.Type.getArgumentTypes(descriptor);
        } catch (RuntimeException e) {
            return new org.objectweb.asm.Type[0];
        }
    }

    /** authlib 6 made Property a record: getName/getValue/getSignature became name()/value()/signature(). */
    public static String getName(com.mojang.authlib.properties.Property property) {
        return property.name();
    }

    public static String getValue(com.mojang.authlib.properties.Property property) {
        return property.value();
    }

    public static String getSignature(com.mojang.authlib.properties.Property property) {
        return property.signature();
    }

    /** text2speech: say(String) gained an interrupt flag; the old call queued the message. */
    public static void say(com.mojang.text2speech.Narrator narrator, String message) {
        narrator.say(message, false);
    }

    /** fastutil 8 dropped the boxed get(Integer) of its int-keyed maps (null for a null key, as a missing key gave). */
    public static Object get(it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<?> map, Integer key) {
        return key == null ? null : map.get(key.intValue());
    }
}
