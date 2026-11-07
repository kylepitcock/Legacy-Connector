package legacyconnector.v1_7_10.net.minecraftforge.client.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.client.LegacySprites;

public interface IModelCustomLoader {
    String getType();

    String[] getSuffixes();

    /**
     * Forge 1.7.10's entry point. Loaders written for 1.6.4 implement {@link #loadInstance(String, URL)} instead; this default calls it with a
     * URL that reads the resource through the mods' resource lookup.
     */
    default IModelCustom loadInstance(ResourceLocation resource) throws ModelFormatException {
        String path = "/assets/" + resource.getResourceDomain() + "/" + resource.getResourcePath();
        try {
            URL url = new URL(null, "legacyconnector:" + path, new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL u) {
                    return new URLConnection(u) {
                        @Override
                        public void connect() {
                        }

                        @Override
                        public InputStream getInputStream() throws IOException {
                            return new LegacySprites.ModResources().getResource(resource).getInputStream();
                        }
                    };
                }
            });
            return loadInstance(path, url);
        } catch (java.net.MalformedURLException e) {
            throw new ModelFormatException("Bad model location " + resource + ": " + e.getMessage());
        }
    }

    /** Forge 1.6.4's entry point (resource name, URL of the file). */
    default IModelCustom loadInstance(String resourceName, URL resource) throws ModelFormatException {
        throw new AbstractMethodError(getClass().getName() + " implements neither loadInstance(ResourceLocation) nor loadInstance(String, URL)");
    }
}
