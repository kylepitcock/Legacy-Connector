// Suit footsteps: Iron Man walks 30 blocks; an observer next to him must receive the hero sound packets (ASMHooks.playStepSound -> dispatchSoundAtEntity STEP).
var MoverType = Java.type("net.minecraft.world.entity.MoverType");
join("StepObs"); flat(1700, 1700, 100); tp(1704.5, 101, 1700.5); var OBS = P;
join("StepIron"); tp(1700.5, 101, 1700.5); wear("iron_man"); var WALKER = P;
var steps = [function () { return 10; }, function () { P = OBS; sentSummary(); P = WALKER; return 0; }];
for (var i = 0; i < 30; i++) steps.push(function () { WALKER.move(MoverType.SELF, new Vec3(0.0, 0.0, 0.25)); return 1; });
steps.push(function () { P = OBS; out("observer received: " + sentSummary()); });
scenario(steps);
"step probe started"
