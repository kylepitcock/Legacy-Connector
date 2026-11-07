package legacyconnector.v1_6_4.net.minecraft.client.texturepacks;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.kylepitcock.legacyconnector.v1_7_10.client.ClasspathTextures;

/** Minecraft 1.4.7's texture pack list: one pack, the default one, whose files are the mods' class path files (a mod reads its sprite images through it). */
public class TexturePackList {
    public static final ITexturePack DEFAULT = new ITexturePack() {
        @Override
        public InputStream getResourceAsStream(String path) {
            return ClasspathTextures.open(path);
        }

        @Override
        public String getTexturePackFileName() {
            return "Default";
        }

        @Override
        public String getFirstDescriptionLine() {
            return "The default look of Minecraft";
        }

        @Override
        public String getSecondDescriptionLine() {
            return "";
        }
    };

    public ITexturePack defaultTexturePack = DEFAULT;
    public ITexturePack selectedTexturePack = DEFAULT;

    public ITexturePack getSelectedTexturePack() {
        return selectedTexturePack;
    }

    public List availableTexturePacks() {
        List list = new ArrayList();
        list.add(DEFAULT);
        return list;
    }

    public boolean setTexturePack(ITexturePack pack) {
        return false;
    }

    public boolean getIsDownloading() {
        return false;
    }

    public boolean getAcceptsTextures() {
        return true;
    }

    public void updateAvaliableTexturePacks() {
    }
}
