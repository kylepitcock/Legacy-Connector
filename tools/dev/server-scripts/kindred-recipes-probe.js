// #113: the potion powder recipes (custom ingredient factory + NBT potion ingredient) must exist with a potion component ingredient.
var res = "";
if (typeof DONE === "undefined") {
  DONE = 1;
  var ING = T("net.minecraft.world.item.crafting.Ingredient");
  var JO = T("com.mojang.serialization.JsonOps");
  var n = 0;
  server.getRecipeManager().getRecipes().forEach(function (h) {
    var id = "" + h.id();
    if (id.indexOf("kindredlegacy:") == 0 && id.indexOf("powder") >= 0) {
      n++;
      if (id.indexOf("healing_powder") >= 0 || id.indexOf("fire_resistance") >= 0) {
        var ings = h.value().getIngredients();
        var s = [];
        for (var i = 0; i < ings.size(); i++) s.push("" + ING.CODEC.encodeStart(JO.INSTANCE, ings.get(i)).result().orElse(null));
        res += id + " -> " + s.join(" ; ") + "\n";
      }
    }
  });
  res += "powder recipes: " + n;
}
res;
