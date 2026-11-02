var out = "";
var NS = "fiskheroes";
if (tick == 0) {
  server.getRecipeManager().getRecipes().forEach(function (h) {
    if (h.id().getNamespace() != NS) return; var p = String(h.id().getPath());
    if (["shaped/gun_base","shaped/suit_upgrade","shaped/display_stand_5","shaped/quiver","shaped/tutridium_pickaxe"].indexOf(p) < 0) return;
    var r = h.value(); var ings = r.getIngredients(); var s = p + ": ";
    for (var i = 0; i < ings.size(); i++) { var ing = ings.get(i); var j = T("net.minecraft.world.item.crafting.Ingredient").CODEC.encodeStart(T("com.mojang.serialization.JsonOps").INSTANCE, ing); s += (ing.isEmpty() ? "EMPTY " : "") + String(j.result().isPresent() ? j.result().get() : j.error()) + " ; "; }
    out += s + "\n";
  });
}
out;
