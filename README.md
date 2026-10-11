# Monolith Client

Fabric client mod for Minecraft 1.21.11 (Java 21).

## Download
`releases/monolith-1.0.3.jar` is the ready-to-play mod: drop it in your `.minecraft/mods` folder next to Fabric API.
It contains **every 1.0.2 module** (glow, motion blur, view model...) plus the 1.0.3 update below.

## What 1.0.3 adds
* Real 3D filled-box ESP (Storage Finder, Spawner Finder, ESP, X-Ray, Waypoints) with a one-switch **ESP Style** module
* Zoom no longer changes your saved mouse sensitivity (fixes the "stuck low sensitivity" bug)
* New modules: Tracers, Block Highlight, Chunk Borders, Breadcrumbs, Radar
* Faster ESP: batched drawing (4 draw calls per frame) and cached target lists

## What is in `src/`
Source files **for reference / backup**. This is not a buildable Gradle project:
* The 1.0.0 export (`class_xxx` and readable names mixed together) cannot compile as one project.
* The 1.0.2-only classes (GlowRenderer, PostFx, PlayerGlow, ItemGlow, ViewModel, MotionBlur, AuraParticles) exist only compiled, inside the jar.
* The 1.0.3 files (`Render3D`, `Wire`, `ESPModule`, `ESPStyle`, `Zoom`, `EntityLookMixin`, `ExtraModules`, Tracers, Block Highlight,
  Chunk Borders, Breadcrumbs, `Radar`, `esp_fill` shaders) were compiled directly against the names in the 1.0.2 jar and merged into it.

Step-by-step upload instructions for beginners: `docs/GITHUB_GUIDE.md`.
