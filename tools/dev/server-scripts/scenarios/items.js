var Y0 = 150, X0 = 1300, Z0 = 1300;
var IR = Java.type("Reika.RotaryCraft.Registry.ItemRegistry");
if (tick == 0) {
  L.platform(X0, Y0, Z0, 20, 20, 8);
  var vs = Java.from(IR.values());
  var res = [];
  vs.forEach(function (it, i) {
    var name = it.name();
    try {
      var st = it.getStackOf();
      if (st == null) { res.push(name + ": null stack"); return; }
      var n = st.getItem() ? st.getItem().getClass().getSimpleName() : "noitem";
      var lines = [];
      // use on a stone block (top face) and a plain right click
      var p = L.fp(), lv = L.lvl();
      p.teleportTo(lv, X0 + 5.5, Y0, Z0 + 5.5, 0, 30);
      var m = st.mcc$modern();
      p.setItemInHand(L.Hand.MAIN_HAND, m);
      var r1 = "-", r2 = "-";
      try { r1 = "" + m.getItem().useOn(new L.UOC(lv, p, L.Hand.MAIN_HAND, m, new L.BHR(new L.Vec3(X0 + 6.5, Y0 - 0.5, Z0 + 6.5), L.Dir.UP, new L.BP(X0 + 6, Y0 - 1, Z0 + 6), false))); } catch (e) { r1 = "EXC " + e; }
      var m2 = st.mcc$modern(); p.setItemInHand(L.Hand.MAIN_HAND, m2);
      try { r2 = "" + m2.getItem().use(lv, p, L.Hand.MAIN_HAND).getResult(); } catch (e) { r2 = "EXC " + e; }
      res.push(name + " [" + n + "] dmg=" + st.getItemDamage() + " use-on=" + r1 + " use=" + r2);
    } catch (e) { res.push(name + ": EXC " + e); }
  });
  L.say(res.join("\n"));
}
L.run(tick);
