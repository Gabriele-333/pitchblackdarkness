# PBD — Pitch Black Darkness

**Minecraft darkness is a lie.** Even at light level 0, in the deepest cave, on a moonless night — you can always see *something*. PBD makes darkness real, and puts you in control of exactly how real.

Client-side mod for **NeoForge 1.21.1** (Java 21). A SaturnX Studios (STXS) mod.

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

- **`/pbd <0-5>`** (alias `/pitchblack`) — instant effect, no restart. Without arguments it reports the current level.
- Config screen from the **Mods menu**.
- Options: `darknessLevel` (0.0–5.0, default 3.0), `moonMatters`, `affectNether`, `affectEnd`.

## Compatibility

- **Client-side only.** Works in singleplayer and on ANY server. Safe to add or remove at any time; on a dedicated server it does nothing.
- ✅ **Sodium.**
- ⚠️ **Not compatible with shader packs** (Iris/OptiFine): shaders replace the entire lighting pipeline and ignore the vanilla lightmap — no darkness mod can affect them.
- Zero performance cost: everything is precomputed per-tick; at level 0 the mod fully steps aside.

## How it works

Three hooks, all client-side mixins:

1. **Cave axis** — `LightTexture.getBrightness` return value multiplied by a crush curve `(light/15)^exp`, from a precomputed 16-entry lookup table.
2. **Night axis** — `ClientLevel.getSkyDarken(float)` return value scaled, with the remaining sky contribution modulated by the moon phase. Sky and fog use their own curves, so stars and the moon stay visible above a black world.
3. **Floor killer** — vanilla applies `lerp(0.75, 0.04)` twice plus `f*0.95+0.05` *after* the gamma, which lifts pure black back to ~RGB 36 at max Brightness. A `@WrapOperation` (MixinExtras) on `NativeImage.setPixelRGBA` in `updateLightTexture` gates the post-gamma value: 0 where the target is black, bit-identical to vanilla where the texel is legitimate.

## Building

Requires **JDK 21**.

```bash
./gradlew build        # jar in build/libs/
./gradlew runClient    # dev client
```

## Credits

Behavior inspired by **True Darkness** by grondag (Fabric, up to 1.16). PBD is a from-scratch, clean-room implementation for NeoForge — no code was ported.

## License

[MIT](LICENSE).
