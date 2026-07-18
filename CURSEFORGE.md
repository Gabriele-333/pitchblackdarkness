# PBD - Pitch Black Darkness

**Minecraft darkness is a lie.** Even at light level 0, in the deepest cave, on a moonless night — you can always see *something*. PBD makes darkness real, and puts you in control of exactly how real.

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

Fractions work too — `/pbd 0.5` keeps a hint of visibility even in unlit caves, for a subtle "vanilla but moodier" feel. The scale is fully continuous.

## The moon actually matters

Like the classic *True Darkness* did, PBD ties night darkness to the **moon phase**: a full moon night stays navigable, a new moon night swallows you whole. Watch the sky — for the first time in Minecraft, you'll actually care what the moon looks like tonight. (Can be turned off, if you want every night equally dark.)

## What it does NOT touch

- **Daylight is sacred.** At every level, full daylight is pixel-identical to vanilla.
- **Torches still work.** Light sources stay exactly as bright as vanilla — they just *matter* now. A torch at night stops being decoration and becomes survival.
- **Lightning still flashes.** Thunderstorms light up even the blackest night, as they should.
- The night **sky** stays visible: stars and moon above a pitch-black world, like a real moonless night.

## Yes, it beats the Brightness slider

Cranking Brightness to max will not save you. PBD applies its darkness *after* the gamma curve, so at level 5, black is black — on any monitor, at any slider setting. No cheating.

## Details

- **Client-side only.** Works in singleplayer and on ANY server (it's your eyes, not the world). Safe to add or remove at any time.
- Change level in-game with **`/pbd <0-5>`** (or `/pitchblack`) — instant effect, no restart. Or use the config screen from the Mods menu.
- Optional toggles for the **Nether** and the **End** (cave-darkness only — no sky light down there).
- Zero performance cost: everything is precomputed; when set to 0 the mod fully steps aside.
- **Compatible with Sodium.** ⚠️ **Not compatible with shader packs** (Iris/OptiFine): shaders replace the entire lighting pipeline and ignore the vanilla lightmap — no darkness mod can affect them (not even Mojang's own Warden darkness works right under most packs).

## Credits

Behavior inspired by **True Darkness** by grondag (Fabric, up to 1.16). PBD is a from-scratch, clean-room implementation for NeoForge — no code was ported.

*A SaturnX Studios (STXS) mod.*
