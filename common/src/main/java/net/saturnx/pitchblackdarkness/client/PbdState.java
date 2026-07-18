package net.saturnx.pitchblackdarkness.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.dimension.DimensionType;
import net.saturnx.pitchblackdarkness.Pbd;

/**
 * Il cuore della mod: trasforma il livello 0–5 (più l'eventuale boost transiente
 * dell'API) nei pochi float piatti che i mixin leggono a ogni frame.
 *
 * <p><b>Contratto di performance</b>: {@code getBrightness} e la lightmap girano
 * di continuo — qui non si alloca niente e non si legge la config nei punti
 * caldi. Tutto viene precalcolato una volta per tick ({@link #recompute()}:
 * qualche decina di operazioni float, costo irrisorio) e i mixin leggono solo
 * campi statici già pronti.</p>
 *
 * <p><b>I due assi del buio</b> (diagnosi in CLAUDE.md):</p>
 * <ul>
 *   <li><b>Asse caverna</b> — {@link #crushFactor(int)}: curva {@code (luce/15)^exp}
 *       moltiplicata sull'uscita della rampa di {@code LightTexture.getBrightness}.
 *       Luce 15 resta 1.0 (il giorno e le torce non si toccano), luce 0 collassa
 *       a 0. Precalcolata in una lookup table da 16 voci: zero pow per texel.</li>
 *   <li><b>Asse notte</b> — {@link #remapSkyDarken(float)}: il fattore notturno
 *       vanilla ({@code getSkyDarken} rende {@code f1*0.8 + 0.2}, quindi la notte
 *       piena vale 0.2 e per questo si vede) viene rimappato abbassando il floor
 *       notturno a {@code 0.2 * m}, dove m dipende dal livello e dalla fase
 *       lunare. Il giorno (valore 1.0) è un punto fisso: resta 1.0 identico.</li>
 * </ul>
 */
public final class PbdState {
    // ===== La curva dei livelli (tabella in CLAUDE.md; indice = livello-1) =====
    /** Esponente della curva caverna: più alto = i livelli di luce bassi collassano prima. */
    private static final float[] CRUSH_EXP = {2.0F, 3.0F, 4.0F, 6.5F, 8.0F};
    /**
     * Quanto resta del contributo cielo in piena notte SENZA luna (0 = notte nera).
     * Taratura playtest 2026-07-17: 4 e 5 staccavano troppo — il vecchio 0.08 al
     * livello 4 teneva l'80% del floor residuo nel gate (soglia 0.1), il 5 lo
     * azzera: 0.04 porta il gate a 0.4 e la scala torna progressiva. Il 3 scende
     * da 0.18 a 0.15 per tenere uniforme anche il gradino 3→4.
     */
    private static final float[] SKY_NIGHT = {0.60F, 0.35F, 0.15F, 0.04F, 0.00F};
    /** Bonus di visibilità con luna piena, scalato da getMoonBrightness() (1.0 = piena). */
    private static final float[] MOON_BONUS = {0.25F, 0.20F, 0.15F, 0.10F, 0.05F};

    /** Vanilla: getSkyDarken restituisce f1*0.8+0.2 — la notte piena vale 0.2, mai meno. */
    private static final float VANILLA_NIGHT_FLOOR = 0.2F;

    /**
     * Soglia del gate ammazza-floor (Hook 3): i texel con visibilità target
     * sotto il 10% vengono spenti in proporzione, quelli sopra restano
     * bit-identici a vanilla. 1/0.1 precalcolato.
     */
    private static final float GATE_INV_THRESHOLD = 10.0F;

    // ===== Output precalcolati: gli UNICI campi letti dai mixin =====
    private static boolean active;
    private static final float[] crushTable = new float[16];
    private static float nightFloor = VANILLA_NIGHT_FLOOR;
    /** Visibilità del contributo cielo ADESSO: 1.0 di giorno, m' in piena notte. */
    private static float skyGate = 1.0F;
    private static boolean affectsCurrentDim;
    private static boolean affectNether;
    private static boolean affectEnd;

    // ===== Config cache (aggiornata SOLO da refresh(): mai .get() nel tick) =====
    private static float configLevel;
    private static boolean moonMatters = true;

    // ===== Boost transiente (API per V112): envelope in/out, stile DarknessClient =====
    private static int boostTicksLeft;
    private static int boostTotalTicks;
    private static int boostFadeTicks;
    private static float boostPeak;

    static {
        // Stato di quiete: identità ovunque finché la config non viene caricata.
        crushTable[0] = 1.0F;
        java.util.Arrays.fill(crushTable, 1.0F);
    }

