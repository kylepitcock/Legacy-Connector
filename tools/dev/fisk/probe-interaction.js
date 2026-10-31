// Fires one named Interaction (server side, as MessageInteraction would) for a hero against a golem 8 blocks away and reports damage, new entities and the packets sent.
// usage: set CASE = {hero, name, type} then load this file.
var Anchor4 = Java.type("net.minecraft.commands.arguments.EntityAnchorArgument$Anchor");
var AABB4 = Java.type("net.minecraft.world.phys.AABB");
var Interaction4 = Java.type("com.fiskmods.heroes.common.interaction.Interaction");
var InteractionType4 = Java.type("com.fiskmods.heroes.common.interaction.InteractionType");
var InteractionInfo4 = Java.type("com.fiskmods.heroes.common.interaction.InteractionInfo");
var LSide4 = Java.type("legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side");
var CASES4 = [
  { hero: "black_lightning", name: "InteractionLightning", type: "RIGHT_CLICK_AIR", pre: function () { }, label: "lightning cast" },
  { hero: "shazam_dceu", name: "InteractionLightning", type: "RIGHT_CLICK_AIR", pre: function () { }, label: "lightning cast (Shazam)" },
  { hero: "firestorm", name: "InteractionFireball", type: "RIGHT_CLICK_AIR", pre: function () { Vars.AIMING.set(L(), true).sync(); }, label: "fireball" },
  { hero: "killer_frost", name: "InteractionIcicleShoot", type: "RIGHT_CLICK_AIR", pre: function () { }, label: "icicle" },
  { hero: "geomancer", name: "InteractionEarthquake", type: "RIGHT_CLICK_AIR", pre: function () { }, label: "earthquake" },
  { hero: "geomancer", name: "InteractionGroundSmash", type: "RIGHT_CLICK_AIR", pre: function () { }, label: "ground smash" },
  { hero: "senor_cactus", name: "InteractionCactusSummon", type: "RIGHT_CLICK_AIR", pre: function () { }, label: "cactus summon" },
  { hero: "rupture", name: "InteractionEnergyBlast", type: "RIGHT_CLICK_AIR", pre: function () { }, label: "energy blast" },
  { hero: "harbinger", name: "InteractionEnergyDischarge", type: "RIGHT_CLICK_AIR", pre: function () { }, label: "energy discharge" },
  { hero: "doctor_strange", name: "KeyPressSpellMenu", type: "KEY_PRESS", pre: function () { }, label: "spell menu" },
  { hero: "batman_dceu", name: "InteractionEquipment", type: "RIGHT_CLICK_AIR", pre: function () { }, label: "batman equipment" },
  { hero: "deathstroke_dceu", name: "InteractionEquipment", type: "RIGHT_CLICK_AIR", pre: function () { }, label: "deathstroke equipment" },
  { hero: "spider_man", name: "InteractionEquipment", type: "RIGHT_CLICK_AIR", pre: function () { }, label: "spider-man web equipment" }
];
if (typeof SLICE !== "undefined") CASES4 = CASES4.slice(SLICE[0], SLICE[1]);
if (typeof IPN === "undefined") IPN = 0; IPN++;
join("Interactor" + IPN); flat(2300, 2300, 100); tp(2300.5, 101, 2300.5);
var IP = P, ig = spawn("minecraft:iron_golem", 2300.5, 101, 2308.5); ig.setNoAi(true);
function cnt() { return server.overworld().getEntitiesOfClass(Java.type("net.minecraft.world.entity.Entity").class, new AABB4(2280, 90, 2280, 2320, 130, 2330)).size(); }
var steps4 = [];
CASES4.forEach(function (c) {
  steps4.push(function () { strip(); wear(c.hero); ig.setHealth(100); ig.invulnerableTime = 0; return 4; });
  steps4.push(function () { IP.lookAt(Anchor4.EYES, ig.position().add(0, 1.2, 0)); c.pre(); c.before = cnt(); sentSummary(); var reg = Interaction4.REGISTRY, res = []; for (var i = 0; i < reg.size(); i++) { var it = reg.get(i); if (it.getClass().getSimpleName() == c.name) { try { res.push(it.tryListen(L(), L(), new InteractionInfo4(InteractionType4.valueOf(c.type), 0, 0, 0, 0, 0, 0), LSide4.SERVER)); } catch (e) { res.push("EXC " + e); } } } c.res = res.join(","); return 12; });
  steps4.push(function () { out(c.label + " [" + c.hero + "]: listen=" + c.res + " golem " + c.pre.length + " hp " + ig.getHealth() + " entities +" + (cnt() - c.before) + " packets: " + sentSummary().replace(/Clientbound/g, "").substring(0, 160)); Vars.AIMING.set(L(), false).sync(); return 20; });
});
scenario(steps4);
"interaction probe started"
