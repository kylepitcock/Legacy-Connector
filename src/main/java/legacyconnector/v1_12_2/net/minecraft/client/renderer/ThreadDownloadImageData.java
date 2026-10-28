package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.SimpleTexture;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureUtil;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * 1.12.2 downloaded texture (capes, skins, mod images from the web): shows the fallback texture until the image is read from the cache
 * file or downloaded on a background thread (and saved to the cache file), then uploads it, reshaped by the image buffer if one is given.
 */
public class ThreadDownloadImageData extends SimpleTexture {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Download");
    private static final AtomicInteger THREAD_ID = new AtomicInteger(0);
    private final File cacheFile;
    private final String imageUrl;
    private final IImageBuffer imageBuffer;
    private volatile BufferedImage bufferedImage;
    private Thread imageThread;
    private boolean textureUploaded;

    public ThreadDownloadImageData(File cacheFileIn, String imageUrlIn, ResourceLocation textureResourceLocation, IImageBuffer imageBufferIn) {
        super(textureResourceLocation);
        this.cacheFile = cacheFileIn;
        this.imageUrl = imageUrlIn;
        this.imageBuffer = imageBufferIn;
    }

    private void checkTextureUploaded() {
        if (textureUploaded || bufferedImage == null) return;
        if (textureLocation != null) deleteGlTexture();
        TextureUtil.uploadTextureImageAllocate(getGlTextureId(), bufferedImage, false, false);
        textureUploaded = true;
    }

    @Override
    public int getGlTextureId() {
        if (!textureUploaded && bufferedImage != null && com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
            textureUploaded = true;
            TextureUtil.uploadTextureImageAllocate(super.getGlTextureId(), bufferedImage, false, false);
        }
        return super.getGlTextureId();
    }

    public void setBufferedImage(BufferedImage bufferedImageIn) {
        this.bufferedImage = bufferedImageIn;
        if (imageBuffer != null) imageBuffer.skinAvailable();
        com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(this::checkTextureUploaded);
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
        if (bufferedImage == null && textureLocation != null) {
            try {
                super.loadTexture(resourceManager);
            } catch (IOException e) {
                // no fallback texture: nothing is shown until the download arrives
            }
        }
        if (imageThread != null) return;
        if (cacheFile != null && cacheFile.isFile()) {
            try {
                BufferedImage image = ImageIO.read(cacheFile);
                if (image != null) {
                    setBufferedImage(imageBuffer != null ? imageBuffer.parseUserSkin(image) : image);
                    return;
                }
            } catch (IOException e) {
                LOGGER.debug("Could not read the cached image {}: {}", cacheFile, e.toString());
            }
        }
        loadTextureFromServer();
    }

    protected void loadTextureFromServer() {
        imageThread = new Thread("Texture Downloader #" + THREAD_ID.incrementAndGet()) {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) new URL(imageUrl).openConnection(java.net.Proxy.NO_PROXY);
                    connection.setDoInput(true);
                    connection.setDoOutput(false);
                    connection.connect();
                    if (connection.getResponseCode() / 100 != 2) return;
                    BufferedImage image;
                    if (cacheFile != null) {
                        try (InputStream in = connection.getInputStream()) {
                            java.nio.file.Files.createDirectories(cacheFile.toPath().toAbsolutePath().getParent());
                            java.nio.file.Files.copy(in, cacheFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                        image = ImageIO.read(cacheFile);
                    } else {
                        try (InputStream in = connection.getInputStream()) {
                            image = ImageIO.read(in);
                        }
                    }
                    if (image == null) return;
                    if (imageBuffer != null) image = imageBuffer.parseUserSkin(image);
                    setBufferedImage(image);
                } catch (Exception e) {
                    LOGGER.debug("Couldn't download the texture {}: {}", imageUrl, e.toString());
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
        };
        imageThread.setDaemon(true);
        imageThread.start();
    }
}
