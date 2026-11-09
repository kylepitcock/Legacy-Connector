package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import legacyconnector.vb1_7_3.net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.phys.AABB;

/** Region-safe admission of exact vanilla Beta paintings into native painting tracking and persistence. */
public final class PaintingsB173 {
    private record Placed(java.util.UUID uuid,AABB box){}
    private static final Map<WorldGenRegion,List<Placed>> GENERATED=new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private PaintingsB173(){}
    /** Beta differs from later versions: 0 north, 1 west, 2 south, 3 east. */
    public static Direction direction(int facing){return switch(facing&3){case 0->Direction.NORTH;case 1->Direction.WEST;case 2->Direction.SOUTH;default->Direction.EAST;};}
    public static boolean clear(PaintingEntity original){
        var world=original.world;var access=world.mcc$access();var box=original.boundingBox.mcc$box();
        if(access==null)return world.getEntityCollisions(original,original.boundingBox).isEmpty();
        for(BlockPos pos:BlockPos.betweenClosed(net.minecraft.util.Mth.floor(box.minX),net.minecraft.util.Mth.floor(box.minY),net.minecraft.util.Mth.floor(box.minZ),net.minecraft.util.Mth.floor(box.maxX),net.minecraft.util.Mth.floor(box.maxY),net.minecraft.util.Mth.floor(box.maxZ))){
            // getBlockId validates the region read; pending states include walls queued outside its write radius.
            world.getBlockId(pos.getX(),pos.getY(),pos.getZ());var state=WorldGenWritesB173.pending(world,pos);if(state==null)state=access.getBlockState(pos);
            for(var part:state.getCollisionShape(access,pos).toAabbs())if(part.move(pos).intersects(box))return false;
        }
        if(access instanceof WorldGenRegion region){
            synchronized(GENERATED){for(var other:GENERATED.getOrDefault(region,List.of()))if((original.mcc$modern==null||!other.uuid.equals(original.mcc$modern.getUUID()))&&other.box.intersects(box))return false;}
        }else if(!world.mcc$level().getEntities(original.mcc$modern,box,entity->entity instanceof HangingEntity).isEmpty())return false;
        return true;
    }
    public static boolean spawn(PaintingEntity original){
        var world=original.world;
        if(world==null||world.mcc$level()==null||world.isRemote||original.mcc$modern!=null||!original.canStayAttached())return false;
        var facing=direction(original.facing);var front=new BlockPos(original.attachmentX,original.attachmentY,original.attachmentZ).relative(facing);
        var art=world.mcc$level().registryAccess().registryOrThrow(Registries.PAINTING_VARIANT).getHolder(ResourceKey.create(Registries.PAINTING_VARIANT,original.variant.mcc$id()));
        if(art.isEmpty())return false;
        var painting=new Painting(world.mcc$level(),front,facing,art.get());
        if(!world.mcc$spawnNative(painting))return false;
        original.mcc$modern=painting;EntityViewsB173.spawned(painting,original);
        if(world.mcc$access() instanceof WorldGenRegion region)synchronized(GENERATED){GENERATED.computeIfAbsent(region,key->new ArrayList<>()).add(new Placed(painting.getUUID(),original.boundingBox.mcc$box()));}
        return true;
    }
}
