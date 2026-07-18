package net.saturnx.pitchblackdarkness;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.saturnx.pitchblackdarkness.config.PbdConfig;
import org.slf4j.Logger;

/**
 * Entry point di <b>PBD - Pitch Black Darkness</b>: il buio di Minecraft reso vero.
 *
 * <p>Un solo numero per il giocatore, il livello 0–5: 0 = vanilla, 5 = nero
 * assoluto (caverne a luce 0 E notti senza luna). Il giorno resta intatto a ogni
 * livello. Contesto completo in {@code CLAUDE.md}; stato lavori in {@code TODO.md}.</p>
 *
 * <p><b>Client-only per costruzione</b>: {@code dist = Dist.CLIENT} fa sì che su
 * un server dedicato questa classe non venga nemmeno istanziata — la mod è un
 * no-op totale lato server. La lightmap è client, non c'è niente da fare di là.</p>
 */
@Mod(value = PitchBlackDarkness.MOD_ID, dist = Dist.CLIENT)
public final class PitchBlackDarkness {
    public static final String MOD_ID = "pitchblackdarkness";

    public static final Logger LOGGER = LogUtils.getLogger();

    public PitchBlackDarkness(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, PbdConfig.SPEC);

        // Schermata config dal menu Mods: la ConfigurationScreen built-in di
        // NeoForge 21.1 basta e avanza per quattro opzioni.
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        LOGGER.info("PBD - Pitch Black Darkness: pronto. Il buio adesso fa sul serio.");
    }
}
