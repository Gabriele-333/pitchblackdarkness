package net.saturnx.pitchblackdarkness;

import com.mojang.logging.LogUtils;
import net.saturnx.pitchblackdarkness.client.PbdState;
import net.saturnx.pitchblackdarkness.core.PbdLevels;
import net.saturnx.pitchblackdarkness.platform.PbdPlatform;
import org.slf4j.Logger;

/**
 * Avvio condiviso di <b>PBD - Pitch Black Darkness</b> per Minecraft 1.21.1.
 *
 * <p>Tre livelli, dal generale al particolare: {@code core} è Java puro e non
 * sa nulla né di Minecraft né dei loader; questo modulo sa di Minecraft 1.21.1
 * ma non del loader; i moduli {@code neoforge}/{@code fabric} sanno del loader.
 * È il motivo per cui i tre mixin e la matematica sono scritti una volta sola.</p>
 *
 * <p>Il modulo del loader chiama {@link #init(PbdPlatform)} al proprio avvio,
 * con iniezione esplicita e non {@code ServiceLoader}: un passaggio in meno e
 * nessuna dipendenza dal classloader, che su loader diversi si comporta in modo
 * diverso.</p>
 */
public final class Pbd {
    public static final String MOD_ID = "pitchblackdarkness";

    public static final Logger LOGGER = LogUtils.getLogger();

    private Pbd() {}

    /** Chiamato UNA volta dal modulo del loader, il prima possibile al suo avvio. */
    public static void init(PbdPlatform platform) {
        PbdLevels.setPlatform(platform);
        // Aggancia al nucleo le letture specifiche di questa versione di MC.
        PbdState.install();
        LOGGER.info("PBD - Pitch Black Darkness: pronto. Il buio adesso fa sul serio.");
    }
}
