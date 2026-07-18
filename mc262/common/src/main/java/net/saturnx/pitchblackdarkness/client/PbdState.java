package net.saturnx.pitchblackdarkness.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import net.saturnx.pitchblackdarkness.core.PbdLevels;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Il collegamento tra Minecraft 26.2 e il nucleo {@link PbdLevels}.
 *
 * <p><b>Perché questa classe esiste separata da quella di 1.21.1</b>: dalla 26.1
 * la lightmap non si calcola più sulla CPU. {@code LightTexture} e il suo ciclo
 * per-texel non esistono più; al loro posto c'è uno shader alimentato da un UBO,
 * i cui parametri passano tutti da {@code LightmapRenderState}. Anche
 * {@code getSkyDarken}, {@code getMoonBrightness} e {@code ultraWarm} sono
 * spariti. Il nucleo però è lo stesso: cambia solo <i>come</i> gli si dà da
 * mangiare e <i>dove</i> si applica il risultato.</p>
 *
 * <p><b>I tre hook di 1.21.1 collassano in uno.</b> Nello shader un texel non
 * illuminato vale esattamente {@code max(AmbientColor, nightVision)}, perché
 * {@code get_brightness(0) == 0}; e la luce 15 vale 1 per costruzione. Quindi
 * basta scalare {@code ambientColor} (asse caverna) e {@code skyFactor} (asse
 * notte) e la luce dei blocchi resta intatta da sola: le torce non si toccano
 * senza doverlo chiedere.</p>
 *
 * <p><b>L'ammazza-floor non serve più</b>: la matematica post-gamma di 1.21.1
 * ({@code lerp(0.75,0.04)} ×2 e {@code f*0.95+0.05}) è stata rimossa. Lo shader
 * finisce con {@code mix(color, notGamma(color), BrightnessFactor)} e nient'altro,
 * quindi se la base è zero il nero resta nero anche con lo slider Luminosità al
 * massimo — il requisito non negoziabile della mod arriva gratis.</p>
 */
public final class PbdState {
    /**
     * Vanilla in piena notte: {@code visual/sky_light_factor} interpola da 1.0
     * a 0.24 (data/minecraft/timeline/day.json). Su 1.21.1 l'analogo era 0.2.
     */
    public static final float NIGHT_FLOOR_262 = 0.24F;

    /**
     * Il nero assoluto NON si può passare allo shader.
     *
     * <p>{@code notGamma()} calcola {@code color * (maxScaled / maxComponent)}:
     * con colore esattamente zero è {@code 0/0} → <b>NaN</b>, e il texel diventa
     * un colore indefinito invece che nero. In vanilla non capita mai perché
     * l'ambient non arriva mai a zero; noi invece ci puntiamo, quindi ci fermiamo
     * un passo prima. 1/255 è sotto la soglia di quantizzazione a 8 bit: a schermo
     * è indistinguibile dal nero, ma tiene la divisione definita.</p>
     */
    private static final float BLACK_EPSILON = 1.0F / 255.0F;

    private PbdState() {}

    /** Aggancia al nucleo le letture specifiche di 26.2. */
    public static void install() {
        PbdLevels.setVanillaNightFloor(NIGHT_FLOOR_262);
        PbdLevels.setMoonSource(PbdState::currentMoonBrightness);
    }

    public static boolean active() {
        return PbdLevels.active();
    }

    /**
     * La fase lunare in 26.2 non è più {@code level.getMoonBrightness()}: si
     * legge dagli attributi d'ambiente, come fa vanilla in {@code MoonBrightnessCheck}.
     */
    private static float currentMoonBrightness() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) {
            return 0.0F;
        }
        MoonPhase phase = mc.level.environmentAttributes()
                .getValue(EnvironmentAttributes.MOON_PHASE, mc.player.position());
        return DimensionType.MOON_BRIGHTNESS_PER_PHASE[phase.index()];
    }

    /**
     * Applica il buio ai parametri che stanno per finire nell'UBO dello shader.
     *
     * <p>Chiamata dal mixin al RETURN di {@code extract()}, e <b>solo</b> quando
     * {@code needsUpdate} è vero. È importante: quando è falso vanilla non
     * riscrive i campi, quindi rimoltiplicarli farebbe accumulare la nostra
     * scalatura frame dopo frame fino a spegnere tutto. Lavorando solo sui campi
     * appena riscritti da vanilla, l'operazione è sempre idempotente.</p>
     */
    public static void apply(LightmapRenderState state) {
        // Asse notte: stessa rimappatura di 1.21.1, con il floor di questa
        // versione (0.24). Il giorno (1.0) è un punto fisso: resta 1.0.
        state.skyFactor = PbdLevels.remapSkyDarken(state.skyFactor);

        // Asse caverna, primo pezzo: un texel non illuminato vale esattamente
        // ambientColor (nell'overworld #0a0a0a), quindi è qui che si spegne il
        // buio totale.
        state.ambientColor = scale(state.ambientColor, PbdLevels.crushFactor(0));

        // Asse caverna, secondo pezzo: i livelli di luce INTERMEDI.
        //
        // Su 1.21.1 la curva (luce/15)^exp li schiacciava uno per uno. Qui non si
        // può: quella curva e' dentro il GLSL (`get_brightness`) e dall'UBO
        // passano solo scalari, che agiscono su tutti i livelli insieme.
        //
        // Esiste pero' un margine gratis. Lo shader calcola
        // `get_brightness(level) * BlockFactor` con BlockFactor ~1.4, e poi
        // clampa il colore a 1.0: alla luce 15 il risultato satura comunque, e
        // il 40% eccedente e' sprecato. Portando BlockFactor giu' fino al punto
        // di saturazione (1.0) le torce restano IDENTICHE — continuano a
        // saturare — mentre tutto cio' che sta sotto si scurisce.
        //
        // NB: e' un miglioramento parziale, non l'equivalente della curva di
        // 1.21.1. Sotto 1.0 si inizierebbe a spegnere anche le torce, e questo
        // la mod non lo fa: riprodurre la curva vera richiederebbe sostituire
        // lightmap.fsh, che confliggerebbe con shader pack e altre mod.
        float saturationPoint = Math.min(state.blockFactor, 1.0F);
        state.blockFactor = PbdLevels.lerp(
                PbdLevels.crushStrength(), state.blockFactor, saturationPoint);
    }

    /** Scala un colore mantenendolo sopra lo zero esatto (vedi BLACK_EPSILON). */
    private static Vector3fc scale(Vector3fc color, float factor) {
        float f = Math.max(factor, 0.0F);
        return new Vector3f(
                Math.max(color.x() * f, BLACK_EPSILON),
                Math.max(color.y() * f, BLACK_EPSILON),
                Math.max(color.z() * f, BLACK_EPSILON));
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
    }

    /** Da chiamare a ogni client tick, dal modulo del loader. */
    public static void clientTick() {
        if (!Minecraft.getInstance().isPaused()) {
            PbdLevels.tickBoost();
        }
        PbdLevels.recompute();
    }
}
