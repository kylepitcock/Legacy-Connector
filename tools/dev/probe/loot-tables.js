var NS=ns_to_test;
var Sup=Java.extend(Java.type("java.util.function.Supplier"),{get:function(){
  var p=server.getPlayerList().getPlayers().get(0); var l=p.serverLevel();
  var RK=Java.type("net.minecraft.resources.ResourceKey"), RL=Java.type("net.minecraft.resources.ResourceLocation"), Reg=Java.type("net.minecraft.core.registries.Registries");
  var LP=Java.type("net.minecraft.world.level.storage.loot.LootParams"), PS=Java.type("net.minecraft.world.level.storage.loot.parameters.LootContextParamSets"), PR=Java.type("net.minecraft.world.level.storage.loot.parameters.LootContextParams");
  var names=[]; server.getResourceManager().listResources("loot_table", function(r){ return r.getNamespace()==NS && r.getPath().endsWith(".json") }).keySet().forEach(function(k){ names.push(k.getPath().substring("loot_table/".length, k.getPath().length-5)) });
  var o=[], empty=[];
  names.forEach(function(n){
    try{
      var t=server.reloadableRegistries().getLootTable(RK.create(Reg.LOOT_TABLE, RL.fromNamespaceAndPath(NS,n)));
      var b=new LP.Builder(l).withParameter(PR.ORIGIN, p.position());
      var ps=PS.CHEST; if(n.indexOf("fishing")>=0){ ps=PS.FISHING; b.withParameter(PR.TOOL, Java.type("net.minecraft.world.item.ItemStack").EMPTY) } else if(n.indexOf("entities")>=0){ return }
      var total=0; for(var i=0;i<10;i++){ total+=t.getRandomItems(b.create(ps)).size() }
      o.push(n+":"+total); if(total==0) empty.push(n);
    }catch(e){ o.push(n+": ERR "+e) }
  });
  return NS+" non-entity tables="+names.length+" generated(10 rolls): "+o.join(", ")+"\nEMPTY: "+empty.join(", ");
}});
""+server.submit(new Sup()).get()
