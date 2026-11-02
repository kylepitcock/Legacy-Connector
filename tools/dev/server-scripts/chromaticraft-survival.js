// ChromatiCraft survival run on a naturally generated overworld (run with LIBS=lib): the probes of tools/dev/probe, a worldgen census around spawn,
// biomes and where the rainbow forest is. Output: SERVERSCRIPT lines.
var out = "";
function probe(name) {
  var src = new (T("java.lang.String"))(T("java.nio.file.Files").readAllBytes(T("java.nio.file.Paths").get(T("java.lang.System").getProperty("legacyconnector.serverTickScript")).getParent().resolve("../../tools/dev/probe/" + name + ".js")));
  return eval("var ns_to_test='chromaticraft';" + src);
}
if (tick == 0) {
  sp = realPlayer("CCSurvival");
  sp.setGameMode(T("net.minecraft.world.level.GameType").SURVIVAL);
  p = legacy(sp);
  level = server.overworld();
  sp.teleportTo(level, 0.5, 100, 0.5, 0, 0);
  out += P("recipes", function () { return probe("recipes"); });
  out += P("recipe results", function () {
    var res = {}; var n = 0;
    server.getRecipeManager().getRecipes().forEach(function (h) {
      if (h.id().getNamespace() != "chromaticraft") return; n++;
      var r = h.value().getResultItem(server.registryAccess()); res[String(r.getItem())] = (res[String(r.getItem())] || 0) + 1;
    });
    return n + " recipes -> " + Object.keys(res).length + " distinct results: " + Object.keys(res).slice(0, 60).join(", ");
  });
}
if (tick == 3) {
  out += P("block drops", function () { return probe("block-drops"); });
  out += P("mob drops", function () { return probe("mob-drops"); });
  out += P("loot tables", function () { return probe("loot-tables"); });
}
if (tick == 6) {
  out += P("worldgen census", function () {
    var R = T("net.minecraft.core.registries.BuiltInRegistries");
    var RAD = 5, cnt = {}, biomes = {}, t0 = Date.now(), chunks = 0;
    for (var cx = -RAD; cx <= RAD; cx++) for (var cz = -RAD; cz <= RAD; cz++) {
      var ch = level.getChunk(cx, cz); chunks++;
      var secs = ch.getSections();
      for (var si = 0; si < secs.length; si++) {
        var sec = secs[si]; if (sec.hasOnlyAir()) continue;
        for (var x = 0; x < 16; x++) for (var y = 0; y < 16; y++) for (var z = 0; z < 16; z++) {
          var b = sec.getBlockState(x, y, z).getBlock(); var k = String(R.BLOCK.getKey(b));
          if (k.indexOf("chromaticraft:") == 0) cnt[k] = (cnt[k] || 0) + 1;
        }
      }
      var bk = String(level.getBiome(bp(cx * 16 + 8, 70, cz * 16 + 8)).unwrapKey().get().location()); biomes[bk] = (biomes[bk] || 0) + 1;
    }
    return chunks + " chunks in " + (Date.now() - t0) + " ms\nchromaticraft blocks: " + JSON.stringify(cnt) + "\nbiomes: " + JSON.stringify(biomes);
  });
}
if (tick == 8) {
  out += P("biomes", function () {
    var BI = T("net.minecraft.core.registries.Registries");
    var keys = []; level.registryAccess().registryOrThrow(BI.BIOME).keySet().forEach(function (k) { if (String(k).indexOf("rainbow") >= 0 || String(k).indexOf("chroma") >= 0 || String(k).indexOf("legacy") >= 0) keys.push(String(k)); });
    return "mod biomes: " + keys.join(",");
  });
}
if (tick == 10) {
  out += P("recipe match", function () {
    var CI = T("net.minecraft.world.item.crafting.CraftingInput");
    var IS = T("net.minecraft.world.item.ItemStack");
    var ok = 0, bad = [], noshape = 0;
    server.getRecipeManager().getRecipes().forEach(function (h) {
      if (h.id().getNamespace() != "chromaticraft") return;
      var r = h.value();
      try {
        var ings = r.getIngredients(); var items = [];
        var w = 3, hgt = 3;
        if (r.getWidth) { w = r.getWidth(); hgt = r.getHeight(); } else { w = Math.min(3, ings.size()); hgt = Math.ceil(ings.size() / 3); }
        ings.forEach(function (ing) { var st = ing.getItems(); items.push(st.length > 0 ? st[0].copy() : IS.EMPTY); });
        var inp = CI.of(w, hgt, T("java.util.Arrays").asList(items));
        if (r.matches(inp, level)) ok++; else bad.push(String(h.id().getPath()));
      } catch (e) { bad.push(h.id().getPath() + "!" + e); }
    });
    return "recipes matching their own ingredients: " + ok + ", not: " + bad.length + " " + bad.slice(0, 20).join(", ");
  });
}
if (tick == 12) {
  out += P("lexicon use", function () {
    var R = T("net.minecraft.core.registries.BuiltInRegistries");
    var Hand = T("net.minecraft.world.InteractionHand");
    var it = R.ITEM.get(T("net.minecraft.resources.ResourceLocation").parse("chromaticraft:chromaticraft_item_help"));
    var st = new (T("net.minecraft.world.item.ItemStack"))(it, 1);
    sp.setItemInHand(Hand.MAIN_HAND, st);
    var before = String(sp.containerMenu.getClass().getSimpleName());
    var res = st.use(level, sp, Hand.MAIN_HAND);
    return "lexicon use -> " + res.getResult() + " menu before " + before + " after " + sp.containerMenu.getClass().getSimpleName();
  });
  out += P("locate biomes", function () {
    var BI = T("net.minecraft.core.registries.Registries"); var s = "";
    ["chromaticraft:rainbow_forest", "chromaticraft:crystal_plains", "chromaticraft:glowing_forest"].forEach(function (k) {
      var r = level.findClosestBiome3d(function (h) { return String(h.unwrapKey().get().location()) == k; }, bp(0, 70, 0), 6400, 32, 64);
      s += k + " -> " + (r ? r.getFirst() : "none") + "; ";
    });
    return s;
  });
}
out;
