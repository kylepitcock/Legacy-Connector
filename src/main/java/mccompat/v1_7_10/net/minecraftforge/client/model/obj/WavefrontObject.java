package mccompat.v1_7_10.net.minecraftforge.client.model.obj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mccompat.v1_7_10.net.minecraft.client.renderer.Tessellator;
import mccompat.v1_7_10.net.minecraft.util.ResourceLocation;
import mccompat.v1_7_10.net.minecraftforge.client.model.IModelCustom;
import mccompat.v1_7_10.net.minecraftforge.client.model.ModelFormatException;
import org.kylepitcock.mccompat.v1_7_10.client.LegacySprites;

/**
 * Forge's Wavefront OBJ model: groups ("g"/"o") of triangle or quad faces, drawn with the Tessellator.
 * Texture V is flipped as in Forge (OBJ's origin is bottom-left).
 */
public class WavefrontObject implements IModelCustom {
    private static final Pattern FACE_SPLIT = Pattern.compile("\\s+");

    public ArrayList<Vertex> vertices = new ArrayList<>();
    public ArrayList<Vertex> vertexNormals = new ArrayList<>();
    public ArrayList<TextureCoordinate> textureCoordinates = new ArrayList<>();
    public ArrayList<GroupObject> groupObjects = new ArrayList<>();
    private GroupObject currentGroupObject;
    private final String fileName;

    public WavefrontObject(ResourceLocation resource) throws ModelFormatException {
        this.fileName = resource.toString();
        try {
            InputStream in = new LegacySprites.ModResources().getResource(resource).getInputStream();
            loadObjModel(in);
        } catch (IOException e) {
            throw new ModelFormatException("IO Exception reading model format", e);
        }
    }

    public WavefrontObject(String fileName, InputStream inputStream) throws ModelFormatException {
        this.fileName = fileName;
        try {
            loadObjModel(inputStream);
        } catch (IOException e) {
            throw new ModelFormatException("IO Exception reading model format", e);
        }
    }

    private void loadObjModel(InputStream inputStream) throws ModelFormatException, IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            int lineCount = 0;
            for (String line; (line = reader.readLine()) != null; ) {
                lineCount++;
                line = line.replaceAll("\\s+", " ").trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                try {
                    if (line.startsWith("v ")) {
                        String[] t = line.substring(2).trim().split(" ");
                        vertices.add(new Vertex(Float.parseFloat(t[0]), Float.parseFloat(t[1]), Float.parseFloat(t[2])));
                    } else if (line.startsWith("vn ")) {
                        String[] t = line.substring(3).trim().split(" ");
                        vertexNormals.add(new Vertex(Float.parseFloat(t[0]), Float.parseFloat(t[1]), Float.parseFloat(t[2])));
                    } else if (line.startsWith("vt ")) {
                        String[] t = line.substring(3).trim().split(" ");
                        textureCoordinates.add(new TextureCoordinate(Float.parseFloat(t[0]), 1 - Float.parseFloat(t[1])));
                    } else if (line.startsWith("f ")) {
                        if (currentGroupObject == null) currentGroupObject = new GroupObject("Default");
                        Face face = parseFace(line.substring(2).trim().split(" "));
                        if (face != null) {
                            if (currentGroupObject.glDrawingMode == -1) {
                                currentGroupObject.glDrawingMode = face.vertices.length == 3 ? 4 : face.vertices.length == 4 ? 7 : 9;
                            }
                            currentGroupObject.faces.add(face);
                        }
                    } else if (line.startsWith("g ") || line.startsWith("o ") || line.equals("g")) {
                        String name = line.length() > 2 ? line.substring(2).trim() : "Default";
                        if (currentGroupObject != null) groupObjects.add(currentGroupObject);
                        currentGroupObject = new GroupObject(name);
                    }
                } catch (RuntimeException e) {
                    throw new ModelFormatException("Error in model " + fileName + " line " + lineCount + ": " + e);
                }
            }
            if (currentGroupObject != null) groupObjects.add(currentGroupObject);
        }
    }

    private Face parseFace(String[] tokens) {
        if (tokens.length < 3) return null;
        Face face = new Face();
        int n = tokens.length;
        face.vertices = new Vertex[n];
        face.textureCoordinates = new TextureCoordinate[n];
        face.vertexNormals = new Vertex[n];
        boolean hasTex = true, hasNormals = true;
        for (int i = 0; i < n; i++) {
            String[] p = tokens[i].split("/", -1);
            face.vertices[i] = vertices.get(index(p[0], vertices.size()));
            if (p.length > 1 && !p[1].isEmpty()) face.textureCoordinates[i] = textureCoordinates.get(index(p[1], textureCoordinates.size()));
            else hasTex = false;
            if (p.length > 2 && !p[2].isEmpty()) face.vertexNormals[i] = vertexNormals.get(index(p[2], vertexNormals.size()));
            else hasNormals = false;
        }
        if (!hasTex) face.textureCoordinates = null;
        if (!hasNormals) face.vertexNormals = null;
        return face;
    }

    /** OBJ indices are 1-based, negative ones count from the end. */
    private static int index(String token, int size) {
        int i = Integer.parseInt(token);
        return i < 0 ? size + i : i - 1;
    }

    @Override
    public String getType() {
        return "obj";
    }

    @Override
    public void renderAll() {
        Tessellator tessellator = Tessellator.instance;
        for (GroupObject group : groupObjects) {
            if (group.faces.isEmpty()) continue;
            tessellator.startDrawing(group.glDrawingMode);
            group.render(tessellator);
            tessellator.draw();
        }
    }

    public void tessellateAll(Tessellator tessellator) {
        for (GroupObject group : groupObjects) group.render(tessellator);
    }

    @Override
    public void renderOnly(String... groupNames) {
        for (GroupObject group : groupObjects) {
            for (String name : groupNames) {
                if (name.equalsIgnoreCase(group.name)) {
                    group.render();
                    break;
                }
            }
        }
    }

    public void tessellateOnly(Tessellator tessellator, String... groupNames) {
        for (GroupObject group : groupObjects) {
            for (String name : groupNames) {
                if (name.equalsIgnoreCase(group.name)) {
                    group.render(tessellator);
                    break;
                }
            }
        }
    }

    @Override
    public void renderPart(String partName) {
        for (GroupObject group : groupObjects) {
            if (partName.equalsIgnoreCase(group.name)) group.render();
        }
    }

    public void tessellatePart(Tessellator tessellator, String partName) {
        for (GroupObject group : groupObjects) {
            if (partName.equalsIgnoreCase(group.name)) group.render(tessellator);
        }
    }

    @Override
    public void renderAllExcept(String... excludedGroupNames) {
        for (GroupObject group : groupObjects) {
            boolean skip = false;
            for (String name : excludedGroupNames) {
                if (name.equalsIgnoreCase(group.name)) {
                    skip = true;
                    break;
                }
            }
            if (!skip) group.render();
        }
    }

    public void tessellateAllExcept(Tessellator tessellator, String... excludedGroupNames) {
        for (GroupObject group : groupObjects) {
            boolean skip = false;
            for (String name : excludedGroupNames) {
                if (name.equalsIgnoreCase(group.name)) {
                    skip = true;
                    break;
                }
            }
            if (!skip) group.render(tessellator);
        }
    }

    public String getFileName() {
        return fileName;
    }
}
