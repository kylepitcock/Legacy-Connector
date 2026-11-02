// Kills 15 of each named entity of a namespace and prints the loot table and the items dropped. Prepend: var ns_to_test="galacticraftcore"; var mobs_to_test=["evolved_creeper"];
var Sup=Java.extend(Java.type("java.util.function.Supplier"),{get:function(){
  var p=server.getPlayerList().getPlayers().get(0); var l=p.serverLevel();
  var ET=Java.type("net.minecraft.core.registries.BuiltInRegistries").ENTITY_TYPE;
  var RL=Java.type("net.minecraft.resources.ResourceLocation");
  var AABB=Java.type("net.minecraft.world.phys.AABB"); var IE=Java.type("net.minecraft.world.entity.item.ItemEntity");
  var out="";
  mobs_to_test.forEach(function(n){
    var t=ET.get(RL.parse(ns_to_test+":"+n)); var pos=p.blockPosition().offset(8,0,0); var tot=[];
    for(var i=0;i<15;i++){
      var e=t.create(l); e.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,0,0); e.setNoAi(true); l.addFreshEntity(e);
      if(i==0) out+=n+" "+e.getClass().getName()+" table="+e.getLootTable()+"\n";
      e.hurt(l.damageSources().playerAttack(p),1000);
      l.getEntitiesOfClass(IE.class,new AABB(pos).inflate(3)).forEach(function(d){tot.push(""+d.getItem()); d.discard()});
    }
    out+=tot.join(",")+"\n";
  });
  return out;
}});
""+server.submit(new Sup()).get()
