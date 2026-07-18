package net.saturnx.pitchblackdarkness.client;

import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.saturnx.pitchblackdarkness.core.PbdCommands;
import net.saturnx.pitchblackdarkness.core.PbdLevels;
import net.saturnx.pitchblackdarkness.platform.PbdPlatform;

import java.util.function.Consumer;

/**
 * Le opzioni della mod come voci delle <b>Impostazioni Video</b>, accanto allo
 * slider Luminosità.
 *
 * <p>È il posto giusto per due ragioni. Concettualmente il buio è
 * un'impostazione di resa video, non una preferenza di gameplay; e in pratica il
 * giocatore che va a cercare "Luminosità" perché non ci vede è esattamente il
 * giocatore che deve trovare queste. Vanilla mette {@code gamma()} in quella
 * schermata: ci mettiamo accanto le nostre.</p>
 *
 * <p><b>Perché tutte e quattro e non solo il livello</b>: essendo un mixin su una
 * schermata vanilla, questa è l'unica interfaccia che funziona su <i>ogni</i>
 * loader senza dipendenze. Tenerci solo il livello avrebbe lasciato le tre
 * booleane raggiungibili solo editando il file su Fabric, e in una schermata
 * diversa su NeoForge. Così invece la config è identica ovunque, e la mod non
 * chiede al giocatore di installare nulla.</p>
 *
 * <p>Lo slider usa lo stesso stampo di vanilla per {@code gamma}:
 * {@link OptionInstance.UnitDouble} è 0–1, e come gamma (che mostra 0–100) noi
 * mostriamo il valore vero 0–5. Il testo lo produce {@link PbdCommands#describe},
 * lo stesso usato da {@code /pbd}, così il livello si legge identico ovunque.</p>
 */
public final class PbdVideoOption {
    private static final String KEY = "pitchblackdarkness.configuration.";

    private PbdVideoOption() {}

    /** Le quattro voci, nell'ordine in cui compaiono. */
    public static OptionInstance<?>[] createAll() {
        return new OptionInstance<?>[]{
                level(),
                toggle("moonMatters", PbdLevels.platform().moonMatters(), PbdPlatform::setMoonMatters),
                toggle("affectNether", PbdLevels.platform().affectNether(), PbdPlatform::setAffectNether),
                toggle("affectEnd", PbdLevels.platform().affectEnd(), PbdPlatform::setAffectEnd)
        };
    }

    private static OptionInstance<Double> level() {
        String key = KEY + "darknessLevel";
        return new OptionInstance<Double>(
                key,
                OptionInstance.cachedConstantTooltip(Component.translatable(key + ".tooltip")),
                (caption, value) -> CommonComponents.optionNameValue(
                        caption, Component.literal(PbdCommands.describe(toLevel(value)))),
                OptionInstance.UnitDouble.INSTANCE,
                PbdLevels.level() / PbdLevels.MAX_LEVEL,
                // Solo anteprima: il salvataggio è ritardato dal nucleo, perché
                // qui il callback scatta a ogni pixel di trascinamento e
                // OptionInstance non espone alcun evento di rilascio.
                value -> PbdState.previewLevel(toLevel(value)));
    }

    /**
     * Le booleane invece si salvano subito: non c'è nessun trascinamento, il
     * valore cambia una volta sola per click.
     */
    private static OptionInstance<Boolean> toggle(String name, boolean initial,
                                                  BooleanSetter setter) {
        String key = KEY + name;
        return OptionInstance.createBoolean(
                key,
                OptionInstance.cachedConstantTooltip(Component.translatable(key + ".tooltip")),
                initial,
                value -> {
                    setter.set(PbdLevels.platform(), value);
                    PbdState.refresh();
                });
    }

    private static float toLevel(double unit) {
        return (float) (unit * PbdLevels.MAX_LEVEL);
    }

    /** Solo per poter passare i setter della piattaforma come method reference. */
    @FunctionalInterface
    private interface BooleanSetter {
        void set(PbdPlatform platform, boolean value);
    }
}
