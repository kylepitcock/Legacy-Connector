package org.kylepitcock.mccompat.compat.jdk;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;

/**
 * Stand-in for {@code com.sun.imageio.plugins.png.PNGImageReader}, which Java 9+ no longer exports: mods built it
 * directly instead of asking ImageIO. Everything is delegated to the registered PNG reader.
 */
public final class PNGImageReader extends ImageReader {
    private final ImageReader delegate;

    public PNGImageReader(ImageReaderSpi spi) {
        super(spi);
        Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("png");
        this.delegate = it.next();
    }

    @Override
    public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);
        delegate.setInput(input, seekForwardOnly, ignoreMetadata);
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        return delegate.getNumImages(allowSearch);
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        return delegate.getWidth(imageIndex);
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        return delegate.getHeight(imageIndex);
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        return delegate.getImageTypes(imageIndex);
    }

    @Override
    public IIOMetadata getStreamMetadata() throws IOException {
        return delegate.getStreamMetadata();
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        return delegate.getImageMetadata(imageIndex);
    }

    @Override
    public ImageReadParam getDefaultReadParam() {
        return delegate.getDefaultReadParam();
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        return delegate.read(imageIndex, param);
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }
}
