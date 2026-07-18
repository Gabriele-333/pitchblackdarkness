package net.saturnx.pitchblackdarkness.api;

import net.saturnx.pitchblackdarkness.core.PbdLevels;

/**
 * API pubblica di PBD - Pitch Black Darkness. Piccola e stabile di proposito:
 * è il contratto con cui altre mod (in famiglia: V112) pilotano il buio.
 *
 * <p><b>Non importa nessuna classe di Minecraft</b>, ed è una proprietà da
 * preservare: è ciò che la rende valida su qualunque loader e qualunque
 * versione del gioco, e quindi una dipendenza sicura da dichiarare.</p>
 *
 * <p><b>Solo client</b>: la lightmap è client, quindi anche questa API. Va
 * chiamata esclusivamente da codice che gira sul physical client; su un server
 * dedicato la mod non fa nulla.</p>
 */
public final class PitchBlackApi {
    private PitchBlackApi() {}

    /**
     * Il livello di buio persistito del giocatore (0 = vanilla … 5 = nero
     * assoluto). La scala è continua: 0.5 e simili sono valori legittimi.
     */
    public static double getLevel() {
        return PbdLevels.level();
    }

    /** Cambia il livello persistito (clamp a 0–5, decimali ok): effetto immediato, salvato in config. */
    public static void setLevel(double level) {
        PbdLevels.setLevel(level);
    }

    /**
     * Spinta <b>temporanea</b> verso il buio massimo, con envelope in/out. Non
     * tocca il livello persistito del giocatore: si compone con esso prendendo
     * il massimo ({@code effettivo = max(livello, boost)}).
     *
     * @param intensity01   picco della spinta, 0–1 (1.0 = livello 5 pieno)
     * @param durationTicks durata totale in tick, dissolvenze incluse
     * @param fadeTicks     tick di dissolvenza in entrata E in uscita
     *                      (clampato a metà durata)
     */
    public static void pushTransientBoost(float intensity01, int durationTicks, int fadeTicks) {
        PbdLevels.pushTransientBoost(intensity01, durationTicks, fadeTicks);
    }

    /** Spegne subito l'eventuale boost transiente (il livello persistito resta). */
    public static void clearTransientBoost() {
        PbdLevels.clearTransientBoost();
    }
}
