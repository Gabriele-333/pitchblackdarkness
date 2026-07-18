package net.saturnx.pitchblackdarkness.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.saturnx.pitchblackdarkness.Pbd;
import net.saturnx.pitchblackdarkness.client.PbdState;
import net.saturnx.pitchblackdarkness.platform.PbdPlatform;

/**
 * Config client su NeoForge, e insieme l'implementazione di {@link PbdPlatform}.
 * PICCOLA di proposito: l'unica opzione che conta davvero è il livello.
 *
 * <p>I valori NON vanno letti nei punti caldi del rendering: a ogni load/reload
 * {@link PbdState#refresh()} li precalcola in campi statici piatti. I mixin
 * leggono solo quelli.</p>
 */
@EventBusSubscriber(modid = Pbd.MOD_ID, value = Dist.CLIENT)
public final class PbdNeoForgeConfig implements PbdPlatform {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** Il livello 0–5: l'unica cosa che il giocatore deve capire. Mezzi step ammessi (es. 0.5). */
    public static final ModConfigSpec.DoubleValue DARKNESS_LEVEL = BUILDER
            .comment("How dark is dark. 0 = vanilla, 1 = mild, 3 = dark nights and black caves,",
                    "5 = absolute pitch black (moonless nights included). Daylight is never touched.",
                    "Fractions work too: 0.5 keeps a hint of visibility even in unlit caves.",
                    "In-game command: /pbd <0-5>")
            .defineInRange("darknessLevel", 3.0, 0.0, 5.0);

    /** La fase lunare modula la notte (la feature identitaria di True Darkness). */
    public static final ModConfigSpec.BooleanValue MOON_MATTERS = BUILDER
            .comment("If true, the moon phase matters: full moon nights stay somewhat visible,",
                    "new moon nights get the full darkness of your level.",
                    "If false, every night is as dark as a moonless one.")
            .define("moonMatters", true);

    public static final ModConfigSpec.BooleanValue AFFECT_NETHER = BUILDER
            .comment("Apply darkness in the Nether too (cave axis only: the Nether has no sky light).")
            .define("affectNether", false);

    public static final ModConfigSpec.BooleanValue AFFECT_END = BUILDER
            .comment("Apply darkness in the End too (cave axis only: the End has no sky light).")
            .define("affectEnd", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static final PbdNeoForgeConfig INSTANCE = new PbdNeoForgeConfig();

    private PbdNeoForgeConfig() {}

    // ===== PbdPlatform =====

    @Override
    public double darknessLevel() {
        return DARKNESS_LEVEL.get();
    }

    @Override
    public void setDarknessLevel(double level) {
        DARKNESS_LEVEL.set(level);
    }

    @Override
    public boolean moonMatters() {
        return MOON_MATTERS.get();
    }

    @Override
    public boolean affectNether() {
        return AFFECT_NETHER.get();
    }

    @Override
    public boolean affectEnd() {
        return AFFECT_END.get();
    }

    // ===== Ricalcolo a ogni load/reload =====

    @SubscribeEvent
    static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            PbdState.refresh();
        }
    }

    @SubscribeEvent
    static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            PbdState.refresh();
        }
    }
}
