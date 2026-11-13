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

    /**
     * authlib 1.5's {@code joinServer(profile, accessToken, serverId)}: authlib 6 takes the profile id. The checked
     * {@code AuthenticationException} of both versions is the same type, so it propagates as it did.
     */
    public static void joinServer(MinecraftSessionService service, GameProfile profile, String authenticationToken, String serverId) throws Exception {
        service.joinServer(profile.getId(), authenticationToken, serverId);
    }

    /** authlib 1.5's {@code hasJoinedServer(user, serverId, address)}: authlib 6 takes the name and returns a result record (null when not joined). */
    public static GameProfile hasJoinedServer(MinecraftSessionService service, GameProfile user, String serverId, java.net.InetAddress address) throws Exception {
        var result = service.hasJoinedServer(user.getName(), serverId, address);
        return result == null ? null : result.profile();
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

    /** authlib 1.5's {@code new YggdrasilAuthenticationService(proxy, clientToken)}: the client token is gone. */
    public static com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService yggdrasilService(java.net.Proxy proxy, String clientToken) {
        return new com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService(proxy);
    }

    /** authlib 1.5's {@code UUIDTypeAdapter.fromString}: a UUID with or without dashes. */
    public static java.util.UUID uuidFromString(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.length() == 32) {
            s = s.substring(0, 8) + "-" + s.substring(8, 12) + "-" + s.substring(12, 16) + "-" + s.substring(16, 20) + "-" + s.substring(20);
        }
        return java.util.UUID.fromString(s);
    }

    /** authlib 1.5's {@code UUIDTypeAdapter.fromUUID}: the UUID without dashes. */
    public static String uuidToString(java.util.UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    /** authlib 1.5's {@code findProfilesByNames(names, Agent.MINECRAFT, callback)}: no agent argument any more (it is always null here). */
    public static void findProfilesByNames(com.mojang.authlib.GameProfileRepository repo, String[] names, Object agent,
            com.mojang.authlib.ProfileLookupCallback callback) {
        repo.findProfilesByNames(names, new LegacyLookupCallback(callback));
    }

    /**
     * Callbacks written against authlib 1.5 implement {@code onProfileLookupFailed(GameProfile, Exception)}; authlib 6 calls
     * {@code onProfileLookupFailed(String, Exception)}, which such a class does not have (AbstractMethodError).
     */
    private record LegacyLookupCallback(com.mojang.authlib.ProfileLookupCallback delegate) implements com.mojang.authlib.ProfileLookupCallback {
        @Override
        public void onProfileLookupSucceeded(GameProfile profile) {
            delegate.onProfileLookupSucceeded(profile);
        }

        @Override
        public void onProfileLookupFailed(String name, Exception exception) {
            try {
                delegate.onProfileLookupFailed(name, exception);
                return;
            } catch (AbstractMethodError missing) {
                // a legacy callback: fall through to the old signature
            }
            try {
                java.lang.reflect.Method old = delegate.getClass().getMethod("onProfileLookupFailed", GameProfile.class, Exception.class);
                old.setAccessible(true);
                old.invoke(delegate, new GameProfile(GameProfileCompat.id(null, name), GameProfileCompat.name(name)), exception);
            } catch (ReflectiveOperationException | RuntimeException e) {
                // the lookup simply failed
            }
        }
    }

    /** text2speech: say(String) gained an interrupt flag; the old call queued the message. */
    public static void say(com.mojang.text2speech.Narrator narrator, String message) {
        narrator.say(message, false);
    }

    /** fastutil 8 dropped the boxed get(Integer) of its int-keyed maps (null for a null key, as a missing key gave). */
    public static Object get(it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<?> map, Integer key) {
        return key == null ? null : map.get(key.intValue());
    }

    /** fastutil 7's {@code ByteArrayList.rem(Object)}: removes the first occurrence (fastutil 8 spells it remove(Object)). */
    public static boolean rem(it.unimi.dsi.fastutil.bytes.ByteArrayList list, Object o) {
        return list.remove(o);
    }

    /** ASM 5's {@code RemappingMethodAdapter(access, desc, mv, remapper)}: ASM 9's {@code MethodRemapper} takes just the visitor and the remapper. */
    public static org.objectweb.asm.commons.MethodRemapper methodRemapper(int access, String desc, org.objectweb.asm.MethodVisitor mv,
            org.objectweb.asm.commons.Remapper remapper) {
        return new org.objectweb.asm.commons.MethodRemapper(mv, remapper);
    }

    /** The same with an explicit ASM api level first (5-argument form). */
    public static org.objectweb.asm.commons.MethodRemapper methodRemapper(int api, int access, String desc, org.objectweb.asm.MethodVisitor mv,
            org.objectweb.asm.commons.Remapper remapper) {
        return new org.objectweb.asm.commons.MethodRemapper(mv, remapper);
    }

    /** jopt-simple 4's {@code internal.Strings.join(List, String)} (5 takes an Iterable). */
    public static String joinStrings(java.util.List<String> pieces, String separator) {
        return String.join(separator, pieces);
    }

    /** Netty 4.0's {@code ChannelPromiseAggregator.add(ChannelPromise...)}, which returned the aggregator (4.1 inherits the generic one and returns the base type). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static io.netty.channel.ChannelPromiseAggregator add(io.netty.channel.ChannelPromiseAggregator aggregator, io.netty.channel.ChannelPromise[] promises) {
        aggregator.add((io.netty.util.concurrent.Promise[]) promises);
        return aggregator;
    }

    /** jopt-simple 4's {@code acceptsAll(Collection, String)} (5 takes a List). */
    public static joptsimple.OptionSpecBuilder acceptsAll(joptsimple.OptionParser parser, java.util.Collection<String> names, String description) {
        return parser.acceptsAll(new java.util.ArrayList<>(names), description);
    }

    /** authlib 1.5's {@code getTextures(profile, requireSecure)}: the textures by type (authlib 6 returns a record of them). */
    public static java.util.Map<com.mojang.authlib.minecraft.MinecraftProfileTexture.Type, com.mojang.authlib.minecraft.MinecraftProfileTexture> getTextures(
            MinecraftSessionService service, GameProfile profile, boolean requireSecure) {
        java.util.Map<com.mojang.authlib.minecraft.MinecraftProfileTexture.Type, com.mojang.authlib.minecraft.MinecraftProfileTexture> out = new java.util.EnumMap<>(
                com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.class);
        try {
            var textures = service.getTextures(profile);
            if (textures != null) {
                if (textures.skin() != null) out.put(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN, textures.skin());
                if (textures.cape() != null) out.put(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.CAPE, textures.cape());
                if (textures.elytra() != null) out.put(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.ELYTRA, textures.elytra());
            }
        } catch (RuntimeException e) {
            // no textures known
        }
        return out;
    }
}
