var NS=ns_to_test;
var Sup=Java.extend(Java.type("java.util.function.Supplier"),{get:function(){
  var p=server.getPlayerList().getPlayers().get(0); var l=p.serverLevel();
  var ET=Java.type("net.minecraft.core.registries.BuiltInRegistries").ENTITY_TYPE; var LE=Java.type("net.minecraft.world.entity.LivingEntity"); var MOB=Java.type("net.minecraft.world.entity.Mob");
  var AABB=Java.type("net.minecraft.world.phys.AABB"); var IE=Java.type("net.minecraft.world.entity.item.ItemEntity");
  var DS=l.damageSources(); var keys=[]; ET.keySet().forEach(function(k){ if(k.getNamespace()==NS) keys.push(k) });
  var pos=p.blockPosition().offset(8,0,0); var out=[];
  keys.forEach(function(k){
    try{
      var t=ET.get(k); var e=t.create(l); if(e==null||!(e instanceof LE)) return;
      e.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,0,0); if(e instanceof MOB) e.setNoAi(true); l.addFreshEntity(e);
      var tot=0; for(var tries=0;tries<6;tries++){ var wasAlive=e.isAlive(); if(tries>0) { var e2=t.create(l); e2.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,0,0); l.addFreshEntity(e2); e=e2 } e.hurt(DS.playerAttack(p),1000);
        var its=l.getEntitiesOfClass(IE.class,new AABB(pos).inflate(3)); var names=[]; its.forEach(function(d){ names.push(""+d.getItem().getItem()); d.discard() }); tot+=names.length; if(tries==0) var first=names.join("+") }
      out.push([k.getPath(),tot,first||""]);
    }catch(ex){ out.push([k.getPath(),-1,""+ex]) }
  });
  return out.join("\n");
}});
""+server.submit(new Sup()).get()
