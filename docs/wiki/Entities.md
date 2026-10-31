# Entities

Back to [Home](Home.md); see also [Rendering](Rendering.md).

Each legacy entity class a mod registers (`EntityRegistry.registerModEntity` / `registerGlobalEntityID`) becomes a
modern entity type. Its instances are run by a host: a vanilla entity (`PathfinderMob` for living ones, an `Enemy`
monster for `IMob`s) that constructs the legacy object and forwards ticking, AI, damage, death and drops, sounds,
interaction, saving and spawn data to it. The legacy base classes (`Entity`, `EntityLivingBase`, `EntityLiving`,
`EntityCreature`, `EntityMob`, `EntityAgeable`, `EntityAnimal`, `EntityTameable`) are real implementations on top of
the host, so `super` calls compose as on 1.7.10: the legacy AI scheduler (`EntityAITasks`) runs the mod's tasks, the
vanilla AI tasks are backed by the equivalent modern goals, navigation and move/look/jump helpers drive the host's
own, `DataWatcher` values are synced to clients, legacy attributes map to modern ones, Forge's
`EntityConstructing` event and `IExtendedEntityProperties` work (also for players), and `addSpawn` entries become
biome modifiers.

On the client, renderers registered with `RenderingRegistry.registerEntityRenderingHandler` draw the entities: the
legacy `Render`/`RenderLiving` code runs inside a render context that maps its OpenGL matrix and state calls onto
the modern pose stack and buffers, legacy models (`ModelBase`, `ModelBiped`, `ModelQuadruped`, ...) emit real
geometry, and `Tessellator`/immediate-mode (`glBegin`) geometry is assembled into quads with the matching modern
render type (textured, translucent or additive). Biped renderers draw worn armour (vanilla layers or a mod's
`getArmorTexture`) as render passes and the held item through the modern item renderer.
