package net.saturnx.pitchblackdarkness;

import com.mojang.logging.LogUtils;
import net.saturnx.pitchblackdarkness.client.PbdState;
import net.saturnx.pitchblackdarkness.core.PbdLevels;
import net.saturnx.pitchblackdarkness.platform.PbdPlatform;
import org.slf4j.Logger;

/**
 * Avvio condiviso di <b>PBD - Pitch Black Darkness</b> per Minecraft 26.2.
 *
 * <p>Gemello di quello di {@code mc1211}: stessa forma, stesso contratto con i
 * moduli di loader, così i moduli {@code neoforge}/{@code fabric} delle due
 * versioni restano quasi identici. A cambiare è solo cosa {@link PbdState}
 * installa nel nucleo — su 26.2 il floor notturno vale 0.24 invece di 0.2 e la
 * fase lunare si legge dagli attributi d'ambiente.</p>
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
