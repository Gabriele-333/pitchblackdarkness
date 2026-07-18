# PBD — Pitch Black Darkness

**Minecraft darkness is a lie.** Even at light level 0, in the deepest cave, on a moonless night — you can always see *something*. PBD makes darkness real, and puts you in control of exactly how real.

Client-side mod. A SaturnX Studios (STXS) mod.

| Minecraft | NeoForge | Fabric |
|---|---|---|
| **1.21.1** | ✅ | ✅ |
| **26.2** | ✅ | ⏳ *(waiting on Fabric tooling — see below)* |

## One number. That's the whole mod.

Pick your darkness level, from **0** (pure vanilla) to **5** (absolute pitch black):

| Level | Unlit caves | Moonless night | Full moon night |
|---|---|---|---|
| 0 | vanilla | vanilla | vanilla |
| 1 | almost black | a bit darker | ~vanilla |
| 2 | black | much darker | darker |
| 3 *(default)* | black | nearly black | dark but navigable |
| 4 | black | black | very dark |
| 5 | **absolute black** | **BLACK** | barely a glow |

The scale is fully continuous — `/pbd 0.5` keeps a hint of visibility even in unlit caves, for a subtle "vanilla but moodier" feel.

## The moon actually matters

Like the classic *True Darkness* did, PBD ties night darkness to the **moon phase**: a full moon night stays navigable, a new moon night swallows you whole. Watch the sky — for the first time in Minecraft, you'll actually care what the moon looks like tonight. (Can be turned off.)

## What it does NOT touch

- **Daylight is sacred.** At every level, full daylight is pixel-identical to vanilla.
- **Torches still work.** Light sources stay exactly as bright as vanilla — they just *matter* now.
- **Lightning still flashes.** Thunderstorms light up even the blackest night.
- The night **sky** stays visible: stars and moon above a pitch-black world.

## Yes, it beats the Brightness slider

Cranking Brightness to max will not save you. PBD applies its darkness *after* the gamma curve, so at level 5, black is black — on any monitor, at any slider setting.

## Usage

Everything lives in **Options → Video Settings**, right next to Brightness:

- **Darkness level** — `0`…`5`, live preview while you drag
- **Moon phase matters**, **Affect the Nether**, **Affect the End**

Or use **`/pbd <0-5>`** (alias `/pitchblack`) — instant effect, no restart. Without arguments it reports the current level.

There is no config-library dependency and no mod-menu dependency: the settings are injected into the vanilla screen, so they are in the same place on every loader.

## Compatibility

- **Client-side only.** Works in singleplayer and on ANY server. Safe to add or remove at any time; on a dedicated server it does nothing.
- **No required dependencies** beyond the loader (Fabric additionally needs Fabric API).
- ✅ **Sodium.**
- ⚠️ **Not compatible with shader packs** (Iris/OptiFine): shaders replace the entire lighting pipeline and ignore the vanilla lightmap — no darkness mod can affect them.
- Zero performance cost: everything is precomputed per-tick; at level 0 the mod fully steps aside.

## How it works

Minecraft moved the lightmap from the CPU to the GPU in 26.1, so there are two implementations. Both are client-side mixins, and both drive the same shared level curve.

### 1.21.1 — three hooks

1. **Cave axis** — `LightTexture.getBrightness` return value multiplied by a crush curve `(light/15)^exp`, from a precomputed 16-entry lookup table.
2. **Night axis** — `ClientLevel.getSkyDarken(float)` return value scaled, with the remaining sky contribution modulated by the moon phase. Sky and fog use their own curves, so stars and the moon stay visible above a black world.
3. **Floor killer** — vanilla applies `lerp(0.75, 0.04)` twice plus `f*0.95+0.05` *after* the gamma, which lifts pure black back to ~RGB 36 at max Brightness. A `@WrapOperation` (MixinExtras) on `NativeImage.setPixelRGBA` gates the post-gamma value: 0 where the target is black, bit-identical to vanilla where the texel is legitimate.

### 26.2 — one hook

The lightmap is now a shader fed by a uniform buffer, so everything goes through a single injection on `LightmapRenderStateExtractor.extract()`:

- **Cave axis** — an unlit texel evaluates to exactly `ambientColor` (`get_brightness(0) == 0`), so scaling that colour blacks out unlit space while light level 15 is untouched by construction. Intermediate levels are crushed by pulling `blockFactor` down to its saturation point, which is free: the shader clamps to 1.0, so torches keep saturating.
- **Night axis** — `skyFactor` remapped exactly as on 1.21.1, only against this version's night floor (`0.24` instead of `0.2`).
- **No floor killer needed** — the post-gamma lift was removed in the rework, so zero stays zero on its own.

**Known limitation on 26.2:** the per-level crush curve now lives inside the vanilla GLSL and only scalars reach it, so *semi-lit* areas stay somewhat brighter than on 1.21.1. Fully unlit space and moonless nights are unaffected by this. Matching 1.21.1 exactly would require shipping a replacement `lightmap.fsh`, which would conflict with shader packs and other mods — a trade we chose not to make.

## Building

Requires **JDK 25** (Minecraft 26.2 needs it, and the 1.21.1 modules build fine under it too).

```bash
./gradlew build   # jars in build/libs/<loader>/
```

Dev clients:

```bash
./gradlew :mc1211:neoforge:runClient
./gradlew :mc1211:fabric:runClient
./gradlew :mc262:neoforge:runClient
```

### Project layout

```
core/                       plain Java, zero net.minecraft imports —
                            level curve, public API, platform interface
mc1211/common               Minecraft 1.21.1: the three mixins
mc1211/{neoforge,fabric}    loader glue: config, tick, command
mc262/common                Minecraft 26.2: the lightmap mixin
mc262/neoforge              loader glue
mc262/fabric                written, disabled — see below
```

`core` is the only module that survives a Minecraft version bump untouched, which is why it is kept free of game imports by construction: it has no Minecraft plugin applied, so a stray import will not compile.

**`mc262/fabric` is currently disabled** in `settings.gradle`. Minecraft is unobfuscated from 26.1 and Fabric stopped maintaining Yarn/Intermediary, so `officialMojangMappings()` no longer resolves — but removing the `mappings` line fails too, on both `fabric-loom` and `fabric-loom-remap`. The module is complete and will be re-enabled once the toolchain settles.

## For mod developers

`net.saturnx.pitchblackdarkness.api.PitchBlackApi` lets other mods drive the darkness:

```java
PitchBlackApi.getLevel();
PitchBlackApi.setLevel(4.0);
PitchBlackApi.pushTransientBoost(1.0F, 160, 20);  // temporary, with fade in/out
```

It imports no Minecraft classes, so the coordinate is stable across loaders and game versions.

## Credits

Behavior inspired by **True Darkness** by grondag (Fabric, up to 1.16). PBD is a from-scratch, clean-room implementation — no code was ported.

## License

[MIT](LICENSE).
