package legacyconnector.vb1_7_3.net.minecraft.client.render.texture;

import legacyconnector.vb1_7_3.net.minecraft.client.texture.TextureManager;

/** Beta TextureFX: RGBA bytes for one animated atlas tile. */
public class DynamicTexture {
    public byte[] pixels=new byte[16*16*4];
    public int sprite,atlas,copyTo,replicate=1;
    public boolean anaglyph;
    public DynamicTexture(int sprite){this.sprite=sprite;}
    public void tick(){}
    public void bind(TextureManager manager){manager.bindTexture(manager.getTextureId(atlas==0?"/terrain.png":"/gui/items.png"));}
}
