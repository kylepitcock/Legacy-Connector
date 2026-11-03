package legacyconnector.v1_7_10.net.minecraft.world.gen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;

/** A parsed superflat preset ({@code "3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village"}): layers, biome and features. */
public class FlatGeneratorInfo {
    private final List<FlatLayerInfo> flatLayers = new ArrayList<>();
    private final Map<String, Map<String, String>> worldFeatures = new HashMap<>();
    private int biomeToUse;

    public int getBiome() {
        return biomeToUse;
    }

    public void setBiome(int biome) {
        this.biomeToUse = biome;
    }

    /** Feature name to its parameters. */
    public Map func_82644_b() {
        return worldFeatures;
    }

    public List getFlatLayers() {
        return flatLayers;
    }

    /** Numbers the layers' lowest Y from the bottom. */
    public void func_82645_d() {
        int y = 0;
        for (FlatLayerInfo layer : flatLayers) {
            layer.setMinY(y);
            y += layer.getLayerCount();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("3;");
        for (int i = 0; i < flatLayers.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(flatLayers.get(i));
        }
        sb.append(';').append(biomeToUse);
        if (!worldFeatures.isEmpty()) {
            sb.append(';');
            boolean first = true;
            for (var e : worldFeatures.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(e.getKey().toLowerCase(Locale.ROOT));
                if (!e.getValue().isEmpty()) {
                    sb.append('(');
                    boolean firstParam = true;
                    for (var p : e.getValue().entrySet()) {
                        if (!firstParam) sb.append(' ');
                        firstParam = false;
                        sb.append(p.getKey()).append('=').append(p.getValue());
                    }
                    sb.append(')');
                }
            }
        }
        return sb.toString();
    }

    public static FlatGeneratorInfo getDefaultFlatGenerator() {
        FlatGeneratorInfo info = new FlatGeneratorInfo();
        info.setBiome(1);
        info.flatLayers.add(new FlatLayerInfo(1, Blocks.bedrock));
        info.flatLayers.add(new FlatLayerInfo(2, Blocks.dirt));
        info.flatLayers.add(new FlatLayerInfo(1, Blocks.grass));
        info.func_82645_d();
        info.worldFeatures.put("village", new HashMap<>());
        return info;
    }

    /** Parses a preset (versions 0 to 3); the default preset when it is malformed. */
    public static FlatGeneratorInfo createFlatGeneratorFromString(String preset) {
        if (preset == null || preset.isEmpty()) return getDefaultFlatGenerator();
        String[] parts = preset.split(";", -1);
        int version = parts.length == 1 ? 0 : parseInt(parts[0], 0);
        if (version < 0 || version > 3) return getDefaultFlatGenerator();
        FlatGeneratorInfo info = new FlatGeneratorInfo();
        int at = parts.length == 1 ? 0 : 1;
        int y = 0;
        for (String layer : parts[at++].split(",")) {
            String[] count = version >= 3 ? layer.split("\\*", 2) : layer.split("x", 2);
            int thickness = 1;
            int meta = 0;
            Block block;
            try {
                if (count.length == 2) thickness = Math.max(0, Math.min(Integer.parseInt(count[0].trim()), 256 - y));
                String id = count[count.length - 1].trim();
                if (version < 3) {
                    String[] p = id.split(":", 2);
                    if (p.length > 1) meta = Integer.parseInt(p[1]);
                    block = Block.getBlockById(Integer.parseInt(p[0]));
                } else {
                    String[] p = id.split(":", 3);
                    block = p.length > 1 ? Block.getBlockFromName(p[0] + ":" + p[1]) : null;
                    if (block != null) {
                        meta = p.length > 2 ? Integer.parseInt(p[2]) : 0;
                    } else {
                        block = Block.getBlockFromName(p[0]);
                        if (block != null) meta = p.length == 2 ? Integer.parseInt(p[1]) : 0;
                    }
                }
            } catch (RuntimeException e) {
                return getDefaultFlatGenerator();
            }
            if (block == null) return getDefaultFlatGenerator();
            if (meta < 0 || meta > 15) meta = 0;
            FlatLayerInfo layerInfo = new FlatLayerInfo(thickness, block, meta);
            layerInfo.setMinY(y);
            y += thickness;
            info.flatLayers.add(layerInfo);
        }
        info.biomeToUse = version > 0 && parts.length > at ? parseInt(parts[at++], 1) : 1;
        if (version > 0 && parts.length > at) {
            for (String feature : parts[at].split(",")) {
                String[] nameAndParams = feature.split("\\(", 2);
                String name = nameAndParams[0].toLowerCase(Locale.ROOT);
                if (name.isEmpty()) continue;
                Map<String, String> params = new HashMap<>();
                if (nameAndParams.length > 1 && nameAndParams[1].endsWith(")")) {
                    String inner = nameAndParams[1].substring(0, nameAndParams[1].length() - 1);
                    for (String kv : inner.split(" ")) {
                        String[] pair = kv.split("=", 2);
                        if (pair.length == 2) params.put(pair[0], pair[1]);
                    }
                }
                info.worldFeatures.put(name, params);
            }
        } else {
            info.worldFeatures.put("village", new HashMap<>());
        }
        return info;
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