    private PbdState() {}

    // ------------------------------------------------------------------
    // Hot path: chiamati dai mixin a ogni aggiornamento della lightmap.
    // ------------------------------------------------------------------

    /** True se c'è QUALCOSA da fare (livello > 0 o boost attivo). A livello 0 i mixin escono qui. */
    public static boolean active() {
        return active;
    }

    /** Fattore caverna per il livello di luce dato (0–15). Lookup pura, niente pow. */
    public static float crushFactor(int lightLevel) {
        return crushTable[lightLevel & 15];
    }

    /**
     * Rimappa il valore di ritorno di {@code getSkyDarken(float)}: il floor
     * notturno vanilla (0.2) scende a {@link #nightFloor}, il giorno (1.0) resta
     * esattamente 1.0, e in mezzo è la stessa interpolazione lineare di vanilla
     * — quindi alba e tramonto restano morbidi per costruzione.
     */
    public static float remapSkyDarken(float vanilla) {
        float dayness = Mth.clamp((vanilla - VANILLA_NIGHT_FLOOR) / (1.0F - VANILLA_NIGHT_FLOOR), 0.0F, 1.0F);
        return dayness * (1.0F - nightFloor) + nightFloor;
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
        return dimensionType.ultraWarm() ? affectNether : affectEnd;
    }

    /** Come sopra, ma per la dimensione in cui il giocatore è ADESSO (cache per-tick). */
    public static boolean affectsCurrentDimension() {
        return affectsCurrentDim;
    }

    /**
     * Hook 3: gate ammazza-floor sul colore FINALE del texel della lightmap
     * (post-gamma — è ciò che rende il nero a prova di slider). La visibilità
     * target del texel è il massimo tra asse blocco e asse cielo (quest'ultimo
     * scalato dalla visibilità notturna corrente); sotto il 10% il colore viene
     * spento in proporzione, sopra resta bit-identico a vanilla.
     *
     * <p>Formato colore: ABGR packed come lo scrive updateLightTexture
     * ({@code 0xFF000000 | b<<16 | g<<8 | r}).</p>
     */
    public static int gateLightmapColor(int color, int blockLevel, int skyLevel) {
        float target = Math.max(crushTable[blockLevel & 15], crushTable[skyLevel & 15] * skyGate);
        float gate = target * GATE_INV_THRESHOLD;
        if (gate >= 1.0F) {
            return color;
        }
        int r = (int) ((color & 0xFF) * gate);
        int g = (int) ((color >> 8 & 0xFF) * gate);
        int b = (int) ((color >> 16 & 0xFF) * gate);
        return 0xFF000000 | b << 16 | g << 8 | r;
    }

    // ------------------------------------------------------------------
    // Livello e boost (comando, config screen, API per V112).
    // ------------------------------------------------------------------

    /** Il livello persistito del giocatore (0–5, decimali ammessi: la curva è continua). */
    public static float level() {
        return configLevel;
    }

    /** Cambia il livello persistito: effetto immediato, salvataggio su file. */
    public static void setLevel(double level) {
        double clamped = Mth.clamp(level, 0.0, 5.0);
        Pbd.platform().setDarknessLevel(clamped);
        refresh();
    }

    /**
     * Spinta temporanea verso il buio massimo, con dissolvenza in entrata e in
     * uscita. NON tocca il livello persistito: si compone con esso prendendo il
     * massimo. intensity01 = 1.0 equivale a livello 5 pieno.
     */
    public static void pushTransientBoost(float intensity01, int durationTicks, int fadeTicks) {
        if (durationTicks <= 0 || intensity01 <= 0.0F) {
            return;
        }
        boostTotalTicks = durationTicks;
        boostTicksLeft = durationTicks;
        boostFadeTicks = Math.max(1, Math.min(fadeTicks, durationTicks / 2));
        boostPeak = Mth.clamp(intensity01, 0.0F, 1.0F);
        recompute();
    }

    /** Spegne il boost transiente all'istante (il livello persistito resta). */
    public static void clearTransientBoost() {
        boostTicksLeft = 0;
        boostTotalTicks = 0;
        boostPeak = 0.0F;
        recompute();
    }

    /** Rilegge la config e ricalcola tutto. Chiamata a ogni load/reload/setLevel. */
    public static void refresh() {
        var platform = Pbd.platform();
        configLevel = (float) platform.darknessLevel();
        moonMatters = platform.moonMatters();
        affectNether = platform.affectNether();
        affectEnd = platform.affectEnd();
        recompute();
    }

