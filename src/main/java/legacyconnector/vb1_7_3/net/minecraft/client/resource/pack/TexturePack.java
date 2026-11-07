package legacyconnector.vb1_7_3.net.minecraft.client.resource.pack;

public class TexturePack {
    public java.io.InputStream getResource(String path){
        byte[] bytes=org.kylepitcock.legacyconnector.vb1_7_3.AssetsB173.resourceBytes(path);
        return bytes==null?null:new java.io.ByteArrayInputStream(bytes);
    }
}
