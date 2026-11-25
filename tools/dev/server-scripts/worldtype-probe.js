// World type probe (server tick script; tools/dev/worldtype-test.sh prepends `var AREAS = [[cx0, cz0, cx1, cz1], ...]; var LABEL = "..."`).
// On the first run: lists the world_preset registry and the minecraft:normal tag, describes the overworld's generator, finds the spawn
// point (solid ground below, air above, not in the void), and samples every chunk of AREAS (step 4 columns): surface heights, top blocks,
// biomes, lowest solid y. Prints one `WORLDTYPE ...` block (a signature hash per area lets two runs be compared) and writes it to
// build/devtools/worldtype-<LABEL>.txt. Later runs do nothing.
if (tick == 0) {
  var Registries = Java.type("net.minecraft.core.registries.Registries");
  var BuiltIn = Java.type("net.minecraft.core.registries.BuiltInRegistries");
  var TagKey = Java.type("net.minecraft.tags.TagKey");
  var RL = Java.type("net.minecraft.resources.ResourceLocation");
  var BlockPos = Java.type("net.minecraft.core.BlockPos");
  var Heightmap = Java.type("net.minecraft.world.level.levelgen.Heightmap");
  var lines = [];
  function P(s) { lines.push(s); }
  var level = server.overworld();
  var access = server.registryAccess();
  var presets = access.registryOrThrow(Registries.WORLD_PRESET);
  var ids = [];
  presets.keySet().forEach(function (k) { ids.push(String(k)); });
  P("LABEL " + LABEL);
  P("world_presets " + ids.sort().join(","));
  var tag = presets.getTag(TagKey.create(Registries.WORLD_PRESET, RL.parse("minecraft:normal")));
  var tagged = [];
  if (tag.isPresent()) tag.get().forEach(function (h) { tagged.push(String(h.unwrapKey().get().location())); });
  P("normal_tag " + tagged.sort().join(","));
  var gen = level.getChunkSource().getGenerator();
  P("generator " + gen.getClass().getName());
  P("biome_source " + gen.getBiomeSource().getClass().getName());
  try {
    var gs = gen.generatorSettings();
    P("noise_settings " + access.registryOrThrow(Registries.NOISE_SETTINGS).getKey(gs.value()));
  } catch (e) { P("noise_settings ? " + e); }
  P("seed " + level.getSeed() + " minY " + level.getMinBuildHeight() + " maxY " + level.getMaxBuildHeight());
  var sp = level.getSharedSpawnPos();
  P("spawn " + sp.getX() + "," + sp.getY() + "," + sp.getZ());
  var below = level.getBlockState(sp.below()), at = level.getBlockState(sp), above = level.getBlockState(sp.above());
  var solidBelow = !below.isAir() && !below.getFluidState().isSource();
  P("spawn_blocks below=" + BuiltIn.BLOCK.getKey(below.getBlock()) + " at=" + BuiltIn.BLOCK.getKey(at.getBlock()) + " above=" + BuiltIn.BLOCK.getKey(above.getBlock()));
  var surfaceAtSpawn = level.getMinBuildHeight() - 1;
  for (var ssy = level.getMaxBuildHeight() - 1; ssy >= level.getMinBuildHeight(); ssy--) {
    var sb = level.getBlockState(new BlockPos(sp.getX(), ssy, sp.getZ()));
    // leaves and logs above the spawn do not count: the first block with collision or fluid below the spawn column's open air
    if (!sb.isAir() && ssy <= sp.getY()) { surfaceAtSpawn = ssy + 1; break; }
  }
  P("spawn_ok " + (solidBelow && at.getCollisionShape(level, sp).isEmpty() && sp.getY() > 0 && Math.abs(sp.getY() - surfaceAtSpawn) <= 3) + " (surface at spawn " + surfaceAtSpawn + ")");
  for (var a = 0; a < AREAS.length; a++) {
    var ar = AREAS[a];
    var n = 0, sum = 0, mn = 9999, mx = -9999, voidCols = 0, water = 0, hash = 17, lowest = 9999, sub0 = 0;
    var tops = {}, biomes = {};
    for (var cx = ar[0]; cx <= ar[2]; cx++) for (var cz = ar[1]; cz <= ar[3]; cz++) {
      var chunk = level.getChunk(cx, cz);
      for (var x = 0; x < 16; x += 4) for (var z = 0; z < 16; z += 4) {
        var wx = (cx << 4) + x, wz = (cz << 4) + z;
        // the real surface: the highest non-air block (the stored heightmaps of a hosted legacy generator can be stale)
        var h = level.getMinBuildHeight() - 1;
        for (var sy = level.getMaxBuildHeight() - 1; sy >= level.getMinBuildHeight(); sy--) {
          if (!chunk.getBlockState(new BlockPos(wx, sy, wz)).isAir()) { h = sy + 1; break; }
        }
        n++; sum += h; if (h < mn) mn = h; if (h > mx) mx = h;
        if (h <= level.getMinBuildHeight() + 1) {
          voidCols++;
          if (voidCols == 1) { // one empty column in detail: its non-air runs over the whole height
            var runs = "", last = null, from = 0;
            for (var yy = level.getMinBuildHeight(); yy <= level.getMaxBuildHeight(); yy++) {
              var nm = yy == level.getMaxBuildHeight() ? "end" : String(BuiltIn.BLOCK.getKey(chunk.getBlockState(new BlockPos(wx, yy, wz)).getBlock()).getPath());
              if (nm != last) { if (last != null && last != "air") runs += last + " " + from + "-" + (yy - 1) + ", "; last = nm; from = yy; }
            }
            P("  first empty column " + wx + "," + wz + " (surface " + h + "): " + (runs || "all air"));
          }
        }
        var top = chunk.getBlockState(new BlockPos(wx, Math.max(level.getMinBuildHeight(), h - 1), wz));
        var tn = String(BuiltIn.BLOCK.getKey(top.getBlock()));
        tops[tn] = (tops[tn] || 0) + 1;
        if (tn == "minecraft:water") water++;
        var b = String(level.getBiome(new BlockPos(wx, h, wz)).unwrapKey().get().location());
        biomes[b] = (biomes[b] || 0) + 1;
        hash = (hash * 31 + h) | 0;
        // lowest non-air y of the column
        for (var y = level.getMinBuildHeight(); y < Math.min(h, 320); y++) {
          if (!chunk.getBlockState(new BlockPos(wx, y, wz)).isAir()) { if (y < lowest) lowest = y; if (y < 0) sub0++; break; }
        }
      }
    }
    function top5(o) { var k = Object.keys(o).sort(function (p, q) { return o[q] - o[p]; }).slice(0, 6); return k.map(function (s) { return s + "=" + o[s]; }).join(" "); }
    P("area " + JSON.stringify(ar) + ": columns " + n + " height min/mean/max " + mn + "/" + (sum / n).toFixed(1) + "/" + mx + " voidColumns " + voidCols + " waterTop " + water
      + " lowestSolidY " + lowest + " columnsWithSolidBelowY0 " + sub0 + " hash " + hash);
    P("  tops " + top5(tops));
    P("  biomes " + top5(biomes) + " (" + Object.keys(biomes).length + " distinct)");
  }
  var text = lines.join("\n");
  java.nio.file.Files.createDirectories(java.nio.file.Paths.get(OUTDIR));
  java.nio.file.Files.write(java.nio.file.Paths.get(OUTDIR + "/worldtype-" + LABEL + ".txt"), text.getBytes("UTF-8"));
  log.info("WORLDTYPE\n" + text);
}
