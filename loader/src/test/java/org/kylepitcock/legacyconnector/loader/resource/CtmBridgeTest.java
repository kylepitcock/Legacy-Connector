package org.kylepitcock.legacyconnector.loader.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class CtmBridgeTest {
    private static byte[] sheet(int size) throws Exception {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < size; x++) for (int y = 0; y < size; y++) image.setRGB(x, y, 0xFF000000 | ((x < size / 2 ? 0 : 255) << 16));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static byte[] meta(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void randomSheetBecomesItsFirstTileAndGetsProperties() throws Exception {
        var converter = new LegacyResourceConverter();
        converter.accept("assets/chisel/textures/blocks/stone/chaotic.png", sheet(32));
        converter.accept("assets/chisel/textures/blocks/stone/chaotic.png.mcmeta", meta("{\"ctm\":{\"ctm_version\":1,\"type\":\"random\",\"extra\":{\"size\":2}}}"));
        Map<String, byte[]> out = converter.finish();
        BufferedImage sprite = ImageIO.read(new ByteArrayInputStream(out.get("assets/chisel/textures/blocks/stone/chaotic.png")));
        assertEquals(16, sprite.getWidth());
        String dir = "assets/minecraft/optifine/ctm/legacyconnector/chisel/blocks_stone_chaotic/";
        for (int i = 0; i < 4; i++) assertNotNull(out.get(dir + i + ".png"));
        String props = new String(out.get(dir + "ctm.properties"), StandardCharsets.UTF_8);
        assertTrue(props.contains("method=random"));
        assertTrue(props.contains("tiles=0-3"));
        assertTrue(props.contains("matchTiles=chisel:blocks/stone/chaotic"));
    }

    @Test
    void patternSheetRepeats() throws Exception {
        var converter = new LegacyResourceConverter();
        converter.accept("assets/chisel/textures/blocks/stone/array.png", sheet(32));
        converter.accept("assets/chisel/textures/blocks/stone/array.png.mcmeta", meta("{\"ctm\":{\"type\":\"pattern\",\"extra\":{\"size\":2}}}"));
        String props = new String(converter.finish().get("assets/minecraft/optifine/ctm/legacyconnector/chisel/blocks_stone_array/ctm.properties"), StandardCharsets.UTF_8);
        assertTrue(props.contains("method=repeat"));
        assertTrue(props.contains("width=2") && props.contains("height=2"));
    }

    @Test
    void otherTypesAndOddSheetsAreLeftAlone() throws Exception {
        var converter = new LegacyResourceConverter();
        byte[] png = sheet(16);
        converter.accept("assets/chisel/textures/blocks/a.png", png);
        converter.accept("assets/chisel/textures/blocks/a.png.mcmeta", meta("{\"ctm\":{\"type\":\"ctm\"}}"));
        converter.accept("assets/chisel/textures/blocks/b.png", sheet(30));
        converter.accept("assets/chisel/textures/blocks/b.png.mcmeta", meta("{\"ctm\":{\"type\":\"random\",\"extra\":{\"size\":4}}}"));
        Map<String, byte[]> out = converter.finish();
        assertEquals(png.length, out.get("assets/chisel/textures/blocks/a.png").length);
        assertFalse(out.keySet().stream().anyMatch(k -> k.startsWith("assets/minecraft/optifine/")));
    }
}
