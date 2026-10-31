// Held-key abilities that deal damage on the server tick: heat vision (Martian Manhunter comics), charged beam (Vision), energy projection (Spectre),
// sonic waves (Black Canary Dinah), cryo charge (Killer Frost), energy manipulation (Rupture). The target is an iron golem 8 blocks away; the player looks at it.
var Anchor = Java.type("net.minecraft.commands.arguments.EntityAnchorArgument$Anchor");
var F = function (x) { return new (Java.type("java.lang.Float"))(x); };
join("Beamer2"); flat(600, 600, 100); tp(600.5, 101, 600.5);
var BEAMER = P;
var golem = spawn("minecraft:iron_golem", 600.5, 101, 608.5); golem.setNoAi(true);
function aimAt() { BEAMER.lookAt(Anchor.EYES, golem.position().add(0, 1.4, 0)); }
var CASES = [
  { hero: "martian_manhunter_comics", name: "heat vision", on: function () { Vars.HEAT_VISION.set(L(), true).sync(); }, off: function () { Vars.HEAT_VISION.set(L(), false).sync(); }, wait: 40 },
  { hero: "vision", name: "charged beam", on: function () { Vars.BEAM_CHARGING.set(L(), true).sync(); }, off: function () { Vars.BEAM_CHARGING.set(L(), false).sync(); }, wait: 60 },
  { hero: "spectre", name: "energy projection", on: function () { Vars.ENERGY_PROJECTION.set(L(), true).sync(); }, off: function () { Vars.ENERGY_PROJECTION.set(L(), false).sync(); }, wait: 40 },
  { hero: "black_canary_dinah", name: "sonic waves", on: function () { Vars.SONIC_WAVES.set(L(), true).sync(); }, off: function () { Vars.SONIC_WAVES.set(L(), false).sync(); }, wait: 40 },
  { hero: "killer_frost", name: "cryo charge", on: function () { Vars.CRYO_CHARGING.set(L(), true).sync(); }, off: function () { Vars.CRYO_CHARGING.set(L(), false).sync(); }, wait: 60 },
  { hero: "rupture", name: "energy charge", on: function () { Vars.ENERGY_CHARGING.set(L(), true).sync(); }, off: function () { Vars.ENERGY_CHARGING.set(L(), false).sync(); }, wait: 60 }
];
var steps = [];
CASES.forEach(function (c) {
  steps.push(function () { strip(); wear(c.hero); golem.setHealth(100); golem.invulnerableTime = 0; return 4; });
  steps.push(function () { aimAt(); c.on(); c.hp0 = golem.getHealth(); sentSummary(); return 2; });
  steps.push(function () { aimAt(); return c.wait; });
  steps.push(function () {
    out(c.name + " (" + c.hero + "): worn " + worn() + " golem hp " + c.hp0 + " -> " + golem.getHealth() + "; extra entities near: " +
      server.overworld().getEntitiesOfClass(Java.type("net.minecraft.world.entity.Entity").class, new (Java.type("net.minecraft.world.phys.AABB"))(590, 95, 590, 620, 120, 620)).size() + "; sent " + sentSummary());
    c.off(); return 3;
  });
});
scenario(steps);
"beam probe started"
