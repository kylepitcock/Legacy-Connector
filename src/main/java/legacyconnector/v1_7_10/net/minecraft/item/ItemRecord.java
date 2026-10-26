package legacyconnector.v1_7_10.net.minecraft.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.net.minecraft.util.StatCollector;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * A music disc. {@link #recordName} names its song: the sound {@code records.<name>} (or what {@link #getRecordResource} maps it
 * to). A mod's disc goes into the vanilla jukebox, which then plays its sound once (the modern jukebox only knows the songs of
 * its registry, so it cannot stop or loop a mod's song).
 */
public class ItemRecord extends Item {
    private static final Map<String, ItemRecord> records = new HashMap<>();
    public final String recordName;

    public ItemRecord(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        String name = info.legacyName() == null ? "" : info.legacyName().substring(info.legacyName().indexOf(':') + 1);
        this.recordName = name.startsWith("record_") ? name.substring("record_".length()) : name;
        records.put(recordName, this);
    }

    protected ItemRecord(String name) {
        super();
        this.recordName = name;
        this.maxStackSize = 1;
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabMisc);
        records.put(name, this);
    }

    /** The disc registered for a song name, or null. */
    public static ItemRecord getRecord(String name) {
        return records.get(name);
    }

    /** The translated song title ({@code item.record.<name>.desc}). */
    public String getRecordNameLocal() {
        return StatCollector.translateToLocal("item.record." + recordName + ".desc");
    }

    /** Forge: the sound of a song ({@code name} is {@code "records." + recordName}); mods map it to their own domain. */
    public ResourceLocation getRecordResource(String name) {
        return new ResourceLocation(name);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        if (mcc$vanilla == null) list.add(getRecordNameLocal());
        else super.addInformation(stack, player, list, advanced);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.rare;
    }

    /** Puts a mod's disc into an empty vanilla jukebox and starts its song. */
    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (mcc$vanilla != null) return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
        if (world.getBlock(x, y, z) != legacyconnector.v1_7_10.net.minecraft.init.Blocks.jukebox || world.getBlockMetadata(x, y, z) != 0) return false;
        if (world.isRemote) return true;
        legacyconnector.v1_7_10.net.minecraft.block.BlockJukebox.mcc$insert(world, x, y, z, stack);
        String sound = getRecordResource("records." + recordName).toString();
        world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, sound, 4.0F, 1.0F);
        stack.mcc$setCount(stack.mcc$count() - 1);
        return true;
    }
}
