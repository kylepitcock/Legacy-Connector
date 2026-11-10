package org.kylepitcock.legacyconnector.vb1_7_3;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

/** A world root is shared across dimensions; remote presentation files stay local to this game instance and server. */
public final class WorldStorageB173 {
    private static volatile Function<Level,Path> client=level->null;
    private WorldStorageB173(){}
    public static void installClient(Function<Level,Path> resolver){client=Objects.requireNonNull(resolver);}
    public static Path root(Level level){
        if(level==null)throw new IllegalStateException("No native world storage directory available");
        Path nativeRoot=level instanceof net.minecraft.server.level.ServerLevel server?server.getServer().getWorldPath(LevelResource.ROOT).toAbsolutePath():null;
        return choose(nativeRoot,()->level.isClientSide?client.apply(level):null);
    }
    static Path choose(Path nativeRoot,Supplier<Path> client){
        Path path=nativeRoot!=null?nativeRoot:client.get();
        if(path==null||!path.isAbsolute())throw new IllegalStateException("No bound native world storage directory available");
        return path.normalize();
    }
    public static Path boundClientRoot(boolean activeWorld,Path integratedRoot,Path gameDirectory,String serverIdentity){
        if(!activeWorld)throw new IllegalStateException("No active client world storage binding available");
        return choose(integratedRoot,()->clientCacheRoot(gameDirectory,serverIdentity));
    }
    public static Path clientCacheRoot(Path gameDirectory,String serverIdentity){
        if(gameDirectory==null||!gameDirectory.isAbsolute()||serverIdentity==null||serverIdentity.isBlank())throw new IllegalStateException("No active client world storage identity available");
        try{
            var digest=java.security.MessageDigest.getInstance("SHA-256");
            String key=HexFormat.of().formatHex(digest.digest(serverIdentity.getBytes(StandardCharsets.UTF_8)));
            return gameDirectory.normalize().resolve(".legacyconnector").resolve("client-worlds").resolve(key);
        }catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
}
