package net.saturnx.pitchblackdarkness.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.saturnx.pitchblackdarkness.client.PbdConfigScreen;

/**
 * Aggancia la schermata di config al pulsante di ModMenu.
 *
 * <p><b>ModMenu è opzionale</b>: è dichiarato in {@code suggests}, non in
 * {@code depends}, e in compilazione entra come {@code modCompileOnly}. Se il
 * giocatore non ce l'ha installato, Fabric non interroga mai l'entrypoint
 * {@code modmenu} e questa classe non viene nemmeno caricata — quindi nessun
 * {@code NoClassDefFoundError}, e la mod resta senza dipendenze obbligatorie.
 * Senza ModMenu la config si cambia con {@code /pbd} o a mano nel file.</p>
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
