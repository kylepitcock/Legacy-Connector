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
}
