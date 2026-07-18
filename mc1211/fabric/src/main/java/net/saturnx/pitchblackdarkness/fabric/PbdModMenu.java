package net.saturnx.pitchblackdarkness.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.saturnx.pitchblackdarkness.client.PbdConfigScreen;

/**
 * Aggancia la schermata di config al pulsante di ModMenu.
 *
 * <p><b>Su Fabric ModMenu è una dipendenza obbligatoria</b> ({@code depends} nel
 * {@code fabric.mod.json}): è l'unico modo di raggiungere la schermata di
 * config, che su NeoForge è invece offerta dal loader stesso. Senza, al
 * giocatore resterebbero solo {@code /pbd} e il file {@code .properties}.</p>
 *
 * <p>La schermata sta in {@code common} ed è vanilla puro: vedi
 * {@link PbdConfigScreen} per il perché non si usa Cloth Config.</p>
 */
public final class PbdModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PbdConfigScreen::new;
    }
}
