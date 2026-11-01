var NS=ns_to_test;
var n=0, empty=[], err=0;
server.getRecipeManager().getRecipes().forEach(function(h){
  if(h.id().getNamespace()!=NS) return; n++;
  try{ if(h.value().getResultItem(server.registryAccess()).isEmpty()) empty.push(""+h.id().getPath()) }catch(e){ err++ }
});
NS+" recipes="+n+" emptyResult="+empty.length+" errors="+err+(empty.length?" e.g. "+empty.slice(0,5).join(", "):"")
