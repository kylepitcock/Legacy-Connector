var Sup=Java.extend(Java.type("java.util.function.Supplier"),{get:function(){
  var p=server.getPlayerList().getPlayers().get(0); var l=p.serverLevel(); var o=[];
  var W=Java.type("legacyconnector.v1_7_10.net.minecraft.world.World").mcc$wrap(l); var te=W.getTileEntity(110,83,100);
  var API=Java.type("thaumcraft.api.ThaumcraftApi"); var recs=API.getCraftingRecipes(); var CR=Java.type("thaumcraft.api.crafting.CrucibleRecipe");
  var rec=null; for(var i=0;i<recs.size();i++){ var r=recs.get(i); if(r instanceof CR && (""+r.recipeOutput).indexOf("ingot")>=0 || (r instanceof CR && rec==null)) { rec=r; if((""+r.recipeOutput).indexOf("ingot")>=0) break } }
  if(rec==null) return "no crucible recipe";
  o.push("recipe -> "+rec.recipeOutput+" catalyst="+rec.catalyst+" aspects="+rec.aspects.visSize());
  te.aspects=rec.aspects.copy? rec.aspects.copy(): rec.aspects; // enough aspects in the cauldron
  var cat=rec.catalyst; var stack=null;
  if(cat!=null && (""+cat).indexOf("tile.")<0 && typeof cat=="string"){ var OD=Java.type("legacyconnector.v1_7_10.net.minecraftforge.oredict.OreDictionary"); var lst=OD.getOres(cat); if(lst.size()>0) stack=lst.get(0).copy() } else stack=cat.copy();
  o.push("catalyst stack="+stack);
  var ie=new (Java.type("legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem"))(W,110.5,84.2,100.5,stack);
  W.spawnEntityInWorld(ie);
  return o.join("\n");
}});
""+server.submit(new Sup()).get()
