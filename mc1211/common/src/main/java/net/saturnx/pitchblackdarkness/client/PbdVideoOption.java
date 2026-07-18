package net.saturnx.pitchblackdarkness.client;

import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.saturnx.pitchblackdarkness.core.PbdCommands;
import net.saturnx.pitchblackdarkness.core.PbdLevels;

/**
 * Il livello di buio come voce delle <b>Impostazioni Video</b>, accanto allo
 * slider Luminosità.
 *
 * <p>È il posto giusto per due ragioni. Concettualmente il buio è
 * un'impostazione di resa video, non una preferenza di gameplay; e in pratica il
 * giocatore che va a cercare "Luminosità" perché non ci vede è esattamente il
 * giocatore che deve trovare questa. Vanilla mette {@code gamma()} in quella
 * schermata: ci mettiamo accanto la nostra.</p>
 *
 * <p>Stesso stampo di vanilla per {@code gamma}: {@link OptionInstance.UnitDouble}
 * è uno slider 0–1, e come fa gamma (che mostra 0–100) noi mostriamo il valore
 * vero 0–5 nella caption. Il testo lo produce {@link PbdCommands#describe} —
 * lo stesso usato da {@code /pbd}, così il livello si legge identico ovunque.</p>
 */
public final class PbdVideoOption {
    private static final String KEY = "pitchblackdarkness.configuration.darknessLevel";

    private PbdVideoOption() {}

    public static OptionInstance<Double> create() {
        return new OptionInstance<Double>(
                KEY,
                OptionInstance.cachedConstantTooltip(Component.translatable(KEY + ".tooltip")),
                (caption, value) -> CommonComponents.optionNameValue(
                        caption, Component.literal(PbdCommands.describe(toLevel(value)))),
                OptionInstance.UnitDouble.INSTANCE,
                PbdLevels.level() / PbdLevels.MAX_LEVEL,
                // Solo anteprima: il salvataggio è ritardato dal nucleo, perché
                // qui il callback scatta a ogni pixel di trascinamento e
                // OptionInstance non espone alcun evento di rilascio.
                value -> PbdState.previewLevel(toLevel(value)));
    }

    private static float toLevel(double unit) {
        return (float) (unit * PbdLevels.MAX_LEVEL);
    }
}
