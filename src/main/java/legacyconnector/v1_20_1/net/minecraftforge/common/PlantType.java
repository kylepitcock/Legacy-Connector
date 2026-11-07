package legacyconnector.v1_20_1.net.minecraftforge.common;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Forge 1.20.1's plant type ({@code IPlantable.getPlantType}); NeoForge 1.21.1 dropped plant types, the constants and {@code get} keep mod code linking. */
public final class PlantType {
    private static final Map<String, PlantType> VALUES = new ConcurrentHashMap<>();

    public static final PlantType CROP = get("crop");
    public static final PlantType NETHER = get("nether");
    public static final PlantType WATER = get("water");
    public static final PlantType BEACH = get("beach");
    public static final PlantType PLAINS = get("plains");
    public static final PlantType DESERT = get("desert");
    public static final PlantType CAVE = get("cave");

    private final String name;

    private PlantType(String name) {
        this.name = name;
    }

    public static PlantType get(String name) {
        return VALUES.computeIfAbsent(name.toLowerCase(Locale.ROOT), PlantType::new);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
