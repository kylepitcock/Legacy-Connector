// Iron Man MK50 (nanite suit): transformation through the "dyn/nanites" data var, then the blade, shield, the crab cannon (AIM + energy bolt) and flight vars.
var Anchor = Java.type("net.minecraft.commands.arguments.EntityAnchorArgument$Anchor");
var DataVarC = Java.type("com.fiskmods.heroes.common.data.var.DataVar");
var InteractionType2 = Java.type("com.fiskmods.heroes.common.interaction.InteractionType");
var InteractionInfo2 = Java.type("com.fiskmods.heroes.common.interaction.InteractionInfo");
var Interaction2 = Java.type("com.fiskmods.heroes.common.interaction.Interaction");
var LSide2 = Java.type("legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side");
join("Stark2"); flat(1300, 1300, 100); tp(1300.5, 101, 1300.5); wear("iron_man_mk50");
var SP2 = P, sg2 = spawn("minecraft:iron_golem", 1300.5, 101, 1308.5); sg2.setNoAi(true);
function dyn(n) { return DataVarC.REGISTRY.getObject("fiskheroes:dyn/" + n); }
function mods() { return Java.from(hero("iron_man_mk50").getEnabledModifiers(L()).toArray()).map(function (e) { return (String(e).match(/modifier=([^,]*)/)||[0,"?"])[1].replace("fiskheroes:",""); }).join(","); }
function fire(name, type) { var reg = Interaction2.REGISTRY, hit = []; for (var i = 0; i < reg.size(); i++) { var it = reg.get(i); if (it.getClass().getSimpleName() == name) { try { hit.push(it.tryListen(L(), L(), new InteractionInfo2(type, 0, 0, 0, 0, 0, 0), LSide2.SERVER)); } catch (e) { hit.push("EXC " + e); } } } return hit.join(","); }
scenario([
  function () { return 5; },
  function () { out("worn " + worn() + " dyn vars: nanites=" + (dyn("nanites") ? dyn("nanites").get(L()) : "no var") + " enabled modifiers: " + mods()); return 0; },
  function () { var r = Vars.BLADE.set(L(), true); out("BLADE before transform -> " + Vars.BLADE.get(L())); Vars.BLADE.set(L(), false); return 0; },
  function () { if (dyn("nanites")) dyn("nanites").set(L(), true).sync(); return 3; },
  function () { out("nanites on: enabled modifiers: " + mods()); SP2.lookAt(Anchor.EYES, sg2.position().add(0, 1.4, 0)); Vars.BLADE.set(L(), true).sync(); Vars.AIMING.set(L(), true).sync(); return 6; },
  function () { out("BLADE=" + Vars.BLADE.get(L()) + " AIMING=" + Vars.AIMING.get(L()) + " blade timer " + Vars.BLADE_TIMER.get(L()) + " modifiers " + mods()); out("fire EnergyBolt right-click: " + fire("InteractionEnergyBolt", InteractionType2.RIGHT_CLICK_AIR) + " golem hp " + sg2.getHealth() + " sent " + sentSummary()); return 10; },
  function () { out("later golem hp " + sg2.getHealth() + " cooldown etc sent " + sentSummary()); Vars.FLYING.set(L(), true).sync(); return 6; },
  function () { out("FLYING=" + Vars.FLYING.get(L()) + " flight timer " + Vars.FLIGHT_TIMER.get(L()) + " booster " + (dyn("booster_timer") ? dyn("booster_timer").get(L()) : "no var") + " fall " + SP2.fallDistance); }
]);
"mk50 probe started"
