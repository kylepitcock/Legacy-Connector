package legacyconnector.v1_6_4.net.minecraft.src;

/** ModLoader's villager trade description ({@code ModLoader.addTrade}); kept as data. */
public class TradeEntry {
    public int id;
    public boolean buying;
    public float chance = 1.0F;
    public int min = 1;
    public int max = 1;

    public TradeEntry(int id, boolean buying, float chance, int min, int max) {
        this.id = id;
        this.buying = buying;
        this.chance = chance;
        this.min = min;
        this.max = max;
    }

    public TradeEntry(int id, boolean buying) {
        this.id = id;
        this.buying = buying;
    }

    public TradeEntry(int id, int min, int max) {
        this.id = id;
        this.min = min;
        this.max = max;
    }
}