    // ------------------------------------------------------------------
    // Tick: countdown del boost e ricalcolo (fase lunare inclusa).
    // ------------------------------------------------------------------

    /**
     * Da chiamare a ogni client tick. Non è un evento: lo aggancia il modulo del
     * loader con la propria API ({@code ClientTickEvent.Post} su NeoForge,
     * {@code ClientTickEvents.END_CLIENT_TICK} su Fabric).
     */
    public static void clientTick() {
        if (boostTicksLeft > 0 && !Minecraft.getInstance().isPaused()) {
            boostTicksLeft--;
        }
        recompute();
    }

    /** Envelope del boost, 0–1: min(rampa ingresso, rampa uscita), mai sopra 1. */
    private static float boostValue() {
        if (boostTicksLeft <= 0 || boostPeak <= 0.0F) {
            return 0.0F;
        }
        int elapsed = boostTotalTicks - boostTicksLeft;
        float envelope = Math.min(1.0F, Math.min(
                (float) elapsed / boostFadeTicks,
                (float) boostTicksLeft / boostFadeTicks));
        return boostPeak * envelope;
    }

    /**
     * Dal livello effettivo (float: config + boost, max dei due) ai valori piatti.
     *
     * <p>Il livello effettivo L vive in [0,5]. La forza s = min(L,1) fa da
     * dissolvenza globale (per L sotto 1, es. durante il fade del boost, l'effetto
     * sfuma verso vanilla); da 1 in su i parametri si interpolano linearmente
     * sulla tabella, quindi ai livelli interi la tabella vale esatta.</p>
     */
    private static void recompute() {
        float L = Math.max(configLevel, 5.0F * boostValue());
        active = L > 0.001F;
        if (!active) {
            nightFloor = VANILLA_NIGHT_FLOOR;
            skyGate = 1.0F;
            java.util.Arrays.fill(crushTable, 1.0F);
            return;
        }

        float strength = Math.min(L, 1.0F);
        float t = Mth.clamp(L, 1.0F, 5.0F) - 1.0F; // 0..4 sulla tabella
        int idx = Math.min((int) t, 3);
        float frac = t - idx;

        float exp = Mth.lerp(frac, CRUSH_EXP[idx], CRUSH_EXP[idx + 1]);
        float skyNight = Mth.lerp(frac, SKY_NIGHT[idx], SKY_NIGHT[idx + 1]);
        float moonBonus = Mth.lerp(frac, MOON_BONUS[idx], MOON_BONUS[idx + 1]);

        // Asse caverna: lookup table, luce 15 resta 1.0 per costruzione.
        for (int i = 0; i < 16; i++) {
            float crush = (float) Math.pow(i / 15.0F, exp);
            crushTable[i] = Mth.lerp(strength, 1.0F, crush);
        }

        // Asse notte: floor notturno = 0.2 * (base + bonus luna * fase).
        var mc = Minecraft.getInstance();
        float moon = (moonMatters && mc != null && mc.level != null) ? mc.level.getMoonBrightness() : 0.0F;
        float m = Mth.clamp(skyNight + moonBonus * moon, 0.0F, 1.0F);
        nightFloor = VANILLA_NIGHT_FLOOR * Mth.lerp(strength, 1.0F, m);

        // Cache per l'Hook 3: dimensione corrente e visibilità cielo di ADESSO.
        if (mc != null && mc.level != null) {
            affectsCurrentDim = affectsDimension(mc.level.dimensionType());
            if (mc.level.getSkyFlashTime() > 0) {
                // Fulmine: vanilla porta il fattore cielo a 1.0 di colpo — il
                // lampo deve restare visibile anche nella notte più nera.
                skyGate = 1.0F;
            } else {
                // getSkyDarken qui passa dal nostro mixin, quindi è già rimappato:
                // si ricava il rapporto rimappato/vanilla per sapere quanto cielo
                // "resta" in questo istante (1.0 a mezzogiorno, m' a mezzanotte).
                float remapped = mc.level.getSkyDarken(1.0F);
                float dayness = Mth.clamp((remapped - nightFloor) / Math.max(1.0F - nightFloor, 1.0E-4F), 0.0F, 1.0F);
                float vanilla = dayness * (1.0F - VANILLA_NIGHT_FLOOR) + VANILLA_NIGHT_FLOOR;
                skyGate = Mth.clamp(remapped / vanilla, 0.0F, 1.0F);
            }
        } else {
            affectsCurrentDim = false;
            skyGate = 1.0F;
        }
    }
}
