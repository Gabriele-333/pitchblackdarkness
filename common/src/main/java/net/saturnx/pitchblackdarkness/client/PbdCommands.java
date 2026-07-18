package net.saturnx.pitchblackdarkness.client;

/**
 * Logica del comando client {@code /pbd <0-5>} (alias {@code /pitchblack}): il
 * livello si cambia in gioco, senza riavvio, e funziona anche sui server altrui
 * perché è un comando puramente client-side.
 *
 * <p><b>Perché qui c'è solo la logica e non l'albero Brigadier</b>: i due loader
 * registrano i comandi client su tipi diversi e incompatibili — NeoForge usa
 * {@code CommandSourceStack} (via {@code RegisterClientCommandsEvent}), Fabric
 * usa {@code FabricClientCommandSource} (via {@code ClientCommandRegistrationCallback}).
 * Genericizzare l'albero su quel tipo per risparmiare una decina di righe non
 * vale il giro. Qui stanno nomi, range e messaggi — tutto ciò che deve restare
 * identico sui due loader — e ogni modulo costruisce il suo alberello.</p>
 */
public final class PbdCommands {
    /** Nome del comando e suo alias: registrali entrambi. */
    public static final String[] NAMES = {"pbd", "pitchblack"};

    /** Nome dell'argomento del livello (uguale sui due loader, compare negli errori). */
    public static final String LEVEL_ARG = "level";

    public static final double MIN_LEVEL = 0.0;
    public static final double MAX_LEVEL = 5.0;

    private static final String[] LEVEL_LABELS = {
            "vanilla", "mild", "dark", "very dark", "black", "pitch black"};

    private PbdCommands() {}

    /** "3 (dark)" per i livelli interi, "0.5" secco per i decimali (curva continua). */
    private static String describe(float level) {
        if (level == Math.floor(level)) {
            int i = (int) level;
            return i + " (" + LEVEL_LABELS[i] + ")";
        }
        return String.valueOf(level);
    }

    /** Messaggio di {@code /pbd} senza argomenti: mostra il livello attuale. */
    public static String showMessage() {
        return "PBD darkness level: " + describe(PbdState.level())
                + " — /pbd <0-5> to change (halves like 0.5 work too)";
    }

    /** Applica il livello e rende il messaggio di conferma. */
    public static String setAndDescribe(double level) {
        PbdState.setLevel(level);
        return "PBD darkness level set to " + describe(PbdState.level());
    }
}
