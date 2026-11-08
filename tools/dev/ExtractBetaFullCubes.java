import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;

/** Optional metadata extraction; neither a build dependency nor a Minecraft client launch. */
public final class ExtractBetaFullCubes {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Expected original b1.7.3 client jar and output TSV");
        Path jar = Path.of(args[0]).toRealPath();
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar)));
        try (var original = new URLClassLoader(new java.net.URL[]{jar.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class<?> block = Class.forName("uu", true, original);
            Field array = Arrays.stream(block.getDeclaredFields())
                    .filter(field -> field.getType().isArray() && field.getType().componentType() == block)
                    .findFirst().orElseThrow();
            array.setAccessible(true);
            Object[] blocks = (Object[]) array.get(null);
            var full = block.getDeclaredMethod("d");
            var id = block.getDeclaredField("bn");
            full.setAccessible(true);
            id.setAccessible(true);
            var lines = new ArrayList<String>();
            lines.add("# Original Minecraft Beta 1.7.3 client Block.isFullCube snapshot");
            lines.add("# Source SHA-256: " + sha);
            lines.add("# Derived by invoking original uu.d() on every nonnull uu.m[] block; no Minecraft instance created.");
            lines.add("# id\tfull_cube");
            int count = 0;
            for (int index = 0; index < blocks.length; index++) if (blocks[index] != null) {
                if (id.getInt(blocks[index]) != index) throw new IllegalStateException("Block index/id mismatch: " + index);
                lines.add(index + "\t" + full.invoke(blocks[index]));
                count++;
            }
            if (count != 96) throw new IllegalStateException("Expected 96 original Beta blocks, found " + count);
            Files.write(Path.of(args[1]), lines);
            System.out.println("Verified " + count + " original blocks; source SHA-256=" + sha);
        }
    }
}
