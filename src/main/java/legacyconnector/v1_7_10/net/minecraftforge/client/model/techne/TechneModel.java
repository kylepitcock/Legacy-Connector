package legacyconnector.v1_7_10.net.minecraftforge.client.model.techne;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_7_10.net.minecraft.client.model.ModelRenderer;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.net.minecraftforge.client.model.IModelCustom;
import legacyconnector.v1_7_10.net.minecraftforge.client.model.ModelFormatException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.kylepitcock.legacyconnector.v1_7_10.client.LegacySprites;

/**
 * Forge's Techne (.tcn) model: a zip holding {@code model.xml}, whose cube shapes become {@link ModelRenderer}s keyed by
 * shape name. Mods read {@code parts} reflectively (EnderCore bakes it into block quads).
 */
public class TechneModel implements IModelCustom {
    private static final String CUBE_TYPE = "d9e621f7-957f-4b77-b1ae-20dcd0da7751";

    private final ModelBase parentModel = new ModelBase() {};
    private Map<String, ModelRenderer> parts = new LinkedHashMap<>();
    private String fileName;
    private String texture;
    private int textureWidth = 64;
    private int textureHeight = 32;

    public TechneModel(ResourceLocation resource) throws ModelFormatException {
        this.fileName = resource.toString();
        try (InputStream in = new LegacySprites.ModResources().getResource(resource).getInputStream()) {
            load(in);
        } catch (ModelFormatException e) {
            throw e;
        } catch (Exception e) {
            throw new ModelFormatException("IO Exception reading model format: " + fileName, e);
        }
    }

    private void load(InputStream in) throws Exception {
        byte[] xml = null;
        try (ZipInputStream zip = new ZipInputStream(in)) {
            for (ZipEntry e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
                if (e.getName().equals("model.xml")) {
                    xml = zip.readAllBytes();
                } else if (e.getName().endsWith(".png")) {
                    texture = e.getName();
                }
            }
        }
        if (xml == null) throw new ModelFormatException("Model " + fileName + " contains no model.xml file");
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml));
        NodeList sizes = doc.getElementsByTagName("TextureSize");
        if (sizes.getLength() > 0) {
            String[] wh = sizes.item(0).getTextContent().trim().split(",");
            if (wh.length == 2) {
                textureWidth = Integer.parseInt(wh[0].trim());
                textureHeight = Integer.parseInt(wh[1].trim());
            }
        }
        parentModel.textureWidth = textureWidth;
        parentModel.textureHeight = textureHeight;
        NodeList shapes = doc.getElementsByTagName("Shape");
        for (int i = 0; i < shapes.getLength(); i++) {
            Element shape = (Element) shapes.item(i);
            String type = shape.getAttribute("Type");
            if (!type.equals(CUBE_TYPE)) continue;
            String name = shape.getAttribute("Name");
            float[] position = floats(child(shape, "Position"));
            float[] offset = floats(child(shape, "Offset"));
            float[] rotation = floats(child(shape, "Rotation"));
            float[] size = floats(child(shape, "Size"));
            float[] texOffset = floats(child(shape, "TextureOffset"));
            if (position == null || offset == null || size == null || texOffset == null) continue;
            ModelRenderer cube = new ModelRenderer(parentModel, name);
            cube.setTextureSize(textureWidth, textureHeight);
            cube.setTextureOffset((int) texOffset[0], (int) texOffset[1]);
            Node mirrored = child(shape, "IsMirrored");
            cube.mirror = mirrored != null && Boolean.parseBoolean(mirrored.getTextContent().trim());
            cube.addBox(offset[0], offset[1], offset[2], (int) size[0], (int) size[1], (int) size[2]);
            cube.setRotationPoint(position[0], position[1], position[2]);
            if (rotation != null) {
                cube.rotateAngleX = (float) Math.toRadians(rotation[0]);
                cube.rotateAngleY = (float) Math.toRadians(rotation[1]);
                cube.rotateAngleZ = (float) Math.toRadians(rotation[2]);
            }
            parts.put(name, cube);
        }
    }

    private static Node child(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        return list.getLength() == 0 ? null : list.item(0);
    }

    private static float[] floats(Node n) {
        if (n == null) return null;
        String[] s = n.getTextContent().trim().split(",");
        float[] out = new float[s.length];
        for (int i = 0; i < s.length; i++) out[i] = Float.parseFloat(s[i].trim());
        return out.length >= 2 ? out : null;
    }

    public Map<String, ModelRenderer> getParts() {
        return Collections.unmodifiableMap(parts);
    }

    @Override
    public String getType() {
        return "tcn";
    }

    @Override
    public void renderAll() {
        for (ModelRenderer part : parts.values()) part.render(0.0625F);
    }

    @Override
    public void renderOnly(String... groupNames) {
        for (ModelRenderer part : parts.values()) {
            for (String name : groupNames) {
                if (name.equalsIgnoreCase(part.boxName)) part.render(0.0625F);
            }
        }
    }

    @Override
    public void renderPart(String partName) {
        ModelRenderer part = parts.get(partName);
        if (part != null) part.render(0.0625F);
    }

    @Override
    public void renderAllExcept(String... excludedGroupNames) {
        for (ModelRenderer part : parts.values()) {
            boolean skip = false;
            for (String name : excludedGroupNames) skip |= name.equalsIgnoreCase(part.boxName);
            if (!skip) part.render(0.0625F);
        }
    }
}
