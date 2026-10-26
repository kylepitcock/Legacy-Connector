package org.kylepitcock.legacyconnector.core;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Runtime state of one legacy mod: identity, the instance of its {@code @Mod} class, and the NeoForge container
 * that hosts it.
 */
public final class LegacyModRecord {
    public final String version;
    public final String legacyId;
    public final String modernId;
    public final String mainClassName;
    public final ModContainer container;
    public final IEventBus modBus;
    public final JsonObject metadata;
    public final Logger logger;
    /** Set by the platform during construction. */
    public Class<?> mainClass;
    public Object instance;
    /** Version-specific state (e.g. the legacy ModContainer/ModMetadata objects). */
    public Object platformData;

    public LegacyModRecord(String version, String legacyId, String mainClassName, ModContainer container, IEventBus modBus, JsonObject metadata) {
        this.version = version;
        this.legacyId = legacyId;
        this.modernId = container.getModId();
        this.mainClassName = mainClassName;
        this.container = container;
        this.modBus = modBus;
        this.metadata = metadata;
        this.logger = LogManager.getLogger(legacyId);
    }

    /** The mod's jar also holds a coremod (FMLCorePluginContainsFMLMod); FML loaded those mods first. */
    public boolean inCoreModJar() {
        return metadata != null && metadata.has("inCoreModJar") && metadata.get("inCoreModJar").getAsBoolean();
    }

    public String name() {
        return metadata != null && metadata.has("name") ? metadata.get("name").getAsString() : legacyId;
    }

    public String modVersion() {
        return metadata != null && metadata.has("version") ? metadata.get("version").getAsString() : container.getModInfo().getVersion().toString();
    }

    public String dependencies() {
        return metadata != null && metadata.has("dependencies") ? metadata.get("dependencies").getAsString() : "";
    }

    public boolean flag(String key) {
        return metadata != null && metadata.has(key) && metadata.get(key).getAsBoolean();
    }

    /** The mod's jar as the user installed it (mods read their own jar through the container's source). */
    public Path originalFile() {
        if (metadata != null && metadata.has("sourcePath")) {
            Path p = Path.of(metadata.get("sourcePath").getAsString());
            if (java.nio.file.Files.isRegularFile(p)) return p;
        }
        return sourceFile();
    }

    public Path sourceFile() {
        return container.getModInfo().getOwningFile().getFile().getFilePath();
    }

    @Override
    public String toString() {
        return legacyId + " [" + version + "]";
    }
}
