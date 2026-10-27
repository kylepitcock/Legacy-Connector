package legacyconnector.v1_12_2.net.minecraft.client.renderer.texture;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Width and height read from a PNG's header. */
public class PngSizeInfo {
    public final int pngWidth;
    public final int pngHeight;

    public PngSizeInfo(String toString, InputStream textureStream) throws IOException {
        DataInputStream in = new DataInputStream(textureStream);
        if (in.readLong() != -8552249625308161526L) throw new IOException("Bad PNG Signature: " + toString);
        if (in.readInt() != 13) throw new IOException("Bad length for IHDR chunk: " + toString);
        if (in.readInt() != 1229472850) throw new IOException("Bad type for IHDR chunk: " + toString);
        pngWidth = in.readInt();
        pngHeight = in.readInt();
        textureStream.close();
    }
}
