if (tick == 0) {
  var HR = Java.type("Reika.RotaryCraft.Registry.HandbookRegistry");
  var vs = Java.from(HR.values());
  var bad = [], empty = [], withCraft = 0, ok = 0, total = vs.length, sample = null;
  vs.forEach(function (h) {
    try {
      var t = h.getTitle(), d = h.getData();
      if (t == null || ("" + t).length == 0) empty.push(h.name() + " no title");
      else if (d == null || ("" + d).length == 0) empty.push(h.name() + " no data (title=" + t + ")");
      else ok++;
      if (h.isCrafting()) { var c = h.getCrafting(); if (c != null && c.size() > 0) withCraft++; else empty.push(h.name() + " crafting recipe missing"); }
      if (h.isSmelting()) { if (h.getSmelting() == null) empty.push(h.name() + " smelting missing"); }
      if (sample == null && d != null && ("" + d).length > 50) sample = h.name() + ": " + ("" + d).substring(0, 160).replace(/\n/g, " ");
    } catch (e) { bad.push(h.name() + " EXC " + e); }
  });
  L.say("handbook entries=" + total + " ok=" + ok + " with crafting=" + withCraft + " problems=" + empty.length + " exceptions=" + bad.length);
  L.say("sample " + sample);
  L.say("problems: " + empty.slice(0, 25).join(" | "));
  L.say("exceptions: " + bad.slice(0, 15).join(" | "));
}
L.run(tick);
