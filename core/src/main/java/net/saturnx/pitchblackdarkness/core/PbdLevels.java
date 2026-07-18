package net.saturnx.pitchblackdarkness.core;

import net.saturnx.pitchblackdarkness.platform.PbdPlatform;

/**
 * Il motore del buio: dal livello 0–5 ai pochi float piatti che i mixin leggono.
 *
 * <p><b>Java puro, zero import di Minecraft — di proposito.</b> È l'unica parte
 * della mod che sopravvive intatta tra versioni di Minecraft: dalla 26.1 in poi
 * la lightmap si calcola sulla GPU, {@code LightTexture} non esiste più e
 * {@code getSkyDarken}/{@code getMoonBrightness} sono spariti, quindi ogni riga
 * che tocca il gioco va riscritta per versione. Questa no. Chi la modifica
 * <b>non deve</b> introdurre import {@code net.minecraft.*}: è ciò che tiene in
 * piedi il multi-versione.</p>
 *
 * <p>Il dato che servirebbe dal gioco — la luminosità lunare — arriva come
 * parametro di {@link #recompute(float)}: è il modulo della versione a
 * procurarselo, con l'API che quella versione espone.</p>
 *
 * <p><b>Contratto di performance</b>: la lightmap gira di continuo, quindi qui
 * non si alloca niente e non si legge la config nei punti caldi. Tutto è
 * precalcolato una volta per tick e i mixin leggono solo campi già pronti.</p>
 */
public final class PbdLevels {
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
    /** Bonus di visibilità con luna piena, scalato dalla fase (1.0 = piena). */
    private static final float[] MOON_BONUS = {0.25F, 0.20F, 0.15F, 0.10F, 0.05F};

    /**
     * Quanto vale il fattore cielo vanilla in piena notte (il giorno vale 1.0).
     * <b>Dipende dalla versione</b>, quindi lo installa il modulo della versione:
     * su 1.21.1 {@code getSkyDarken} rende {@code f1*0.8+0.2} → 0.2; su 26.2 il
     * timeline {@code day.json} interpola {@code visual/sky_light_factor} da 1.0
     * a <b>0.24</b>. È l'unico numero che cambia: la rimappatura è identica.
     */
    private static float vanillaNightFloor = 0.2F;

    public static float vanillaNightFloor() {
        return vanillaNightFloor;
    }

    /** Installato dal modulo della versione al proprio avvio. */
    public static void setVanillaNightFloor(float value) {
        vanillaNightFloor = value;
        nightFloor = value;
    }

    /**
     * Soglia del gate ammazza-floor: i texel con visibilità target sotto il 10%
     * vengono spenti in proporzione, quelli sopra restano bit-identici a vanilla.
     * 1/0.1 precalcolato.
     */
    private static final float GATE_INV_THRESHOLD = 10.0F;

    public static final double MIN_LEVEL = 0.0;
    public static final double MAX_LEVEL = 5.0;

    // ===== Output precalcolati: gli UNICI campi letti dai mixin =====
    private static boolean active;
    private static final float[] crushTable = new float[16];
    private static float nightFloor = vanillaNightFloor;
    /** Visibilità del contributo cielo ADESSO: 1.0 di giorno, m' in piena notte. */
    private static float skyGate = 1.0F;

    // ===== Config cache (aggiornata SOLO da refresh(): mai .get() nel tick) =====
    private static float configLevel;
    private static boolean moonMatters = true;

    // ===== Boost transiente (API per V112): envelope in/out =====
    private static int boostTicksLeft;
    private static int boostTotalTicks;
    private static int boostFadeTicks;
    private static float boostPeak;

    private static PbdPlatform platform;

    /**
     * Come il nucleo si procura la fase lunare senza conoscere Minecraft: il
     * modulo della versione inietta qui la lettura giusta per la sua versione
     * ({@code level.getMoonBrightness()} su 1.21.1 — metodo che dalla 26.x non
     * esiste più e va ricavato altrimenti).
     */
    @FunctionalInterface
    public interface MoonSource {
        /** 0–1, dove 1 = luna piena. 0 se non c'è un mondo caricato. */
        float moonBrightness();
    }

    private static MoonSource moonSource = () -> 0.0F;

    static {
        java.util.Arrays.fill(crushTable, 1.0F);
    }

