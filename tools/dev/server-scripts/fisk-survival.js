// Fisk's Superheroes progression/world survival run (LIBS=lib): probes, recipe craftability, armor stats, names, worldgen census, player NBT round trip.
var out = "";
var NS = "fiskheroes";
function probe(name) {
  var src = new (T("java.lang.String"))(T("java.nio.file.Files").readAllBytes(T("java.nio.file.Paths").get(T("java.lang.System").getProperty("legacyconnector.serverTickScript")).getParent().resolve("../../tools/dev/probe/" + name + ".js")));
  return eval("var ns_to_test='" + NS + "';" + src);
}
if (tick == 0) {
  sp = realPlayer("FiskSurvival");
  sp.setGameMode(T("net.minecraft.world.level.GameType").SURVIVAL);
  level = server.overworld();
  sp.teleportTo(level, 0.5, 100, 0.5, 0, 0);
  out += P("recipes", function () { return probe("recipes"); });
}
if (tick == 3) {
  out += P("block drops", function () { return probe("block-drops"); });
  out += P("mob drops", function () { return probe("mob-drops"); });
  out += P("loot tables", function () { return probe("loot-tables"); });
}
if (tick == 6) {
  out += P("items", function () {
    var R = T("net.minecraft.core.registries.BuiltInRegistries"); var rows = []; var AI = T("net.minecraft.world.item.ArmorItem");
    var noName = [], noIcon = 0, armor = [];
    R.ITEM.keySet().forEach(function (k) {
      if (k.getNamespace() != NS) return; var it = R.ITEM.get(k); var st = new (T("net.minecraft.world.item.ItemStack"))(it);
      var nm = String(st.getHoverName().getString());
      if (nm.indexOf(".") >= 0 || nm.indexOf("item") == 0 || nm == "") noName.push(k.getPath() + "=" + nm);
      var dur = st.getMaxDamage();
      if (it instanceof AI) armor.push(k.getPath() + " def=" + it.getDefense() + " tough=" + it.getToughness() + " dur=" + dur + " slot=" + it.getType());
      rows.push(k.getPath() + "|" + nm + (dur ? "|dur" + dur : ""));
    });
    return rows.length + " items\nUNNAMED: " + noName.join(", ") + "\nARMOR: " + armor.join("; ") + "\nALL: " + rows.join(", ");
  });
  out += P("blocks", function () {
    var R = T("net.minecraft.core.registries.BuiltInRegistries"); var rows = [];
    R.BLOCK.keySet().forEach(function (k) { if (k.getNamespace() == NS) rows.push(k.getPath() + "|" + R.BLOCK.get(k).getName().getString()); });
    return rows.join(", ");
  });
  out += P("entities", function () {
    var R = T("net.minecraft.core.registries.BuiltInRegistries"); var rows = [];
    R.ENTITY_TYPE.keySet().forEach(function (k) { if (k.getNamespace() == NS) rows.push(k.getPath() + "|" + R.ENTITY_TYPE.get(k).getDescription().getString()); });
    return rows.join(", ");
  });
  out += P("creative tabs", function () {
    var R = T("net.minecraft.core.registries.BuiltInRegistries"); var rows = [];
    R.CREATIVE_MODE_TAB.keySet().forEach(function (k) { var t = R.CREATIVE_MODE_TAB.get(k); if (k.getNamespace() != "minecraft") rows.push(k + "|" + t.getDisplayName().getString() + "|icon=" + t.getIconItem()); });
    return rows.join("\n");
  });
}
if (tick == 9) {
  out += P("craftability", function () {
    var RT = T("net.minecraft.world.item.crafting.RecipeType"); var CI = T("net.minecraft.world.item.crafting.CraftingInput");
    var res = []; var okc = 0, bad = [];
    server.getRecipeManager().getRecipes().forEach(function (h) {
      if (h.id().getNamespace() != NS) return;
      var r = h.value(); if (r.getType() != RT.CRAFTING) { res.push("nonCrafting:" + h.id().getPath() + ":" + r.getType()); return; }
      var ings = r.getIngredients(); var items = T("java.util.ArrayList");
      var l = new items(); var emptyIng = false;
      for (var i = 0; i < ings.size(); i++) { var ing = ings.get(i); var ms = ing.getItems(); if (ing.isEmpty() || ms.length == 0) { emptyIng = true; l.add(T("net.minecraft.world.item.ItemStack").EMPTY); } else l.add(ms[0].copy()); }
      if (emptyIng) { bad.push(h.id().getPath() + ": ingredient with no items"); return; }
      var w = 3, hh = 3;
      if (r.canCraftInDimensions) {}
      try {
        var inp;
        if (r instanceof T("net.minecraft.world.item.crafting.ShapedRecipe")) { w = r.getWidth(); hh = r.getHeight(); inp = CI.of(w, hh, l); }
        else inp = CI.of(l.size(), 1, l);
        if (r.matches(inp, level)) { var o = r.assemble(inp, server.registryAccess()); if (o.isEmpty()) bad.push(h.id().getPath() + ": assembles empty"); else okc++; }
        else bad.push(h.id().getPath() + ": does not match its own ingredients");
      } catch (e) { bad.push(h.id().getPath() + ": EXC " + e); }
    });
    return "craftable=" + okc + " bad=" + bad.length + "\n" + bad.slice(0, 40).join("\n") + "\n" + res.slice(0, 20).join(", ");
  });
  out += P("recipe list", function () {
    var rows = [];
    server.getRecipeManager().getRecipes().forEach(function (h) { if (h.id().getNamespace() == NS) rows.push(h.id().getPath() + "->" + h.value().getResultItem(server.registryAccess()).getItem()); });
    return rows.join(", ");
  });
}
if (tick == 12) {
  out += P("worldgen census", function () {
    var R = T("net.minecraft.core.registries.BuiltInRegistries");
    var RAD = 6, cnt = {}, chunks = 0;
    for (var cx = -RAD; cx <= RAD; cx++) for (var cz = -RAD; cz <= RAD; cz++) {
      var ch = level.getChunk(cx, cz); chunks++; var secs = ch.getSections();
      for (var si = 0; si < secs.length; si++) { var sec = secs[si]; if (sec.hasOnlyAir()) continue;
        for (var x = 0; x < 16; x++) for (var y = 0; y < 16; y++) for (var z = 0; z < 16; z++) {
          var k = String(R.BLOCK.getKey(sec.getBlockState(x, y, z).getBlock())); if (k.indexOf(NS + ":") == 0) cnt[k] = (cnt[k] || 0) + 1; } }
    }
    var ents = {}; level.getAllEntities().forEach(function (e) { var k = String(R.ENTITY_TYPE.getKey(e.getType())); if (k.indexOf(NS) == 0) ents[k] = (ents[k] || 0) + 1; });
    return chunks + " chunks; blocks " + JSON.stringify(cnt) + "; entities " + JSON.stringify(ents);
  });
  out += P("player nbt", function () {
    var tag = sp.saveWithoutId(new (T("net.minecraft.nbt.CompoundTag"))());
    var s = String(tag); var i = s.indexOf("legacy"); 
    return "len=" + s.length + " legacyKeys=" + (i >= 0 ? s.substring(i, i + 300) : "none");
  });
}
out;
