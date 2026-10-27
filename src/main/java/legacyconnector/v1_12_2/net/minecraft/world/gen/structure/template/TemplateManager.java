package legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.nbt.CompressedStreamTools;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * Loads and caches structure templates: first {@code <world>/structures/<path>.nbt}, then {@code assets/<ns>/structures/<path>.nbt}
 * of the mod jars (where 1.12 mods ship theirs).
 */
public class TemplateManager {
    private final Map<String, Template> templates = new ConcurrentHashMap<>();
    private final String baseFolder;

    public TemplateManager(String baseFolder, legacyconnector.v1_12_2.net.minecraft.util.datafix.DataFixer fixer) {
        this.baseFolder = baseFolder;
    }

    /** The manager of a world's save folder. */
    public static TemplateManager mcc$forWorld(legacyconnector.v1_12_2.net.minecraft.world.World world) {
        File dir = world.getSaveHandler().getWorldDirectory();
        return new TemplateManager(new File(dir, "structures").getPath(), null);
    }

    public Template getTemplate(MinecraftServer server, ResourceLocation id) {
        Template t = get(server, id);
        if (t == null) {
            t = new Template();
            templates.put(id.getPath(), t);
        }
        return t;
    }

    public Template get(MinecraftServer server, ResourceLocation templatePath) {
        String key = templatePath.getPath();
        Template cached = templates.get(key);
        if (cached != null) return cached;
        if (readTemplate(templatePath)) return templates.get(key);
        return null;
    }

    public boolean readTemplate(ResourceLocation server) {
        String path = server.getPath();
        File file = new File(baseFolder, path + ".nbt");
        if (file.isFile()) {
            try (InputStream in = new java.io.FileInputStream(file)) {
                readTemplateFromStream(path, in);
                return true;
            } catch (Throwable e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Structures").warn("Cannot read structure template {}: {}", file, e.toString());
                return false;
            }
        }
        return readTemplateFromJar(server);
    }

    private boolean readTemplateFromJar(ResourceLocation id) {
        String ns = id.getNamespace();
        String path = id.getPath() + ".nbt";
        try {
            for (var mod : net.neoforged.fml.ModList.get().getModFiles()) {
                var p = mod.getFile().findResource("assets", ns, "structures", path);
                if (p == null || !java.nio.file.Files.isRegularFile(p)) continue;
                try (InputStream in = java.nio.file.Files.newInputStream(p)) {
                    readTemplateFromStream(id.getPath(), in);
                    return true;
                }
            }
            InputStream in = TemplateManager.class.getResourceAsStream(Template.resourcePath(id));
            if (in != null) {
                try (in) {
                    readTemplateFromStream(id.getPath(), in);
                    return true;
                }
            }
        } catch (Throwable e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Structures").warn("Cannot read structure template {}: {}", id, e.toString());
        }
        return false;
    }

    private void readTemplateFromStream(String id, InputStream stream) throws java.io.IOException {
        NBTTagCompound tag = CompressedStreamTools.readCompressed(stream);
        Template template = new Template();
        template.read(tag);
        templates.put(id, template);
    }

    public boolean writeTemplate(MinecraftServer server, ResourceLocation id) {
        Template template = templates.get(id.getPath());
        if (template == null) return false;
        File file = new File(baseFolder, id.getPath() + ".nbt");
        file.getParentFile().mkdirs();
        try (var out = new java.io.FileOutputStream(file)) {
            CompressedStreamTools.writeCompressed(template.writeToNBT(new NBTTagCompound()), out);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public void remove(ResourceLocation id) {
        templates.remove(id.getPath());
    }
}
