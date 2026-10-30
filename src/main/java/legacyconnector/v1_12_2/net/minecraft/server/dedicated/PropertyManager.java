package legacyconnector.v1_12_2.net.minecraft.server.dedicated;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A properties file ({@code server.properties} for the dedicated server): reading a missing key stores the default, as 1.12 did, and
 * saves the file. The modern server reads its own settings at start, so values mods change take effect at the next start.
 */
public class PropertyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("LegacyConnector/1.12.2");
    private final Properties serverProperties = new Properties();
    private final File serverPropertiesFile;

    public PropertyManager(File propertiesFile) {
        this.serverPropertiesFile = propertiesFile;
        if (propertiesFile != null && propertiesFile.exists()) {
            try (InputStream in = new FileInputStream(propertiesFile)) {
                serverProperties.load(in);
            } catch (IOException e) {
                LOGGER.warn("Failed to load {}", propertiesFile, e);
            }
        }
    }

    public void saveProperties() {
        if (serverPropertiesFile == null) return;
        try (OutputStream out = new FileOutputStream(serverPropertiesFile)) {
            serverProperties.store(out, "Minecraft server properties");
        } catch (IOException e) {
            LOGGER.warn("Failed to save {}", serverPropertiesFile, e);
        }
    }

    public File getPropertiesFile() {
        return serverPropertiesFile;
    }

    public boolean hasProperty(String key) {
        return serverProperties.containsKey(key);
    }

    public void removeProperty(String key) {
        serverProperties.remove(key);
    }

    public String getStringProperty(String key, String defaultValue) {
        if (!serverProperties.containsKey(key)) {
            serverProperties.setProperty(key, defaultValue);
            saveProperties();
        }
        return serverProperties.getProperty(key, defaultValue);
    }

    public int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(getStringProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            serverProperties.setProperty(key, String.valueOf(defaultValue));
            saveProperties();
            return defaultValue;
        }
    }

    public long getLongProperty(String key, long defaultValue) {
        try {
            return Long.parseLong(getStringProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            serverProperties.setProperty(key, String.valueOf(defaultValue));
            saveProperties();
            return defaultValue;
        }
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getStringProperty(key, String.valueOf(defaultValue)));
    }

    public void setProperty(String key, Object value) {
        serverProperties.setProperty(key, String.valueOf(value));
    }
}
