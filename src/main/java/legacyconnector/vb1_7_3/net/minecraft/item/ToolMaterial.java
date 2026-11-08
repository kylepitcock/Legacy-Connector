package legacyconnector.vb1_7_3.net.minecraft.item;

public enum ToolMaterial {
    WOOD(0,59,2,0), STONE(1,131,4,1), IRON(2,250,6,2), DIAMOND(3,1561,8,3), GOLD(0,32,12,0);
    private final int miningLevel, itemDurability, attackDamage;
    private final float miningSpeed;
    ToolMaterial(int level,int durability,float speed,int damage) {
        miningLevel=level;itemDurability=durability;miningSpeed=speed;attackDamage=damage;
    }
    public int getDurability() { return itemDurability; }
    public float getMiningSpeedMultiplier() { return miningSpeed; }
    public int getAttackDamage() { return attackDamage; }
    public int getMiningLevel() { return miningLevel; }
}
