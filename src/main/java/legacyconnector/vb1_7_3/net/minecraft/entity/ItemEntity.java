package legacyconnector.vb1_7_3.net.minecraft.entity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
public class ItemEntity extends Entity {
    public ItemStack stack;
    public int pickupDelay;
    public ItemEntity(World world) { super(world); }
    public ItemEntity(World world,double x,double y,double z,ItemStack stack) { this(world);this.stack=stack;setPosition(x,y,z); }
}
