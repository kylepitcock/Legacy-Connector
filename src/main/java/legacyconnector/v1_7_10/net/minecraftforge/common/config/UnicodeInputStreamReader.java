package legacyconnector.v1_7_10.net.minecraftforge.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;

/** Forge's BOM-aware reader for config files (mods that parse their own text files with it); a UTF-8/16 byte order mark is skipped. */
public class UnicodeInputStreamReader extends Reader {
    private final InputStreamReader reader;

    public UnicodeInputStreamReader(InputStream source, String defaultEncoding) throws IOException {
        PushbackInputStream in = new PushbackInputStream(source, 4);
        byte[] bom = new byte[4];
        int n = in.read(bom, 0, 4);
        int unread = n;
        String enc = defaultEncoding;
        if (n >= 3 && (bom[0] & 0xFF) == 0xEF && (bom[1] & 0xFF) == 0xBB && (bom[2] & 0xFF) == 0xBF) {
            enc = "UTF-8";
            unread = n - 3;
        } else if (n >= 2 && (bom[0] & 0xFF) == 0xFE && (bom[1] & 0xFF) == 0xFF) {
            enc = "UTF-16BE";
            unread = n - 2;
        } else if (n >= 2 && (bom[0] & 0xFF) == 0xFF && (bom[1] & 0xFF) == 0xFE) {
            enc = "UTF-16LE";
            unread = n - 2;
        }
        if (n > 0 && unread > 0) in.unread(bom, n - unread, unread);
        this.reader = new InputStreamReader(in, enc);
    }

    public String getEncoding() {
        return reader.getEncoding();
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return reader.read(cbuf, off, len);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
