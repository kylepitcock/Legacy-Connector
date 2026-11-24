// Dev probe for #325: finds the nearest region of each wanted biome (cheap biome-source sampling), generates a square of chunks there and counts
// the marker blocks of Witches and More's structures (its spawner blocks, chests) per region. Output lines start with WAMREG.
if (typeof st === "undefined") st = { n: 0, step: 0 };
st.n++;
var WANT = ["desert", "plains", "forest", "taiga", "windswept_hills", "swamp", "savanna", "snowy_plains", "jungle", "badlands"];
var R = 8;
if (st.n == 2) {
  var lvl = server.overworld();
  var src = lvl.getChunkSource().getGenerator().getBiomeSource();
  var sampler = lvl.getChunkSource().randomState().sampler();
  var found = {};
  for (var r = 0; r <= 4000; r += 96) {
    for (var a = 0; a < (r == 0 ? 1 : Math.max(8, Math.floor(r / 48))); a++) {
      var ang = a * 2 * Math.PI / (r == 0 ? 1 : Math.max(8, Math.floor(r / 48)));
      var x = Math.floor(r * Math.cos(ang)), z = Math.floor(r * Math.sin(ang));
      var b = src.getNoiseBiome(x >> 2, 16, z >> 2, sampler).unwrapKey().get().location().path;
      if (WANT.indexOf(b) >= 0 && !found[b]) found[b] = [x >> 4, z >> 4];
    }
  }
  st.found = found; st.names = Object.keys(found);
  log.info("WAMREG found " + JSON.stringify(found));
}
if (st.n > 2 && st.names && st.step < st.names.length) {
  var lvl2 = server.overworld(), reg = Java.type("net.minecraft.core.registries.BuiltInRegistries").BLOCK;
  var name = st.names[st.step++], c = st.found[name], counts = {}, t0 = Date.now();
  for (var cx = c[0] - R; cx <= c[0] + R; cx++) for (var cz = c[1] - R; cz <= c[1] + R; cz++) {
    var secs = lvl2.getChunk(cx, cz).getSections();
    for (var s = 0; s < secs.length; s++) {
      var sec = secs[s]; if (sec.hasOnlyAir()) continue;
      var stt = sec.getStates();
      for (var x = 0; x < 16; x++) for (var y = 0; y < 16; y++) for (var z = 0; z < 16; z++) {
        var bs = stt.get(x, y, z); if (bs.isAir()) continue;
        var key = reg.getKey(bs.getBlock()), p = key.getPath(), ns = key.getNamespace();
        if ((ns == "witchesandmore" && p.indexOf("spawner") >= 0) || (ns == "minecraft" && p == "chest")) counts[key.toString()] = (counts[key.toString()] || 0) + 1;
      }
    }
  }
  log.info("WAMREG " + name + " at chunk " + c + " (" + (2 * R + 1) * (2 * R + 1) + " chunks, " + (Date.now() - t0) + " ms): " + JSON.stringify(counts));
}
