package legacyconnector.vb1_7_3.net.minecraft.item;

import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.src.forge.MinecraftForge;
import legacyconnector.vb1_7_3.net.minecraft.util.hit.HitResultType;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta's ItemBucket, including Forge 1.0's custom pickup callback. */
public class BucketItem extends Item {
    private int fluidBlockId;

    public BucketItem(int id,int fluidBlockId){
        super(id);maxCount=1;this.fluidBlockId=fluidBlockId;
    }

    @Override public ItemStack use(ItemStack stack,World world,PlayerEntity player){
        double yaw=Math.toRadians(player.yaw),pitch=Math.toRadians(player.pitch);
        Vec3d from=Vec3d.create(player.x,player.y+1.62-player.standingEyeHeight,player.z);
        Vec3d to=Vec3d.create(from.x-Math.sin(yaw)*Math.cos(pitch)*5,from.y-Math.sin(pitch)*5,from.z+Math.cos(yaw)*Math.cos(pitch)*5);
        var hit=world.raycast(from,to,fluidBlockId==0);
        if(hit==null||hit.type!=HitResultType.BLOCK)return stack;
        int x=hit.blockX,y=hit.blockY,z=hit.blockZ;
        if(!world.canInteract(player,x,y,z))return stack;
        if(fluidBlockId==0){
            ItemStack custom=MinecraftForge.fillCustomBucket(world,x,y,z);
            if(custom!=null)return custom;
            Material material=world.getMaterial(x,y,z);
            if(world.getBlockMeta(x,y,z)==0 && (material==Material.WATER||material==Material.LAVA)
                    && world.setBlock(x,y,z,0))return new ItemStack(material==Material.WATER?326:327,1,0);
            return stack;
        }
        if(fluidBlockId<0)return new ItemStack(325,1,0);
        switch(hit.side){case 0->y--;case 1->y++;case 2->z--;case 3->z++;case 4->x--;case 5->x++;default->{return stack;}}
        if(!world.canInteract(player,x,y,z)||!world.isAir(x,y,z)&&world.getMaterial(x,y,z).isSolid())return stack;
        if(world.dimension.evaporatesWater && fluidBlockId==8){
            world.playSound(x+.5,y+.5,z+.5,"random.fizz",.5F,2.6F);
            for(int i=0;i<8;i++)world.addParticle("largesmoke",x+world.random.nextDouble(),y+world.random.nextDouble(),z+world.random.nextDouble(),0,0,0);
        }else if(!world.setBlock(x,y,z,fluidBlockId))return stack;
        // Beta had no creative mode; preserve the host's infinite-material placement behavior.
        if(player.mcc$modern instanceof net.minecraft.world.entity.player.Player modern && modern.hasInfiniteMaterials())return stack;
        return new ItemStack(325,1,0);
    }
}
