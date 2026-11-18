// Ore census and vein connectivity over an 11x11 chunk area of the overworld (CustomOreGen, #82 and #295). Generates the chunks ring by ring,
// collects every ore block (block id with an "ore" token, deepslate variants merged), and at the end writes a JSON file (path: the literal
// below, substituted by tools/dev/ore-probe.sh) with
//   counts          ore blocks per type
//   bondX/Y/Z       per type: fraction of its ore blocks that have a block of the same type next to them along that axis (a vein cut into
//                   flat pieces shows as a low bondY, a checkerboard as a low bondZ/bondX)
//   down            per y: [blocks at y, blocks at y whose block below (y-1) is the same type]  (a pass boundary shows as a dip at y=0)
//   slice           ore blocks of the slab x in 0..7 as [z, y, typeIndex] (for tools/dev/render_ore_slice.py)
// and prints a one-line summary ("OREPROBE ...").
var OUT = "@OUT@";
var R = 5;
var T_ = function (n) { return Java.type(n); };
var level = server.overworld();
var BuiltInRegistries = T_("net.minecraft.core.registries.BuiltInRegistries");
var HashMap = T_("java.util.HashMap");
var P = T_("net.minecraft.core.BlockPos");
var YMIN = -64, YMAX = 320;
if (typeof st === "undefined") {
  st = { ring: 0, map: new HashMap(), names: [], idx: {}, byBlock: new HashMap(), done: false };
}
function typeOf(state) {
  var b = state.getBlock();
  var c = st.byBlock.get(b);
  if (c !== null && c !== undefined) return c;
  var key = BuiltInRegistries.BLOCK.getKey(b);
  var path = String(key.getPath()).replace(/^deepslate_/, "");
  var isOre = /(^|_)ore(_|$)/.test(path) && !/^(nether|end)_/.test(path);
  var t = -1;
  if (isOre) {
    var name = key.getNamespace() + ":" + path;
    if (st.idx[name] === undefined) { st.idx[name] = st.names.length; st.names.push(name); }
    t = st.idx[name];
  }
  st.byBlock.put(b, t);
  return t;
}
function pack(x, y, z) { return ((x + 4096) * 8192 + (z + 4096)) * 1024 + (y + 128); }
function scanChunk(cx, cz) {
  var chunk = level.getChunk(cx, cz);
  var secs = chunk.getSections();
  for (var s = 0; s < secs.length; s++) {
    var sec = secs[s];
    if (sec.hasOnlyAir()) continue;
    var anyOre = sec.maybeHas(function (bs) { return typeOf(bs) >= 0; });
    if (!anyOre) continue;
    var y0 = chunk.getMinBuildHeight() + s * 16;
    for (var y = 0; y < 16; y++) for (var z = 0; z < 16; z++) for (var x = 0; x < 16; x++) {
      var t = typeOf(sec.getBlockState(x, y, z));
      if (t >= 0) st.map.put(pack(cx * 16 + x, y0 + y, cz * 16 + z), t);
    }
  }
}
function analyse() {
  var n = st.names.length;
  var counts = [], bx = [], by = [], bz = [];
  for (var i = 0; i < n; i++) { counts.push(0); bx.push(0); by.push(0); bz.push(0); }
  var down = {};
  var slice = [];
  var it = st.map.entrySet().iterator();
  while (it.hasNext()) {
    var e = it.next();
    var key = Number(e.getKey()), t = Number(e.getValue());
    var y = key % 1024 - 128; var r = Math.floor(key / 1024);
    var z = r % 8192 - 4096; var x = Math.floor(r / 8192) - 4096;
    counts[t]++;
    var has = function (X, Y, Z) { var v = st.map.get(pack(X, Y, Z)); return v !== null && v !== undefined && Number(v) === t; };
    if (has(x + 1, y, z) || has(x - 1, y, z)) bx[t]++;
    if (has(x, y + 1, z) || has(x, y - 1, z)) by[t]++;
    if (has(x, y, z + 1) || has(x, y, z - 1)) bz[t]++;
    var d = down[y]; if (!d) d = down[y] = [0, 0];
    d[0]++; if (has(x, y - 1, z)) d[1]++;
    if (x >= 0 && x < 8) slice.push([z, y, t]);
  }
  var out = { radius: R, names: st.names, counts: counts, bondX: [], bondY: [], bondZ: [], down: down, slice: slice, total: 0 };
  for (var i = 0; i < n; i++) {
    out.total += counts[i];
    out.bondX.push(counts[i] ? bx[i] / counts[i] : 0);
    out.bondY.push(counts[i] ? by[i] / counts[i] : 0);
    out.bondZ.push(counts[i] ? bz[i] / counts[i] : 0);
  }
  // the vertical bond rate across y=0 against the average of the layers around it (-8..-1 and 1..8)
  function rate(y) { var d = down[y]; return d && d[0] ? d[1] / d[0] : null; }
  var around = [], k;
  for (k = -8; k <= 8; k++) if (k !== 0 && k !== 1) { var rr = rate(k); if (rr !== null) around.push(rr); }
  var mean = around.length ? around.reduce(function (a, b) { return a + b; }, 0) / around.length : 0;
  out.bondDownAt0 = rate(0); out.bondDownAt1 = rate(1); out.bondDownAround = mean;
  T_("java.nio.file.Files").writeString(T_("java.nio.file.Path").of(OUT), JSON.stringify(out));
  var top = [];
  for (var i = 0; i < n; i++) top.push(st.names[i] + "=" + counts[i]);
  return "OREPROBE total=" + out.total + " bondDown(y=0)=" + out.bondDownAt0 + " bondDown(y=1)=" + out.bondDownAt1 + " bondDown(around)=" + mean + " " + top.join(" ");
}
var __r = undefined;
if (!st.done) {
  if (st.ring <= R) {
    for (var cx = -st.ring; cx <= st.ring; cx++) for (var cz = -st.ring; cz <= st.ring; cz++) {
      if (Math.max(Math.abs(cx), Math.abs(cz)) !== st.ring) continue;
      scanChunk(cx, cz);
    }
    st.ring++;
  } else {
    st.done = true;
    __r = analyse();
  }
}
__r;
