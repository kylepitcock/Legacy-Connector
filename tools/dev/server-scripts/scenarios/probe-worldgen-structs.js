// Dev probe (#325): generates chunks of a dimension and counts "building" blocks (not terrain) per block name, so a legacy mod's
// structures (villages, pyramids, mineshafts) show up. Also sends a real player through legacy travelToDimension(44).
// Run: ./gradlew runServerSubTest -PsubTestDir=<dir> -PserverTickScript=<this file> -PserverTickScriptFrom=100 -PautostopTicks=1500
function T(n) { return Java.type(n); }
if (typeof PROBED === "undefined" && tick >= 150) {
  PROBED = true;
  var RK = T("net.minecraft.resources.ResourceKey"), RL = T("net.minecraft.resources.ResourceLocation"), Regs = T("net.minecraft.core.registries.Registries");
  var CS = T("net.minecraft.world.level.chunk.status.ChunkStatus"), BP = T("net.minecraft.core.BlockPos");
  var BI = T("net.minecraft.core.registries.BuiltInRegistries");
  var dims = ["legacyconnector:dim_45", "minecraft:the_end"];
  var re = /door|bookshelf|window|stairs|brick|plank|wamwood|fence|torch|rail|spawner|chest|thatch|shingles|glass|sandstone|ladder|table|bed/;
  dims.forEach(function (d) {
    var level = server.getLevel(RK.create(Regs.DIMENSION, RL.parse(d)));
    if (!level) { log.info("SERVERSCRIPT: dim " + d + " missing"); return; }
    var R = 10, counts = {}, t0 = Date.now();
    for (var cx = -R; cx <= R; cx++) for (var cz = -R; cz <= R; cz++) {
      var ch = level.getChunk(cx + 40, cz + 40, CS.FULL, true);
      for (var x = 0; x < 16; x++) for (var z = 0; z < 16; z++) for (var y = 30; y < 140; y++) {
        var bs = ch.getBlockState(new BP((cx + 40) * 16 + x, y, (cz + 40) * 16 + z));
        if (bs.isAir()) continue;
        var key = BI.BLOCK.getKey(bs.getBlock()), p = key.getPath();
        if (!re.test(p)) continue;
        var nm = key.getNamespace() + ":" + p; counts[nm] = (counts[nm] || 0) + 1;
      }
    }
    var s = "";
    for (var k in counts) s += k + "=" + counts[k] + " ";
    log.info("SERVERSCRIPT: " + d + " (" + (Date.now() - t0) + " ms) " + s);
  });
}
