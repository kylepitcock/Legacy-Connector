package legacyconnector.vb1_7_3.net.minecraft.item;

/** Original Beta armor constructor state; the declared fields are also part of ModLoader's reflection API. */
public class ArmorItem extends Item {
    private static final int[] PROTECTION_BY_SLOT = {3, 8, 6, 3};
    private static final int[] DURABILITY_BY_SLOT = {11, 16, 15, 13};

    public final int type;
    public final int equipmentSlot;
    public final int maxProtection;
    public final int textureIndex;

    public ArmorItem(int id, int type, int textureIndex, int equipmentSlot) {
        this(id,type,textureIndex,equipmentSlot,false);
    }
    private ArmorItem(int id,int type,int textureIndex,int equipmentSlot,boolean vanillaView){
        super(id,vanillaView);
        this.type = type;
        this.equipmentSlot = equipmentSlot;
        this.textureIndex = textureIndex;
        this.maxProtection = PROTECTION_BY_SLOT[equipmentSlot];
        setMaxDamage((DURABILITY_BY_SLOT[equipmentSlot] * 3) << type);
        setMaxCount(1);
    }
    public static ArmorItem mcc$vanilla(int id,int type,int textureIndex,int equipmentSlot){return new ArmorItem(id,type,textureIndex,equipmentSlot,true);}
}
