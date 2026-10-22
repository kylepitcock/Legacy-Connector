package mccompat.v1_7_10.net.minecraft.server.dedicated;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/** Reader of server.properties for legacy mods (world name, ports, ...). */
public class PropertyManager {
    private final Properties serverProperties = new Properties();
    private final File serverPropertiesFile;

    public PropertyManager(File file) {
        this.serverPropertiesFile = file;
        if (file != null && file.isFile()) {
            try (FileInputStream in = new FileInputStream(file)) {
                serverProperties.load(in);
            } catch (IOException e) {
                generateNewProperties();
            }
        } else {
            generateNewProperties();
        }
    }

    public void generateNewProperties() {
        serverProperties.clear();
    }

    public void saveProperties() {
        if (serverPropertiesFile == null) return;
        try (FileOutputStream out = new FileOutputStream(serverPropertiesFile)) {
            serverProperties.store(out, "Minecraft server properties");
        } catch (IOException ignored) {
            // read-only: as vanilla, the failure is only logged
        }
    }

    public File getPropertiesFile() {
        return serverPropertiesFile;
    }

    public String getStringProperty(String key, String fallback) {
        if (!serverProperties.containsKey(key)) {
            serverProperties.setProperty(key, fallback);
            saveProperties();
        }
        return serverProperties.getProperty(key, fallback);
    }

    public int getIntProperty(String key, int fallback) {
        try {
            return Integer.parseInt(getStringProperty(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            serverProperties.setProperty(key, String.valueOf(fallback));
            return fallback;
        }
    }

    public boolean getBooleanProperty(String key, boolean fallback) {
        try {
            return Boolean.parseBoolean(getStringProperty(key, String.valueOf(fallback)));
        } catch (RuntimeException e) {
            serverProperties.setProperty(key, String.valueOf(fallback));
            return fallback;
        }
    }

    public void setProperty(String key, Object value) {
        serverProperties.setProperty(key, String.valueOf(value));
    }
}
