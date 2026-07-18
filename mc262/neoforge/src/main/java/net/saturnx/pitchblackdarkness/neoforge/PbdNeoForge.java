package net.saturnx.pitchblackdarkness.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.saturnx.pitchblackdarkness.Pbd;

/**
 * Entry point NeoForge di <b>PBD - Pitch Black Darkness</b>.
 *
 * <p>Qui non c'è logica della mod: tutto il comportamento vive in {@code common}.
 * Questo modulo fa solo tre cose — inietta la piattaforma, registra la config e
 * appende la schermata al menu Mods. Tick e comando stanno in
 * {@link PbdNeoForgeClient}.</p>
 *
 * <p><b>Client-only per costruzione</b>: {@code dist = Dist.CLIENT} fa sì che su
 * un server dedicato questa classe non venga nemmeno istanziata — la mod è un
 * no-op totale lato server. La lightmap è client, non c'è niente da fare di là.</p>
 */
@Mod(value = Pbd.MOD_ID, dist = Dist.CLIENT)
public final class PbdNeoForge {
    public PbdNeoForge(ModContainer modContainer) {
        // Prima di registrare la config: l'evento di load scatta subito dopo e
        // porta a PbdState.refresh(), che legge la piattaforma.
        Pbd.init(PbdNeoForgeConfig.INSTANCE);

        modContainer.registerConfig(ModConfig.Type.CLIENT, PbdNeoForgeConfig.SPEC);

        // Schermata config dal menu Mods: la ConfigurationScreen built-in di
        // NeoForge 21.1 basta e avanza per quattro opzioni.
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