    private PbdLevels() {}

    /** Iniettata dal modulo del loader al proprio avvio. */
    public static void setPlatform(PbdPlatform impl) {
        platform = impl;
    }

    /** Iniettato dal modulo della versione al proprio avvio. */
    public static void setMoonSource(MoonSource source) {
        moonSource = source;
    }

    private static float moon() {
        return moonSource.moonBrightness();
    }

    // ===== Comode senza parametro: usano il MoonSource iniettato =====

    public static void refresh() {
        refresh(moon());
    }

    public static void recompute() {
        recompute(moon());
    }

    public static void setLevel(double level) {
        setLevel(level, moon());
    }

    public static void previewLevel(double level) {
        previewLevel(level, moon());
    }

    public static void pushTransientBoost(float intensity01, int durationTicks, int fadeTicks) {
        pushTransientBoost(intensity01, durationTicks, fadeTicks, moon());
    }

    public static void clearTransientBoost() {
        clearTransientBoost(moon());
    }

    public static PbdPlatform platform() {
        PbdPlatform p = platform;
        if (p == null) {
            throw new IllegalStateException("PbdPlatform non inizializzata: Pbd.init() non è stata chiamata");
        }
        return p;
    }

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

    public static float nightFloor() {
        return nightFloor;
    }

    public static float skyGate() {
        return skyGate;
    }

    public static void setSkyGate(float value) {
        skyGate = clamp(value, 0.0F, 1.0F);
    }

    /**
     * Rimappa il valore di ritorno del fattore notturno vanilla: il floor
     * notturno (0.2) scende a {@link #nightFloor}, il giorno (1.0) resta
     * esattamente 1.0, e in mezzo è la stessa interpolazione lineare di vanilla
     * — quindi alba e tramonto restano morbidi per costruzione.
     */
    public static float remapSkyDarken(float vanilla) {
        float dayness = clamp((vanilla - vanillaNightFloor) / (1.0F - vanillaNightFloor), 0.0F, 1.0F);
        return dayness * (1.0F - nightFloor) + nightFloor;
    }

    /**
     * Gate ammazza-floor sul colore FINALE del texel della lightmap (post-gamma
     * — è ciò che rende il nero a prova di slider). Sotto il 10% di visibilità
     * target il colore viene spento in proporzione, sopra resta bit-identico.
     *
     * <p>Formato colore: ABGR packed, come lo scrive updateLightTexture
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
    public static void setLevel(double level, float moonBrightness) {
        platform().setDarknessLevel(clamp(level, MIN_LEVEL, MAX_LEVEL));
        refresh(moonBrightness);
    }

    /**
     * Anteprima dal vivo: cambia l'effetto SENZA scrivere su file né toccare la
     * piattaforma. La usa lo slider della schermata di config mentre lo trascini
     * — persistere a ogni pixel significherebbe una scrittura su disco per pixel.
     */
    public static void previewLevel(double level, float moonBrightness) {
        configLevel = (float) clamp(level, MIN_LEVEL, MAX_LEVEL);
        persistPending = true;
        persistCountdown = PERSIST_DELAY_TICKS;
        recompute(moonBrightness);
    }

    /**
     * Spinta temporanea verso il buio massimo, con dissolvenza in entrata e in
     * uscita. NON tocca il livello persistito: si compone con esso prendendo il
     * massimo. intensity01 = 1.0 equivale a livello 5 pieno.
     */
    public static void pushTransientBoost(float intensity01, int durationTicks, int fadeTicks, float moonBrightness) {
        if (durationTicks <= 0 || intensity01 <= 0.0F) {
            return;
        }
        boostTotalTicks = durationTicks;
        boostTicksLeft = durationTicks;
        boostFadeTicks = Math.max(1, Math.min(fadeTicks, durationTicks / 2));
        boostPeak = clamp(intensity01, 0.0F, 1.0F);
        recompute(moonBrightness);
    }

    /** Spegne il boost transiente all'istante (il livello persistito resta). */
    public static void clearTransientBoost(float moonBrightness) {
        boostTicksLeft = 0;
        boostTotalTicks = 0;
        boostPeak = 0.0F;
        recompute(moonBrightness);
    }

