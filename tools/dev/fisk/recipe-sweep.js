// Crafting sweep: for every crafting recipe whose result is a Fisk item (or whose id is Fisk's), lay out its ingredients in a crafting grid and check that
// the recipe matches and assembles the advertised result. Custom (code) recipes are listed with their class.
var CraftingInput = Java.type("net.minecraft.world.item.crafting.CraftingInput");
var ShapedRecipe = Java.type("net.minecraft.world.item.crafting.ShapedRecipe");
var ShapelessRecipe = Java.type("net.minecraft.world.item.crafting.ShapelessRecipe");
var RecipeType = Java.type("net.minecraft.world.item.crafting.RecipeType");
var all = server.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).iterator();
var okc = 0, bad = [], custom = [], total = 0, lv = server.overworld();
while (all.hasNext()) {
  var h = all.next(), rc = h.value(), id = String(h.id());
  var res = rc.getResultItem(server.registryAccess());
  var ns = String(BuiltInRegistries.ITEM.getKey(res.getItem()).getNamespace());
  if (id.indexOf("fisk") < 0 && ns != "fiskheroes" && ns != "fisktag") continue;
  total++;
  try {
    var ing = rc.getIngredients(), list = new (Java.type("java.util.ArrayList"))();
    var w, hh;
    if (rc instanceof ShapedRecipe) { w = rc.getWidth(); hh = rc.getHeight(); }
    else if (rc instanceof ShapelessRecipe) { w = 3; hh = 3; }
    else { custom.push(id.replace("fiskheroes:", "") + " <" + rc.getClass().getSimpleName() + ">"); continue; }
    for (var i = 0; i < ing.size(); i++) { var items = ing.get(i).getItems(); list.add(items.length ? items[0].copy() : Java.type("net.minecraft.world.item.ItemStack").EMPTY); }
    while (list.size() < w * hh) list.add(Java.type("net.minecraft.world.item.ItemStack").EMPTY);
    var input = CraftingInput.of(w, hh, list);
    var m = rc.matches(input, lv);
    var out2 = m ? rc.assemble(input, server.registryAccess()) : null;
    if (m && out2 != null && !out2.isEmpty()) okc++; else bad.push(id.replace("fiskheroes:", "") + " matches=" + m + " out=" + out2 + " ingredients=" + ing);
  } catch (e) { bad.push(id + " EXC " + String(e).substring(0, 120)); }
}
"recipes " + total + ": ok " + okc + ", failing " + bad.length + ", code recipes " + custom.length + "\nFAILING:\n" + bad.slice(0, 40).join("\n").substring(0, 4000) + "\nCUSTOM: " + custom.join(", ")
