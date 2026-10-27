package legacyconnector.v1_12_2.net.minecraftforge.client.model.obj;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.BakedQuad;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemOverrideList;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormatElement;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResource;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.client.model.IModel;
import legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader;
import legacyconnector.v1_12_2.net.minecraftforge.client.model.PerspectiveMapWrapper;
import legacyconnector.v1_12_2.net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelPart;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelState;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.TRSRTransformation;
import legacyconnector.v1_12_2.net.minecraftforge.common.property.IExtendedBlockState;
import legacyconnector.v1_12_2.net.minecraftforge.common.property.IUnlistedProperty;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Forge 1.12.2 Wavefront OBJ model, ported: the parsed geometry (groups of faces of vertices with texture coordinates and normals),
 * the material library (colours and textures, which {@link #retexture} replaces by material or texture name), group visibility through
 * {@link OBJState}, and {@link OBJBakedModel}, which bakes the visible groups into legacy quads. Mods (Immersive Engineering,
 * Mekanism) extend these classes and read the structures directly. Blockstates that just name an .obj file are drawn by NeoForge's own
 * OBJ loader instead (see {@code ObjAssets1122}).
 */
public class OBJModel implements IModel {
    private final Set<ResourceLocation> textures = new HashSet<>();
    private final MaterialLibrary matLib;
    private final ResourceLocation modelLocation;
    private CustomData customData;

    public OBJModel(MaterialLibrary matLib, ResourceLocation modelLocation) {
        this(matLib, modelLocation, new CustomData());
    }

    public OBJModel(MaterialLibrary matLib, ResourceLocation modelLocation, CustomData customData) {
        this.matLib = matLib;
        this.modelLocation = modelLocation;
        this.customData = customData;
        for (Material m : matLib.materials.values()) {
            String path = m.getTexture().getPath();
            if (!path.startsWith("#") && !m.getTexture().equals(Texture.WHITE)) textures.add(new ResourceLocation(path));
        }
    }

    @Override
    public Collection<ResourceLocation> getTextures() {
        return ImmutableList.copyOf(textures);
    }

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        ImmutableMap.Builder<String, TextureAtlasSprite> builder = ImmutableMap.builder();
        builder.put(ModelLoader.White.LOCATION.toString(), ModelLoader.White.INSTANCE);
        TextureAtlasSprite missing = bakedTextureGetter.apply(new ResourceLocation("missingno"));
        for (Map.Entry<String, Material> e : matLib.materials.entrySet()) {
            Texture t = e.getValue().getTexture();
            if (t.equals(Texture.WHITE) || t.getPath().startsWith("#")) {
                builder.put(e.getKey(), t.getPath().startsWith("#") ? missing : ModelLoader.White.INSTANCE);
            } else {
                TextureAtlasSprite sprite = bakedTextureGetter.apply(t.getTextureLocation());
                builder.put(e.getKey(), sprite != null ? sprite : missing);
            }
        }
        builder.put("missingno", missing);
        return new OBJBakedModel(this, state, format, builder.buildKeepingLast());
    }

    public MaterialLibrary getMatLib() {
        return matLib;
    }

    public ResourceLocation getModelLocation() {
        return modelLocation;
    }

    @Override
    public IModel process(ImmutableMap<String, String> customData) {
        CustomData data = new CustomData(this.customData, customData);
        return new OBJModel(matLib, modelLocation, data);
    }

    @Override
    public IModel retexture(ImmutableMap<String, String> textures) {
        return new OBJModel(matLib.makeLibWithReplacements(textures), modelLocation, customData);
    }

    @Override
    public IModelState getDefaultState() {
        return TRSRTransformation.identity();
    }

    @Override
    public String toString() {
        return modelLocation.toString();
    }

    // ------------------------------------------------------------------ custom data

    static class CustomData {
        public boolean ambientOcclusion = true;
        public boolean gui3d = true;
        public boolean flipV = false;

        CustomData() {
        }

        CustomData(CustomData parent, ImmutableMap<String, String> customData) {
            this.ambientOcclusion = parent.ambientOcclusion;
            this.gui3d = parent.gui3d;
            this.flipV = parent.flipV;
            for (Map.Entry<String, String> e : customData.entrySet()) {
                boolean value = Boolean.parseBoolean(e.getValue().trim());
                switch (e.getKey()) {
                    case "ambient" -> ambientOcclusion = value;
                    case "gui3d" -> gui3d = value;
                    case "flip-v" -> flipV = value;
                    default -> {
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ parser

    /** Reads an .obj file (and its material libraries) through a legacy resource manager. */
    public static class Parser {
        private static final String DEFAULT_GROUP = Group.DEFAULT_NAME;
        private final MaterialLibrary materialLibrary = new MaterialLibrary();
        private final IResourceManager manager;
        private final ResourceLocation objFrom;
        private final List<Vertex> vertices = new ArrayList<>();
        private final List<Normal> normals = new ArrayList<>();
        private final List<TextureCoordinate> texCoords = new ArrayList<>();

        public Parser(IResource from, IResourceManager manager) throws IOException {
            this.manager = manager;
            this.objFrom = from.getResourceLocation();
            this.mcc$text = read(from);
        }

        private final String mcc$text;

        private static String read(IResource resource) throws IOException {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append('\n');
                return sb.toString();
            }
        }

        public OBJModel parse() throws IOException {
            Material material = materialLibrary.getMaterial(Material.DEFAULT_NAME);
            List<String> groupNames = new ArrayList<>(List.of(DEFAULT_GROUP));
            String pending = "";
            for (String raw : mcc$text.split("\n")) {
                String line = raw.trim();
                if (line.endsWith("\\")) {
                    pending += line.substring(0, line.length() - 1) + " ";
                    continue;
                }
                line = pending + line;
                pending = "";
                if (line.isEmpty() || line.startsWith("#")) continue;
                int space = line.indexOf(' ');
                String key = space < 0 ? line : line.substring(0, space);
                String data = space < 0 ? "" : line.substring(space + 1).trim();
                String[] parts = data.isEmpty() ? new String[0] : data.split("\\s+");
                switch (key) {
                    case "mtllib" -> materialLibrary.parseMaterialLibrary(manager, data, objFrom);
                    case "usemtl" -> {
                        Material m = materialLibrary.materials.get(data);
                        material = m != null ? m : materialLibrary.getMaterial(Material.DEFAULT_NAME);
                    }
                    case "v" -> vertices.add(new Vertex(parseVector4(parts), material));
                    case "vn" -> normals.add(new Normal(parseVector3(parts)));
                    case "vt" -> texCoords.add(new TextureCoordinate(parseVector3(parts)));
                    case "f" -> {
                        if (parts.length < 3) break;
                        Vertex[] verts = new Vertex[parts.length];
                        for (int i = 0; i < parts.length; i++) verts[i] = faceVertex(parts[i], material);
                        for (Face face : toFaces(verts, material.getName())) {
                            for (String g : groupNames) materialLibrary.getGroups().computeIfAbsent(g, Group::new).addFace(face);
                        }
                    }
                    case "g", "o" -> {
                        groupNames = new ArrayList<>();
                        if (parts.length == 0) groupNames.add(DEFAULT_GROUP);
                        else groupNames.addAll(Arrays.asList(parts));
                        for (String g : groupNames) materialLibrary.getGroups().computeIfAbsent(g, Group::new);
                    }
                    default -> materialLibrary.unknownObjectCommands.add(key);
                }
            }
            return new OBJModel(materialLibrary, objFrom);
        }

        /** v, v/vt, v//vn or v/vt/vn (1-based; negative counts back from the last element). */
        private Vertex faceVertex(String token, Material material) {
            String[] idx = token.split("/");
            Vertex base = vertices.get(index(idx[0], vertices.size()));
            Vertex v = new Vertex(new Vector4f(base.getPos()), material);
            if (idx.length > 1 && !idx[1].isEmpty()) v.setTextureCoordinate(texCoords.get(index(idx[1], texCoords.size())));
            if (idx.length > 2 && !idx[2].isEmpty()) v.setNormal(normals.get(index(idx[2], normals.size())));
            return v;
        }

        private static int index(String s, int size) {
            int i = Integer.parseInt(s.trim());
            return i < 0 ? size + i : i - 1;
        }

        /** Triangles and quads as they are (a triangle repeats its last vertex), larger polygons as a fan of quads. */
        private static List<Face> toFaces(Vertex[] verts, String material) {
            List<Face> faces = new ArrayList<>();
            if (verts.length == 3) faces.add(new Face(new Vertex[] {verts[0], verts[1], verts[2], verts[2]}, material));
            else if (verts.length == 4) faces.add(new Face(verts, material));
            else {
                for (int i = 1; i + 1 < verts.length; i += 2) {
                    Vertex last = i + 2 < verts.length ? verts[i + 2] : verts[i + 1];
                    faces.add(new Face(new Vertex[] {verts[0], verts[i], verts[i + 1], last}, material));
                }
            }
            return faces;
        }

        private static float f(String[] parts, int i, float def) {
            return i < parts.length ? Float.parseFloat(parts[i]) : def;
        }

        private static Vector4f parseVector4(String[] parts) {
            return new Vector4f(f(parts, 0, 0.0F), f(parts, 1, 0.0F), f(parts, 2, 0.0F), f(parts, 3, 1.0F));
        }

        private static Vector3f parseVector3(String[] parts) {
            return new Vector3f(f(parts, 0, 0.0F), f(parts, 1, 0.0F), f(parts, 2, 0.0F));
        }
    }

    // ------------------------------------------------------------------ materials and groups

    public static class MaterialLibrary {
        private Set<String> unknownObjectCommands = new HashSet<>();
        public Map<String, Material> materials = new LinkedHashMap<>();
        private Map<String, Group> groups = new LinkedHashMap<>();

        public MaterialLibrary() {
            this.groups.put(Group.DEFAULT_NAME, new Group(Group.DEFAULT_NAME));
            Material def = new Material();
            def.setName(Material.DEFAULT_NAME);
            this.materials.put(Material.DEFAULT_NAME, def);
        }

        public MaterialLibrary makeLibWithReplacements(ImmutableMap<String, String> replacements) {
            Map<String, Material> mats = new LinkedHashMap<>();
            for (Map.Entry<String, Material> e : materials.entrySet()) {
                String keyMat = e.getKey().startsWith("#") ? e.getKey() : "#" + e.getKey();
                String keyTex = e.getValue().getTexture().getPath();
                if (keyTex.endsWith(".png")) keyTex = keyTex.substring(0, keyTex.length() - 4);
                if (!keyTex.startsWith("#")) keyTex = "#" + keyTex;
                String replacement = replacements.containsKey(keyMat) ? replacements.get(keyMat) : replacements.get(keyTex);
                if (replacement != null) {
                    Texture current = e.getValue().getTexture();
                    Texture texture = new Texture(replacement, current.position, current.scale, current.rotation);
                    mats.put(e.getKey(), new Material(e.getValue().getColor(), texture, e.getValue().getName()));
                } else {
                    mats.put(e.getKey(), e.getValue());
                }
            }
            MaterialLibrary ret = new MaterialLibrary();
            ret.unknownObjectCommands = unknownObjectCommands;
            ret.groups = groups;
            ret.materials = mats;
            return ret;
        }

        public Map<String, Group> getGroups() {
            return groups;
        }

        public List<Group> getGroupsContainingFace(Face face) {
            List<Group> out = new ArrayList<>();
            for (Group g : groups.values()) if (g.faces.contains(face)) out.add(g);
            return out;
        }

        public void changeMaterialColor(String name, int color) {
            Vector4f c = new Vector4f((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, (color >> 24 & 255) / 255.0F);
            Material m = materials.get(name);
            if (m != null) m.setColor(c);
        }

        public Material getMaterial(String name) {
            return materials.get(name);
        }

        public ImmutableList<String> getMaterialNames() {
            return ImmutableList.copyOf(materials.keySet());
        }

        /** Reads {@code newmtl}, {@code Kd}, {@code map_Kd} and {@code d}/{@code Tr} from an .mtl next to the .obj (or a full location). */
        public void parseMaterialLibrary(IResourceManager manager, String path, ResourceLocation from) throws IOException {
            String domain = from.getNamespace();
            String objPath = from.getPath();
            ResourceLocation location;
            if (path.contains(":")) location = new ResourceLocation(path);
            else {
                int slash = objPath.lastIndexOf('/');
                location = new ResourceLocation(domain, (slash < 0 ? "" : objPath.substring(0, slash + 1)) + path);
            }
            String text;
            try (IResource resource = manager.getResource(location)) {
                text = Parser.read(resource);
            }
            Material material = new Material();
            material.setName(Material.DEFAULT_NAME);
            boolean hasSetTexture = false, hasSetColor = false;
            for (String raw : text.split("\n")) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int space = line.indexOf(' ');
                String key = space < 0 ? line : line.substring(0, space);
                String data = space < 0 ? "" : line.substring(space + 1).trim();
                switch (key) {
                    case "newmtl" -> {
                        hasSetColor = false;
                        hasSetTexture = false;
                        material = new Material();
                        material.setName(data);
                        materials.put(data, material);
                    }
                    case "Ka", "Kd", "Ks" -> {
                        if (key.equals("Kd") || !hasSetColor) {
                            String[] rgb = data.split("\\s+");
                            if (rgb.length >= 3) {
                                material.setColor(new Vector4f(Float.parseFloat(rgb[0]), Float.parseFloat(rgb[1]), Float.parseFloat(rgb[2]), material.getColor().w));
                                hasSetColor = true;
                            }
                        }
                    }
                    case "map_Ka", "map_Kd", "map_Ks" -> {
                        if (key.equals("map_Kd") || !hasSetTexture) {
                            String tex = data;
                            if (!tex.contains(":") && !tex.startsWith("#")) tex = domain + ":" + tex;
                            material.setTexture(new Texture(tex));
                            hasSetTexture = true;
                        }
                    }
                    case "d", "Tr" -> {
                        float alpha = Float.parseFloat(data.split("\\s+")[0]);
                        Vector4f c = material.getColor();
                        material.setColor(new Vector4f(c.x, c.y, c.z, key.equals("Tr") ? 1.0F - alpha : alpha));
                    }
                    default -> {
                    }
                }
            }
        }
    }

    public static class Material {
        public static final String WHITE_NAME = "OBJModel.White.Texture.Name";
        public static final String DEFAULT_NAME = "OBJModel.Default.Texture.Name";
        private Vector4f color;
        private Texture texture = Texture.WHITE;
        private String name = DEFAULT_NAME;

        public Material() {
            this(new Vector4f(1.0F, 1.0F, 1.0F, 1.0F));
        }

        public Material(Vector4f color) {
            this(color, Texture.WHITE, WHITE_NAME);
        }

        public Material(Texture texture) {
            this(new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), texture, DEFAULT_NAME);
        }

        public Material(Vector4f color, Texture texture, String name) {
            this.color = color;
            this.texture = texture;
            this.name = name != null ? name : DEFAULT_NAME;
        }

        public void setName(String name) {
            this.name = name != null ? name : DEFAULT_NAME;
        }

        public String getName() {
            return name;
        }

        public void setColor(Vector4f color) {
            this.color = color;
        }

        public Vector4f getColor() {
            return color;
        }

        public void setTexture(Texture texture) {
            this.texture = texture;
        }

        public Texture getTexture() {
            return texture;
        }

        public boolean isWhite() {
            return texture.equals(Texture.WHITE);
        }

        @Override
        public String toString() {
            return "Material: name " + name + ", color " + color + ", texture " + texture;
        }
    }

    public static class Texture {
        public static final Texture WHITE = new Texture("builtin/white", new Vector2f(0, 0), new Vector2f(1, 1), 0);
        private String path;
        private Vector2f position;
        private Vector2f scale;
        private float rotation;

        public Texture(String path) {
            this(path, new Vector2f(0, 0), new Vector2f(1, 1), 0);
        }

        public Texture(String path, Vector2f position, Vector2f scale, float rotation) {
            this.path = path;
            this.position = position;
            this.scale = scale;
            this.rotation = rotation;
        }

        public ResourceLocation getTextureLocation() {
            String p = path;
            if (p.endsWith(".png")) p = p.substring(0, p.length() - 4);
            return new ResourceLocation(p);
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        public void setPosition(Vector2f position) {
            this.position = position;
        }

        public Vector2f getPosition() {
            return position;
        }

        public void setScale(Vector2f scale) {
            this.scale = scale;
        }

        public Vector2f getScale() {
            return scale;
        }

        public void setRotation(float rotation) {
            this.rotation = rotation;
        }

        public float getRotation() {
            return rotation;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Texture t && t.path.equals(path);
        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }

        @Override
        public String toString() {
            return "Texture: " + path;
        }
    }

    public static class Face {
        private Vertex[] verts = new Vertex[4];
        private Normal faceNormal;
        private String materialName = Material.DEFAULT_NAME;
        private boolean isSharingVertices = false;

        public Face(Vertex[] verts) {
            this(verts, Material.DEFAULT_NAME);
        }

        public Face(Vertex[] verts, String materialName) {
            this.verts = verts != null && verts.length > 2 ? verts : null;
            setMaterialName(materialName);
            checkUVsAndMaterial();
        }

        public void checkUVsAndMaterial() {
            if (verts == null) return;
            for (int i = 0; i < verts.length; i++) {
                if (verts[i].getTextureCoordinate() == null) verts[i].setTextureCoordinate(TextureCoordinate.getDefaultUVs()[i % 4]);
            }
        }

        public void setMaterialName(String materialName) {
            this.materialName = materialName != null && !materialName.isEmpty() ? materialName : Material.DEFAULT_NAME;
        }

        public String getMaterialName() {
            return materialName;
        }

        public boolean setVertices(Vertex[] verts) {
            if (verts == null) return false;
            this.verts = verts;
            checkUVsAndMaterial();
            return true;
        }

        public Vertex[] getVertices() {
            return verts;
        }

        public Normal getNormal() {
            if (faceNormal == null) {
                Vector3f a = new Vector3f(verts[2].getPos().x - verts[0].getPos().x, verts[2].getPos().y - verts[0].getPos().y, verts[2].getPos().z - verts[0].getPos().z);
                Vector3f b = new Vector3f(verts[3].getPos().x - verts[1].getPos().x, verts[3].getPos().y - verts[1].getPos().y, verts[3].getPos().z - verts[1].getPos().z);
                Vector3f n = new Vector3f();
                n.cross(a, b);
                if (n.lengthSquared() < 1.0E-12F) {
                    a = new Vector3f(verts[1].getPos().x - verts[0].getPos().x, verts[1].getPos().y - verts[0].getPos().y, verts[1].getPos().z - verts[0].getPos().z);
                    b = new Vector3f(verts[2].getPos().x - verts[0].getPos().x, verts[2].getPos().y - verts[0].getPos().y, verts[2].getPos().z - verts[0].getPos().z);
                    n.cross(a, b);
                }
                if (n.lengthSquared() > 0.0F) n.normalize();
                faceNormal = new Normal(n);
            }
            return faceNormal;
        }

        /** The face with its vertices (positions and normals) transformed. */
        public Face bake(TRSRTransformation transform) {
            Matrix4f m = transform.getMatrix();
            javax.vecmath.Matrix3f normalMatrix = transform.getNormalMatrix();
            Vertex[] out = new Vertex[verts.length];
            for (int i = 0; i < verts.length; i++) {
                Vertex v = verts[i];
                Vector4f pos = new Vector4f(v.getPos());
                m.transform(pos);
                Vertex nv = new Vertex(pos, v.getMaterial());
                nv.setTextureCoordinate(v.getTextureCoordinate());
                if (v.hasNormal()) {
                    Vector3f n = new Vector3f(v.getNormal().getNormal());
                    normalMatrix.transform(n);
                    if (n.lengthSquared() > 0.0F) n.normalize();
                    nv.setNormal(new Normal(n));
                }
                out[i] = nv;
            }
            return new Face(out, materialName);
        }
    }

    public static class Vertex {
        private Vector4f position;
        private Normal normal;
        private TextureCoordinate texCoord;
        private Material material = new Material();

        public Vertex(Vector4f position, Material material) {
            this.position = position;
            this.material = material;
        }

        public void setPos(Vector4f position) {
            this.position = position;
        }

        public Vector4f getPos() {
            return position;
        }

        public Vector3f getPos3() {
            return new Vector3f(position.x, position.y, position.z);
        }

        public boolean hasNormal() {
            return normal != null;
        }

        public void setNormal(Normal normal) {
            this.normal = normal;
        }

        public Normal getNormal() {
            return normal;
        }

        public boolean hasTextureCoordinate() {
            return texCoord != null;
        }

        public void setTextureCoordinate(TextureCoordinate texCoord) {
            this.texCoord = texCoord;
        }

        public TextureCoordinate getTextureCoordinate() {
            return texCoord;
        }

        public void setMaterial(Material material) {
            this.material = material;
        }

        public Material getMaterial() {
            return material;
        }

        @Override
        public String toString() {
            return "v: " + position + ", vn: " + normal + ", vt: " + texCoord;
        }
    }

    public static class Normal {
        public float x, y, z;

        public Normal() {
            this(0.0F, 0.0F, 0.0F);
        }

        public Normal(float[] data) {
            this(data[0], data[1], data[2]);
        }

        public Normal(Vector3f vector3f) {
            this(vector3f.x, vector3f.y, vector3f.z);
        }

        public Normal(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Vector3f getNormal() {
            return new Vector3f(x, y, z);
        }
    }

    public static class TextureCoordinate {
        public float u, v, w;

        public TextureCoordinate() {
            this(0.0F, 0.0F, 1.0F);
        }

        public TextureCoordinate(float[] data) {
            this(data[0], data[1], data[2]);
        }

        public TextureCoordinate(Vector3f data) {
            this(data.x, data.y, data.z);
        }

        public TextureCoordinate(float u, float v, float w) {
            this.u = u;
            this.v = v;
            this.w = w;
        }

        public Vector3f getData() {
            return new Vector3f(u, v, w);
        }

        public static TextureCoordinate[] getDefaultUVs() {
            return new TextureCoordinate[] {new TextureCoordinate(0.0F, 0.0F, 1.0F), new TextureCoordinate(1.0F, 0.0F, 1.0F), new TextureCoordinate(1.0F, 1.0F, 1.0F),
                new TextureCoordinate(0.0F, 1.0F, 1.0F)};
        }
    }

    public static class Group implements IModelPart {
        public static final String DEFAULT_NAME = "OBJModel.Default.Element.Name";
        public static final String ALL = "OBJModel.Group.All.Key";
        public static final String ALL_EXCEPT = "OBJModel.Group.All.Except.Key";
        private final String name;
        private LinkedHashSet<Face> faces = new LinkedHashSet<>();
        public float[] minUVBounds = new float[] {0.0F, 0.0F};
        public float[] maxUVBounds = new float[] {1.0F, 1.0F};

        public Group(String name) {
            this(name, null);
        }

        public Group(String name, LinkedHashSet<Face> faces) {
            this.name = name != null ? name : DEFAULT_NAME;
            this.faces = faces == null ? new LinkedHashSet<>() : faces;
        }

        public LinkedHashSet<Face> applyTransform(Optional<TRSRTransformation> transform) {
            LinkedHashSet<Face> out = new LinkedHashSet<>();
            for (Face f : faces) out.add(transform.isPresent() ? f.bake(transform.get()) : f);
            return out;
        }

        public String getName() {
            return name;
        }

        public LinkedHashSet<Face> getFaces() {
            return faces;
        }

        public void setFaces(LinkedHashSet<Face> faces) {
            this.faces = faces;
        }

        public void addFace(Face face) {
            faces.add(face);
        }

        public void addFaces(List<Face> faces) {
            this.faces.addAll(faces);
        }
    }

    // ------------------------------------------------------------------ state

    public static class OBJState implements IModelState {
        protected Map<String, Boolean> visibilityMap = new HashMap<>();
        public IModelState parent;
        protected Operation operation = Operation.SET_TRUE;

        public OBJState(List<String> visibleGroups, boolean visibility) {
            this(visibleGroups, visibility, TRSRTransformation.identity());
        }

        public OBJState(List<String> visibleGroups, boolean visibility, IModelState parent) {
            this.parent = parent;
            for (String s : visibleGroups) visibilityMap.put(s, visibility);
        }

        public IModelState getParent(IModelState parent) {
            if (parent == null) return null;
            if (parent instanceof OBJState s) return s.parent;
            return parent;
        }

        @Override
        public Optional<TRSRTransformation> apply(Optional<? extends IModelPart> part) {
            return parent != null ? parent.apply(part) : Optional.empty();
        }

        public Map<String, Boolean> getVisibilityMap() {
            return visibilityMap;
        }

        public List<String> getGroupsWithVisibility(boolean visibility) {
            List<String> out = new ArrayList<>();
            for (Map.Entry<String, Boolean> e : visibilityMap.entrySet()) if (e.getValue() == visibility) out.add(e.getKey());
            return out;
        }

        public List<String> getGroupNamesFromMap() {
            return new ArrayList<>(visibilityMap.keySet());
        }

        public void changeGroupVisibilities(List<String> names, Operation operation) {
            if (names == null || names.isEmpty()) return;
            this.operation = operation;
            if (names.get(0).equals(Group.ALL)) {
                for (String s : visibilityMap.keySet()) visibilityMap.put(s, operation.performOperation(visibilityMap.get(s)));
            } else if (names.get(0).equals(Group.ALL_EXCEPT)) {
                for (String s : visibilityMap.keySet()) if (!names.subList(1, names.size()).contains(s)) visibilityMap.put(s, operation.performOperation(visibilityMap.get(s)));
            } else {
                for (String s : names) visibilityMap.put(s, operation.performOperation(visibilityMap.getOrDefault(s, false)));
            }
        }

        @Override
        public String toString() {
            return "OBJState: visibility " + visibilityMap + ", parent " + parent;
        }

        public enum Operation {
            SET_TRUE, SET_FALSE, TOGGLE;

            public boolean performOperation(boolean valueToToggle) {
                return switch (this) {
                    case SET_TRUE -> true;
                    case SET_FALSE -> false;
                    case TOGGLE -> !valueToToggle;
                };
            }
        }
    }

    /** The unlisted property tile entities give their OBJState through (extended block state). */
    public enum OBJProperty implements IUnlistedProperty<OBJState> {
        INSTANCE;

        @Override
        public String getName() {
            return "OBJProperty";
        }

        @Override
        public boolean isValid(OBJState value) {
            return value != null;
        }

        @Override
        public Class<OBJState> getType() {
            return OBJState.class;
        }

        @Override
        public String valueToString(OBJState value) {
            return value.toString();
        }
    }

    // ------------------------------------------------------------------ baked

    public class OBJBakedModel implements IBakedModel {
        private final OBJModel model;
        private IModelState state;
        private final VertexFormat format;
        private final ImmutableMap<String, TextureAtlasSprite> textures;
        private volatile List<BakedQuad> quads;
        private TextureAtlasSprite sprite;

        public OBJBakedModel(OBJModel model, IModelState state, VertexFormat format, ImmutableMap<String, TextureAtlasSprite> textures) {
            this.model = model;
            this.state = state instanceof OBJState s && s.parent == null ? new OBJState(s.getGroupNamesFromMap(), true, TRSRTransformation.identity()) : state;
            if (state instanceof OBJState s) ((OBJState) this.state).visibilityMap = new HashMap<>(s.visibilityMap);
            this.format = format;
            this.textures = textures;
            this.sprite = textures.get("missingno");
            for (Material m : model.matLib.materials.values()) {
                TextureAtlasSprite t = textures.get(m.getName());
                if (t != null && !m.isWhite() && !m.getName().equals(Material.DEFAULT_NAME)) {
                    sprite = t;
                    break;
                }
            }
        }

        public void scheduleRebake() {
            quads = null;
        }

        @Override
        public List<BakedQuad> getQuads(IBlockState blockState, EnumFacing side, long rand) {
            if (side != null) return Collections.emptyList();
            if (blockState instanceof IExtendedBlockState ext) {
                OBJState s = null;
                try {
                    s = ext.getValue(OBJProperty.INSTANCE);
                } catch (RuntimeException e) {
                    // the block has no such property
                }
                if (s != null) return getCachedModel(s).getQuads(null, null, rand);
            }
            List<BakedQuad> q = quads;
            if (q == null) quads = q = buildQuads(state);
            return q;
        }

        private final Map<Map<String, Boolean>, OBJBakedModel> cache = new java.util.concurrent.ConcurrentHashMap<>();

        /** One baked model per distinct visibility (tile entities make a new OBJState for every query). */
        public OBJBakedModel getCachedModel(OBJState state) {
            Map<String, Boolean> key = new HashMap<>(state.visibilityMap);
            OBJBakedModel found = cache.get(key);
            if (found == null) {
                if (cache.size() > 256) cache.clear();
                found = new OBJBakedModel(model, state, format, textures);
                cache.put(key, found);
            }
            return found;
        }

        private List<BakedQuad> buildQuads(IModelState modelState) {
            List<BakedQuad> out = new ArrayList<>();
            Optional<TRSRTransformation> transform = modelState.apply(Optional.empty());
            Map<String, Boolean> visibility = modelState instanceof OBJState s ? s.visibilityMap : null;
            Set<Face> faces = new LinkedHashSet<>();
            for (Group g : model.matLib.getGroups().values()) {
                if (visibility != null && !visibility.isEmpty() && !visibility.getOrDefault(g.getName(), false)) continue;
                Optional<TRSRTransformation> groupTransform = modelState.apply(Optional.of(g));
                Optional<TRSRTransformation> t = groupTransform.isPresent() ? groupTransform : transform;
                faces.addAll(g.applyTransform(t));
            }
            for (Face f : faces) {
                if (f.getVertices() == null) continue;
                Material material = model.matLib.materials.get(f.getMaterialName());
                if (material == null) material = model.matLib.materials.get(Material.DEFAULT_NAME);
                TextureAtlasSprite tex = textures.get(f.getMaterialName());
                if (tex == null) tex = material != null && material.isWhite() ? ModelLoader.White.INSTANCE : sprite;
                UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);
                builder.setContractUVs(true);
                Normal faceNormal = f.getNormal();
                builder.setQuadOrientation(EnumFacing.getFacingFromVector(faceNormal.x, faceNormal.y, faceNormal.z));
                builder.setTexture(tex);
                Vertex[] verts = f.getVertices();
                for (int i = 0; i < 4; i++) putVertex(builder, verts[Math.min(i, verts.length - 1)], faceNormal, tex, material);
                out.add(builder.build());
            }
            return out;
        }

        private void putVertex(UnpackedBakedQuad.Builder builder, Vertex v, Normal faceNormal, TextureAtlasSprite tex, Material material) {
            Vector4f color = material != null ? material.getColor() : new Vector4f(1, 1, 1, 1);
            TextureCoordinate tc = v.hasTextureCoordinate() ? v.getTextureCoordinate() : TextureCoordinate.getDefaultUVs()[0];
            Normal n = v.hasNormal() ? v.getNormal() : faceNormal;
            List<VertexFormatElement> elements = format.getElements();
            for (int e = 0; e < elements.size(); e++) {
                VertexFormatElement el = elements.get(e);
                switch (el.getUsage()) {
                    case POSITION -> builder.put(e, v.getPos().x, v.getPos().y, v.getPos().z, 1.0F);
                    case COLOR -> builder.put(e, color.x, color.y, color.z, color.w);
                    case UV -> {
                        if (el.getIndex() == 0) {
                            float vv = model.customData.flipV ? 1.0F - tc.v : tc.v;
                            builder.put(e, tex.getInterpolatedU(tc.u * 16.0F), tex.getInterpolatedV(vv * 16.0F), 0.0F, 1.0F);
                        } else {
                            builder.put(e, 0.0F, 0.0F);
                        }
                    }
                    case NORMAL -> builder.put(e, n.x, n.y, n.z, 0.0F);
                    default -> builder.put(e);
                }
            }
        }

        @Override
        public boolean isAmbientOcclusion() {
            return model.customData.ambientOcclusion;
        }

        @Override
        public boolean isGui3d() {
            return model.customData.gui3d;
        }

        @Override
        public boolean isBuiltInRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return sprite;
        }

        @Override
        public ItemOverrideList getOverrides() {
            return ItemOverrideList.NONE;
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
            return PerspectiveMapWrapper.handlePerspective(this, state, cameraTransformType);
        }

        public OBJModel getModel() {
            return model;
        }

        public IModelState getState() {
            return state;
        }

        public OBJBakedModel getBakedModel() {
            return new OBJBakedModel(model, state, format, textures);
        }

        @Override
        public String toString() {
            return model.modelLocation.toString();
        }
    }
}
