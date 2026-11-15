package legacyconnector.v1_20_1.net.minecraftforge.fml.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.nio.file.Path;
import java.util.function.Function;
import net.neoforged.fml.config.ModConfig;

/**
 * Forge 1.20.1's {@code ConfigFileTypeHandler} (gone in NeoForge 1.21.1, FML reads and writes the files itself). Mods subclassed it to move the
 * {@code serverconfig} files into the global config folder; the subclasses link against this and are never asked for a reader, see {@link LegacyModConfig}.
 */
public class ConfigFileTypeHandler {
    public ConfigFileTypeHandler() {}

    public Function<ModConfig, CommentedFileConfig> reader(Path configBasePath) {
        throw new UnsupportedOperationException("config files are read by FML 4 in NeoForge 1.21.1");
    }

    public void unload(Path configBasePath, ModConfig config) {}
}
