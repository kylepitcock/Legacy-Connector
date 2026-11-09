package legacyconnector.vb1_7_3.net.minecraft.item;

/** Beta records carry the streaming sound name. Native jukeboxes own insertion, playback and ejection. */
public class MusicDiscItem extends Item {
    public final String sound;
    public MusicDiscItem(int id,String sound){super(id);this.sound=sound;maxCount=1;}
    @Override public boolean useOnBlock(ItemStack stack,legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player,
            legacyconnector.vb1_7_3.net.minecraft.world.World world,int x,int y,int z,int side){
        if(world.mcc$level()==null||!(player.mcc$modern instanceof net.minecraft.world.entity.player.Player nativePlayer))return false;
        var nativeStack=org.kylepitcock.legacyconnector.vb1_7_3.StacksB173.toModern(stack);
        var result=net.minecraft.world.item.JukeboxPlayable.tryInsertIntoJukebox(world.mcc$level(),new net.minecraft.core.BlockPos(x,y,z),nativeStack,nativePlayer);
        stack.count=nativeStack.getCount();return result.consumesAction();
    }
}
