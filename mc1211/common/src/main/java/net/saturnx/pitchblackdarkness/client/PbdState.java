package net.saturnx.pitchblackdarkness.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.dimension.DimensionType;
import net.saturnx.pitchblackdarkness.core.PbdLevels;

/**
 * Il collegamento tra Minecraft 1.21.1 e il nucleo {@link PbdLevels}.
 *
 * <p>Tutta la matematica del buio vive in {@code core}, che non conosce
 * Minecraft. Qui c'è solo ciò che <b>dipende dalla versione del gioco</b>: come
 * si legge la fase lunare, come si riconosce la dimensione, e il calcolo del
 * gate del cielo. Sono esattamente i punti che dalla 26.x in poi cambiano
 * ({@code getMoonBrightness} e {@code getSkyDarken} non esistono più,
 * {@code ultraWarm} nemmeno), quindi la 26.2 avrà una classe sua e il nucleo
 * resterà condiviso senza modifiche.</p>
 *
 * <p>I mixin chiamano questa classe; questa classe non alloca e non legge la
 * config nei punti caldi — legge solo i campi già precalcolati dal nucleo.</p>
 */
public final class PbdState {
    private static boolean affectsCurrentDim;

    private PbdState() {}

    /** Aggancia al nucleo il modo 1.21.1 di leggere la fase lunare. */
    public static void install() {
        PbdLevels.setMoonSource(() -> {
            var mc = Minecraft.getInstance();
            return (mc != null && mc.level != null) ? mc.level.getMoonBrightness() : 0.0F;
        });
    }

    // ===== Hot path: quello che i mixin leggono =====

    public static boolean active() {
        return PbdLevels.active();
    }

    public static float crushFactor(int lightLevel) {
        return PbdLevels.crushFactor(lightLevel);
    }

    public static float remapSkyDarken(float vanilla) {
        return PbdLevels.remapSkyDarken(vanilla);
    }

    public static int gateLightmapColor(int color, int blockLevel, int skyLevel) {
        return PbdLevels.gateLightmapColor(color, blockLevel, skyLevel);
    }

    /** Come sopra, ma per la dimensione in cui il giocatore è ADESSO (cache per-tick). */
    public static boolean affectsCurrentDimension() {
        return affectsCurrentDim;
    }

    /**
     * Dimensioni overworld-like (con sky-light) sempre; Nether/End solo se il
     * giocatore lo chiede. Distinzione senza registry lookup: niente sky-light
     * + ultraWarm = nether-like, altrimenti end-like.
     */
    public static boolean affectsDimension(DimensionType dimensionType) {
        if (dimensionType.hasSkyLight()) {
            return true;
        }
        return dimensionType.ultraWarm()
                ? PbdLevels.platform().affectNether()
                : PbdLevels.platform().affectEnd();
    }

    // ===== Deleghe usate da comando, schermata e API =====

    public static float level() {
        return PbdLevels.level();
    }

    public static void setLevel(double level) {
        PbdLevels.setLevel(level);
    }

    public static void previewLevel(double level) {
        PbdLevels.previewLevel(level);
    }

    public static void refresh() {
        PbdLevels.refresh();
        updateWorldState();
    }

    // ===== Tick =====

    /**
     * Da chiamare a ogni client tick. Non è un evento: lo aggancia il modulo del
     * loader con la propria API ({@code ClientTickEvent.Post} su NeoForge,
     * {@code ClientTickEvents.END_CLIENT_TICK} su Fabric).
     */
    public static void clientTick() {
        if (!Minecraft.getInstance().isPaused()) {
            PbdLevels.tickBoost();
        }
        PbdLevels.recompute();
        updateWorldState();
    }

    /**
     * Cache per-tick di ciò che dipende dal mondo: dimensione corrente e quanto
     * cielo "resta" in questo istante.
     */
    private static void updateWorldState() {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            affectsCurrentDim = false;
            PbdLevels.setSkyGate(1.0F);
            return;
        }

        affectsCurrentDim = affectsDimension(mc.level.dimensionType());

        if (mc.level.getSkyFlashTime() > 0) {
            // Fulmine: vanilla porta il fattore cielo a 1.0 di colpo — il lampo
            // deve restare visibile anche nella notte più nera.
            PbdLevels.setSkyGate(1.0F);
            return;
        }

        // getSkyDarken qui passa dal nostro mixin, quindi è già rimappato: si
        // ricava il rapporto rimappato/vanilla per sapere quanto cielo resta
        // in questo istante (1.0 a mezzogiorno, m' a mezzanotte).
        float nightFloor = PbdLevels.nightFloor();
        float remapped = mc.level.getSkyDarken(1.0F);
        float dayness = Mth.clamp((remapped - nightFloor) / Math.max(1.0F - nightFloor, 1.0E-4F), 0.0F, 1.0F);
        float vanilla = dayness * (1.0F - PbdLevels.vanillaNightFloor()) + PbdLevels.vanillaNightFloor();
        PbdLevels.setSkyGate(remapped / vanilla);
    }
}
