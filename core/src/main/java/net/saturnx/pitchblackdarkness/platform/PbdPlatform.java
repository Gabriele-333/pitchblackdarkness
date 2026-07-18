package net.saturnx.pitchblackdarkness.platform;

/**
 * Tutto ciò che NeoForge e Fabric fanno in modo diverso, e nient'altro.
 *
 * <p>La superficie è deliberatamente minuscola: è solo <b>lettura e scrittura
 * della config</b>. Tick, registrazione del comando e schermata di config non
 * stanno qui perché non hanno bisogno di essere astratti — è il modulo del
 * loader a registrarli con la propria API e a chiamare dentro {@code common}
 * ({@code PbdState.clientTick()}, {@code PbdCommands.*}).</p>
 *
 * <p>I valori NON vanno letti nei punti caldi del rendering: {@code PbdState}
 * li precalcola in campi piatti a ogni {@code refresh()}.</p>
 */
public interface PbdPlatform {
    /** Il livello 0–5 persistito (decimali ammessi: la curva è continua). */
    double darknessLevel();

    /** Scrive il livello e lo <b>persiste</b> su file. Il chiamante ha già fatto il clamp. */
    void setDarknessLevel(double level);

    /** La fase lunare modula la notte. */
    boolean moonMatters();

    void setMoonMatters(boolean value);

    /** Applica il buio anche nel Nether (solo asse caverna: non ha sky-light). */
    boolean affectNether();

    void setAffectNether(boolean value);

    /** Applica il buio anche nell'End (solo asse caverna: non ha sky-light). */
    boolean affectEnd();

    void setAffectEnd(boolean value);
}
