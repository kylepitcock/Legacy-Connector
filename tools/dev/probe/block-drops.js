var NS=ns_to_test;
var Sup=Java.extend(Java.type("java.util.function.Supplier"),{get:function(){
  var p=server.getPlayerList().getPlayers().get(0); var l=p.serverLevel();
  var BP=Java.type("net.minecraft.core.BlockPos"); var R=Java.type("net.minecraft.core.registries.BuiltInRegistries").BLOCK;
  var AABB=Java.type("net.minecraft.world.phys.AABB"); var IE=Java.type("net.minecraft.world.entity.item.ItemEntity");
  var base=p.blockPosition().offset(0,3,0); var keys=[]; R.keySet().forEach(function(k){ if(k.getNamespace()==NS) keys.push(k) });
  var nodrop=[], err=[], ok=0;
  keys.forEach(function(k){
    try{
      var b=R.get(k); var st=b.defaultBlockState(); if(st.isAir()) return;
      var pos=base; l.setBlock(pos.below(), Java.type("net.minecraft.world.level.block.Blocks").STONE.defaultBlockState(),3);
      l.setBlock(pos, st, 3);
      var bb=new AABB(pos).inflate(3);
      l.getEntitiesOfClass(IE.class, bb).forEach(function(e){ e.discard() });
      var hard=st.getDestroySpeed(l,pos);
      if(hard<0){ ok++; l.setBlock(pos, Java.type("net.minecraft.world.level.block.Blocks").AIR.defaultBlockState(),3); return; }
      l.destroyBlock(pos, true, p);
      var items=l.getEntitiesOfClass(IE.class, bb); var n=items.size();
      if(n==0) nodrop.push(""+k.getPath()); else ok++;
      items.forEach(function(e){ e.discard() });
      l.setBlock(pos, Java.type("net.minecraft.world.level.block.Blocks").AIR.defaultBlockState(),3);
    }catch(e){ err.push(k.getPath()+": "+e) }
  });
  return NS+": blocks="+keys.length+" dropped="+ok+" nodrop="+nodrop.length+" errors="+err.length+"\nNODROP: "+nodrop.slice(0,80).join(", ")+"\nERR: "+err.slice(0,5).join(" | ");
}});
""+server.submit(new Sup()).get()