    // ===== Persistenza ritardata =====
    //
    // Gli slider (schermata di config e Impostazioni Video) notificano il nuovo
    // valore a OGNI pixel di trascinamento, e nessuno dei due espone un evento
    // di "rilascio" affidabile: OptionInstance non ce l'ha proprio, e da tastiera
    // non arriva comunque. Scrivere lì significherebbe centinaia di scritture su
    // disco per una singola trascinata. Quindi l'anteprima è immediata (si vede
    // il buio cambiare mentre trascini, che per questa mod è desiderabile) e il
    // salvataggio parte quando il valore sta fermo da un po'.

    /** Mezzo secondo: abbastanza da non scrivere durante la trascinata, poco da non perdersi. */
    private static final int PERSIST_DELAY_TICKS = 10;

    private static boolean persistPending;
    private static int persistCountdown;

    /** Scala i countdown di un tick. Da chiamare dal tick del loader. */
    public static void tickBoost() {
        if (boostTicksLeft > 0) {
            boostTicksLeft--;
        }
        if (persistPending && --persistCountdown <= 0) {
            persistPending = false;
            platform().setDarknessLevel(configLevel);
        }
    }

    /**
     * Forza subito su disco un'eventuale anteprima in sospeso. Da chiamare quando
     * si chiude una schermata: senza, uscire dal gioco entro mezzo secondo dalla
     * modifica la perderebbe.
     */
    public static void flushPending() {
        if (persistPending) {
            persistPending = false;
            platform().setDarknessLevel(configLevel);
        }
    }

    /** Rilegge la config e ricalcola tutto. Chiamata a ogni load/reload/setLevel. */
    public static void refresh(float moonBrightness) {
        PbdPlatform p = platform();
        configLevel = (float) p.darknessLevel();
        moonMatters = p.moonMatters();
        recompute(moonBrightness);
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
     * Dal livello effettivo (config + boost, max dei due) ai valori piatti.
     *
     * <p>Il livello effettivo L vive in [0,5]. La forza s = min(L,1) fa da
     * dissolvenza globale (per L sotto 1, es. durante il fade del boost,
     * l'effetto sfuma verso vanilla); da 1 in su i parametri si interpolano
     * linearmente sulla tabella, quindi ai livelli interi la tabella vale esatta.</p>
     *
     * @param moonBrightness 0–1 dal gioco (1 = luna piena); ignorato se moonMatters è off
     */
    public static void recompute(float moonBrightness) {
        float L = Math.max(configLevel, 5.0F * boostValue());
        active = L > 0.001F;
        if (!active) {
            nightFloor = vanillaNightFloor;
            skyGate = 1.0F;
            java.util.Arrays.fill(crushTable, 1.0F);
            return;
        }

        float strength = Math.min(L, 1.0F);
        float t = clamp(L, 1.0F, 5.0F) - 1.0F; // 0..4 sulla tabella
        int idx = Math.min((int) t, 3);
        float frac = t - idx;

        float exp = lerp(frac, CRUSH_EXP[idx], CRUSH_EXP[idx + 1]);
        float skyNight = lerp(frac, SKY_NIGHT[idx], SKY_NIGHT[idx + 1]);
        float moonBonus = lerp(frac, MOON_BONUS[idx], MOON_BONUS[idx + 1]);

        // Asse caverna: lookup table, luce 15 resta 1.0 per costruzione.
        for (int i = 0; i < 16; i++) {
            float crush = (float) Math.pow(i / 15.0F, exp);
            crushTable[i] = lerp(strength, 1.0F, crush);
        }

        // Asse notte: floor notturno = 0.2 * (base + bonus luna * fase).
        float moon = moonMatters ? clamp(moonBrightness, 0.0F, 1.0F) : 0.0F;
        float m = clamp(skyNight + moonBonus * moon, 0.0F, 1.0F);
        nightFloor = vanillaNightFloor * lerp(strength, 1.0F, m);
    }

    // ===== Piccole utility: qui non c'è Mth, siamo fuori da Minecraft =====

    public static float lerp(float delta, float from, float to) {
        return from + delta * (to - from);
    }

    public static float clamp(float v, float min, float max) {
        return v < min ? min : Math.min(v, max);
    }

    public static double clamp(double v, double min, double max) {
        return v < min ? min : Math.min(v, max);
    }
}
