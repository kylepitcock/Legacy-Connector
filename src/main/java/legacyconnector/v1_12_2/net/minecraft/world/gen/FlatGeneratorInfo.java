package legacyconnector.v1_12_2.net.minecraft.world.gen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;

/** A superflat preset ({@code 3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village}): layers, biome id and features. */
public class FlatGeneratorInfo {
    private final List<FlatLayerInfo> flatLayers = new ArrayList<>();
    private final Map<String, Map<String, String>> worldFeatures = new HashMap<>();
    private int biomeToUse;

    public FlatGeneratorInfo() {
    }

    public int getBiome() {
        return biomeToUse;
    }

    public void setBiome(int biome) {
        this.biomeToUse = biome;
    }

    public Map<String, Map<String, String>> getWorldFeatures() {
        return worldFeatures;
    }

    public List<FlatLayerInfo> getFlatLayers() {
        return flatLayers;
    }

    /** Stacks the layers: each starts where the one below ends. */
    public void updateLayers() {
        int y = 0;
        for (FlatLayerInfo layer : flatLayers) {
            layer.setMinY(y);
            y += layer.getLayerCount();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("3;");
        for (int i = 0; i < flatLayers.size(); ++i) {
            if (i > 0) sb.append(',');
            sb.append(flatLayers.get(i));
        }
        sb.append(';').append(biomeToUse).append(';');
        int n = 0;
        for (var e : worldFeatures.entrySet()) {
            if (n++ > 0) sb.append(',');
            sb.append(e.getKey().toLowerCase(java.util.Locale.ROOT));
            Map<String, String> options = e.getValue();
            if (!options.isEmpty()) {
                sb.append('(');
                int m = 0;
                for (var o : options.entrySet()) {
                    if (m++ > 0) sb.append(' ');
                    sb.append(o.getKey()).append('=').append(o.getValue());
                }
                sb.append(')');
            }
        }
        return sb.toString();
    }

    /** One layer: {@code [count*]name[:meta]}, the name with or without its domain. */
    private static FlatLayerInfo parseLayer(String text) {
        int count = 1;
        int star = text.indexOf('*');
        if (star > 0) {
            count = Integer.parseInt(text.substring(0, star));
            text = text.substring(star + 1);
        }
        String[] parts = text.split(":");
        int meta = 0;
        String name;
        if (parts.length >= 3) {
            name = parts[0] + ":" + parts[1];
            meta = Integer.parseInt(parts[2]);
        } else if (parts.length == 2 && parts[1].chars().allMatch(Character::isDigit)) {
            name = "minecraft:" + parts[0];
            meta = Integer.parseInt(parts[1]);
        } else {
            name = parts.length == 2 ? parts[0] + ":" + parts[1] : "minecraft:" + parts[0];
        }
        Block block = Block.getBlockFromName(name);
        return block == null ? null : new FlatLayerInfo(3, count, block, meta);
    }

    /** Reads a version 3 preset string; the default preset for anything it cannot read. */
    public static FlatGeneratorInfo createFlatGeneratorFromString(String preset) {
        if (preset == null) return getDefaultFlatGenerator();
        String[] parts = preset.split(";", -1);
        if (parts.length < 2 || !"3".equals(parts[0])) return getDefaultFlatGenerator();
        FlatGeneratorInfo info = new FlatGeneratorInfo();
        try {
            for (String layer : parts[1].split(",")) {
                if (layer.isBlank()) continue;
                FlatLayerInfo l = parseLayer(layer.trim());
                if (l == null) return getDefaultFlatGenerator();
                info.flatLayers.add(l);
            }
            info.updateLayers();
            info.biomeToUse = parts.length > 2 && !parts[2].isBlank() ? Integer.parseInt(parts[2].trim()) : 1;
            if (parts.length > 3) {
                for (String feature : parts[3].toLowerCase(java.util.Locale.ROOT).split(",")) {
                    if (feature.isBlank()) continue;
                    String[] f = feature.split("\\(", 2);
                    Map<String, String> options = new HashMap<>();
                    if (f.length > 1 && f[1].endsWith(")")) {
                        for (String o : f[1].substring(0, f[1].length() - 1).split(" ")) {
                            String[] kv = o.split("=", 2);
                            if (kv.length == 2) options.put(kv[0], kv[1]);
                        }
                    }
                    info.worldFeatures.put(f[0], options);
                }
            } else {
                info.worldFeatures.put("village", new HashMap<>());
            }
        } catch (RuntimeException e) {
            return getDefaultFlatGenerator();
        }
        return info;
    }

    /** Bedrock, two dirt, grass; plains; villages. */
    public static FlatGeneratorInfo getDefaultFlatGenerator() {
        FlatGeneratorInfo info = new FlatGeneratorInfo();
        info.setBiome(1);
        String[] names = {"minecraft:bedrock", "minecraft:dirt", "minecraft:grass"};
        int[] counts = {1, 2, 1};
        for (int i = 0; i < names.length; i++) {
            Block b = Block.getBlockFromName(names[i]);
            if (b != null) info.flatLayers.add(new FlatLayerInfo(3, counts[i], b));
        }
        info.updateLayers();
        info.worldFeatures.put("village", new HashMap<>());
        return info;
    }
}
