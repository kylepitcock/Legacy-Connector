// Fisk's Heroes ability key HUD and key dispatch on the CLIENT (issue #118: the key hint list flickers between the transformed and the untransformed set,
// the Iron Man blade and crab cannon do nothing). Run in a control client session with the Iron Man Mk50 chestplate on:
//   SCENARIO=control tools/dev/quick-shots.sh      (then, in the game: /give or the Suits tab; wear the arc reactor)
//   tools/dev/js.sh -f tools/dev/fisk/probe-client-keys.js     starts the recorder
//   ... press G (transform), hold C (aim), press V (blade) for a few seconds ...
//   tools/dev/js.sh 'FLK.report()'                             prints every tick on which any input of the key list changed
// Every changed tick prints which input flipped: hero (worn suit seen by the client), nanites, mask_open_timer, flying, sprinting, sneaking, the held item,
// each ability key (down state) and the key list the HUD would draw (suitKeyDescription != the key's own description).
// Mod-specific development tool: nothing here ships.
var Vars = Java.type("com.fiskmods.heroes.common.data.var.Vars");
var DataVarC = Java.type("com.fiskmods.heroes.common.data.var.DataVar");
var HeroTrackerC = Java.type("com.fiskmods.heroes.common.hero.HeroTracker");
var SHKeyBindsC = Java.type("com.fiskmods.heroes.client.keybinds.SHKeyBinds");
var EntityBridgeC = Java.type("org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge");
var ClientTickEventC = Java.type("net.neoforged.neoforge.client.event.ClientTickEvent");
var NeoForgeC = Java.type("net.neoforged.neoforge.common.NeoForge");
var EventPriorityC = Java.type("net.neoforged.bus.api.EventPriority");

if (typeof FLK !== "undefined" && FLK.listener) { try { NeoForgeC.EVENT_BUS.unregister(FLK.listener); } catch (e) {} }
var FLK = { n: 0, last: null, lines: [], listener: null };
function flkDyn(name, p) { var v = DataVarC.REGISTRY.getObject("fiskheroes:dyn/" + name); return v ? String(v.get(p)) : "-"; }
function flkSnap() {
  var p = EntityBridgeC.wrap(mc.player);
  if (p == null) return "no player";
  var iter = HeroTrackerC.iter(p), hero = iter == null ? "none" : String(iter.hero.getName()).replace("fiskheroes:", "");
  var keys = [], shown = [];
  for (var i = 0; i < SHKeyBindsC.KEY_MAPPINGS.length; i++) {
    var k = SHKeyBindsC.KEY_MAPPINGS[i];
    if (k == null) continue;
    keys.push(i + "=" + (k.getIsKeyPressed() ? "DOWN" : "up"));
    if (String(k.suitKeyDescription) != String(k.getKeyDescription())) shown.push(String(k.suitKeyDescription).replace("key.", ""));
  }
  return "hero=" + hero + " nanites=" + flkDyn("nanites", p) + " nanite_timer=" + flkDyn("nanite_timer", p) + " mask_open_timer=" + Vars.MASK_OPEN_TIMER.get(p)
    + " flying=" + Vars.FLYING.get(p) + " sprint=" + p.isSprinting() + " sneak=" + p.isSneaking() + " held=" + (p.getHeldItem() == null ? "empty" : "item")
    + " aiming=" + Vars.AIMING.get(p) + " blade=" + Vars.BLADE.get(p) + " shield=" + Vars.SHIELD.get(p) + " | keys " + keys.join(" ") + " | HUD [" + shown.join(", ") + "]";
}
FLK.listener = new java.util.function.Consumer(function (e) {
  FLK.n++;
  var s;
  try { s = flkSnap(); } catch (ex) { s = "ERR " + ex; }
  if (s != FLK.last) { FLK.lines.push("[" + FLK.n + "] " + s); FLK.last = s; if (FLK.lines.length > 400) FLK.lines.shift(); }
});
// ClientTickEvent.Post is the same moment Fisk's own ClientTickEvent END handler (KeyBindTranslator.translate) has run
NeoForgeC.EVENT_BUS.addListener(EventPriorityC.LOWEST, false, ClientTickEventC.Post.class, FLK.listener);
FLK.report = function () { var s = FLK.lines.join("\n"); FLK.lines = []; return s; };
"client key recorder started"
