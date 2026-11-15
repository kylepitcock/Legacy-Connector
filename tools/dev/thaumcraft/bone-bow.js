// Issue #152, Bow of Bone: counts the arrows one draw makes. Submit lib.js first, then this file; read the result with
//   tools/dev/server-script.sh 'BOW.report()'
// Scenario A: draw and release after 8 ticks (a manual shot). Scenario B: hold for 60 ticks (the bow lets go by itself after 18 use ticks; the fake
// player's use counter runs at half the server tick rate). Arrows are counted in the inventory: the legacy arrow is hosted as legacy_entity.
// Result 2026-11-15: one arrow per draw in both scenarios on the server.
var BOW = (function () {
  var Items = Java.type("net.minecraft.world.item.Items");
  var MStack = Java.type("net.minecraft.world.item.ItemStack");
  var Hand = Java.type("net.minecraft.world.InteractionHand");
  var Reg = Java.type("net.minecraft.core.registries.BuiltInRegistries");
  var p = FAKE != null ? FAKE.p : join("bowtest");
  var bow = null;
  Reg.ITEM.keySet().forEach(function (k) { if (k.getNamespace().toLowerCase().indexOf("thaumcraft") >= 0 && k.getPath().toLowerCase().indexOf("bowbone") >= 0) bow = Reg.ITEM.get(k); });
  var lines = ["bow item: " + bow];
  function arm() {
    p.getInventory().clearContent();
    p.setItemInHand(Hand.MAIN_HAND, new MStack(bow));
    p.getInventory().add(new MStack(Items.ARROW, 64));
  }
  function left() { return p.getInventory().countItem(Items.ARROW); }
  var plan = [], at = 0, state = null;
  function scenario(name, releaseAfter, total) {
    plan.push(function () { if (p.isUsingItem()) p.stopUsingItem(); arm(); var r = p.gameMode.useItem(p, lvl, p.getMainHandItem(), Hand.MAIN_HAND); state = { name: name, start: at, use: "" + r, using0: p.isUsingItem(), log: [] }; });
    for (var i = 1; i <= total; i++) (function (i) {
      plan.push(function () {
        if (i == releaseAfter) p.releaseUsingItem();
        var n = 64 - left();
        if (state.log.length == 0 || state.log[state.log.length - 1].n != n) state.log.push({ t: i, n: n, using: p.isUsingItem() });
        if (i == total) lines.push(state.name + ": use=" + state.use + " usingAfterUse=" + state.using0 + " arrows used=" + n + " still using=" + p.isUsingItem()
            + " timeline=" + state.log.map(function (e) { return "t" + e.t + ":" + e.n + (e.using ? "u" : ""); }).join(" "));
      });
    })(i);
  }
  scenario("A release after 8 ticks", 8, 30);
  scenario("B held 60 ticks", -1, 60);
  var prev = typeof onTick === "function" ? onTick : null;
  onTick = function (n) { if (prev) prev(n); if (at < plan.length) { try { plan[at](); } catch (e) { lines.push("step " + at + " threw " + e); } at++; } return false; };
  return { report: function () { return (at < plan.length ? "running (" + at + "/" + plan.length + ")\n" : "") + lines.join("\n"); } };
})();
"started";
