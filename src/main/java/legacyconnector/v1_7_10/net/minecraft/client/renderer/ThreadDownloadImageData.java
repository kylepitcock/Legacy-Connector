package legacyconnector.v1_7_10.net.minecraft.client.renderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.SimpleTexture;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureUtil;
import legacyconnector.v1_7_10.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/**
 * A texture downloaded from a URL (capes, skins, mods' remote images) on a background thread, cached in a local file when one
 * is given, and uploaded on first use after it arrived; until then it shows its fallback location's image, if any.
 */
public class ThreadDownloadImageData extends SimpleTexture {
    public static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger("ThreadDownloadImageData");
    public static final AtomicInteger threadDownloadCounter = new AtomicInteger(0);
    protected final File field_152434_e;
    protected final String imageUrl;
    protected final IImageBuffer imageBuffer;
    protected volatile BufferedImage bufferedImage;
    protected Thread imageThread;
    protected boolean textureUploaded;

    public ThreadDownloadImageData(File cacheFile, String url, ResourceLocation fallback, IImageBuffer buffer) {
        super(fallback);
        this.field_152434_e = cacheFile;
        this.imageUrl = url;
        this.imageBuffer = buffer;
    }

    private void checkTextureUploaded() {
        if (textureUploaded || bufferedImage == null) return;
        if (textureLocation != null) deleteGlTexture();
        TextureUtil.uploadTextureImage(super.getGlTextureId(), bufferedImage);
        textureUploaded = true;
    }

    @Override
    public int getGlTextureId() {
        checkTextureUploaded();
        return super.getGlTextureId();
    }

    public void setBufferedImage(BufferedImage image) {
        this.bufferedImage = image;
        if (imageBuffer != null) imageBuffer.func_152634_a();
    }

    @Override
    public void loadTexture(IResourceManager manager) throws IOException {
        if (bufferedImage == null && textureLocation != null) {
            try {
                super.loadTexture(manager);
            } catch (IOException e) {
                // 1.7.10 threw here, before starting the download; its fallback images always existed. A missing fallback
                // must not keep the real image from being downloaded.
                logger.debug("No fallback image {} for a downloaded texture: {}", textureLocation, e.toString());
            }
        }
        if (imageThread != null) return;
        if (field_152434_e != null && field_152434_e.isFile()) {
            try {
                bufferedImage = javax.imageio.ImageIO.read(field_152434_e);
                if (imageBuffer != null) setBufferedImage(imageBuffer.parseUserSkin(bufferedImage));
            } catch (IOException e) {
                logger.error("Couldn't load skin " + field_152434_e, e);
                func_152433_a();
            }
        } else {
            func_152433_a();
        }
    }

    /** Starts the download thread. */
    protected void func_152433_a() {
        imageThread = new Thread(this::mcc$download, "Texture Downloader #" + threadDownloadCounter.incrementAndGet());
        imageThread.setDaemon(true);
        imageThread.start();
    }

    private void mcc$download() {
        if (imageUrl == null) return;
        java.net.HttpURLConnection connection = null;
        logger.debug("Downloading http texture from {} to {}", imageUrl, field_152434_e);
        try {
            java.net.Proxy proxy = java.net.Proxy.NO_PROXY;
            try {
                proxy = net.minecraft.client.Minecraft.getInstance().getProxy();
            } catch (RuntimeException ignored) {
                // no client yet
            }
            connection = (java.net.HttpURLConnection) java.net.URI.create(imageUrl).toURL().openConnection(proxy);
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.connect();
            if (connection.getResponseCode() / 100 != 2) return;
            BufferedImage image;
            if (field_152434_e != null) {
                File parent = field_152434_e.getParentFile();
                if (parent != null) parent.mkdirs();
                try (java.io.InputStream in = connection.getInputStream()) {
                    java.nio.file.Files.copy(in, field_152434_e.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                image = javax.imageio.ImageIO.read(field_152434_e);
            } else {
                try (java.io.InputStream in = connection.getInputStream()) {
                    image = javax.imageio.ImageIO.read(in);
                }
            }
            if (imageBuffer != null) image = imageBuffer.parseUserSkin(image);
            setBufferedImage(image);
        } catch (IOException | RuntimeException e) {
            logger.error("Couldn't download http texture", e);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
