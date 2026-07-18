package net.saturnx.pitchblackdarkness;

import com.mojang.logging.LogUtils;
import net.saturnx.pitchblackdarkness.platform.PbdPlatform;
import org.slf4j.Logger;

/**
 * Punto d'ingresso condiviso di <b>PBD - Pitch Black Darkness</b>: costanti,
 * logger e l'aggancio alla piattaforma.
 *
 * <p>Questo modulo ({@code common}) è compilato contro <b>vanilla puro</b>, senza
 * nessuna API di loader: è ciò che permette ai tre mixin e a tutta la matematica
 * di {@link net.saturnx.pitchblackdarkness.client.PbdState} di essere UN solo
 * sorgente valido sia per NeoForge sia per Fabric.</p>
 *
 * <p>Le poche cose che i loader fanno in modo diverso (config, tick, comando,
 * schermata) stanno dietro {@link PbdPlatform}, che il modulo del loader inietta
 * con {@link #init(PbdPlatform)} al proprio avvio. Iniezione esplicita e non
 * {@code ServiceLoader}: un passaggio in meno e nessuna dipendenza dal
 * classloader, che su due loader diversi si comporta in modo diverso.</p>
 */
public final class Pbd {
    public static final String MOD_ID = "pitchblackdarkness";

    public static final Logger LOGGER = LogUtils.getLogger();

    private static PbdPlatform platform;

    private Pbd() {}

    /** Chiamato UNA volta dal modulo del loader, il prima possibile al suo avvio. */
    public static void init(PbdPlatform impl) {
        platform = impl;
        LOGGER.info("PBD - Pitch Black Darkness: pronto. Il buio adesso fa sul serio.");
    }

    /**
     * La piattaforma attiva. Non è mai null dopo {@link #init}: se lo fosse
     * significa che un mixin è scattato prima dell'avvio del loader, e vale la
     * pena accorgersene subito invece di leggere silenziosamente valori sbagliati.
     */
    public static PbdPlatform platform() {
        PbdPlatform p = platform;
        if (p == null) {
            throw new IllegalStateException("PbdPlatform non inizializzata: Pbd.init() non è stata chiamata");
        }
        return p;
    }

    /** True se la piattaforma è già stata iniettata (i mixin lo usano per uscire presto). */
    public static boolean ready() {
        return platform != null;
    }
}
