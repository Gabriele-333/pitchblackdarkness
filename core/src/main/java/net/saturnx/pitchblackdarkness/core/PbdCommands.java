package net.saturnx.pitchblackdarkness.core;

/**
 * Logica del comando client {@code /pbd <0-5>} (alias {@code /pitchblack}): il
 * livello si cambia in gioco, senza riavvio, e funziona anche sui server altrui
 * perché è un comando puramente client-side.
 *
 * <p><b>Perché qui c'è solo la logica e non l'albero Brigadier</b>: i loader
 * registrano i comandi client su tipi diversi e incompatibili — NeoForge usa
 * {@code CommandSourceStack}, Fabric {@code FabricClientCommandSource} — e per
 * di più quei tipi cambiano tra versioni di Minecraft. Qui stanno nomi, range e
 * messaggi, cioè tutto ciò che deve restare identico ovunque; ogni modulo
 * costruisce il suo alberello.</p>
 */
public final class PbdCommands {
    /** Nome del comando e suo alias: registrali entrambi. */
    public static final String[] NAMES = {"pbd", "pitchblack"};

    /** Nome dell'argomento del livello (uguale ovunque, compare negli errori). */
    public static final String LEVEL_ARG = "level";

    public static final double MIN_LEVEL = PbdLevels.MIN_LEVEL;
    public static final double MAX_LEVEL = PbdLevels.MAX_LEVEL;

    private static final String[] LEVEL_LABELS = {
            "vanilla", "mild", "dark", "very dark", "black", "pitch black"};

    private PbdCommands() {}

    /** "3 (dark)" per i livelli interi, "0.5" secco per i decimali (curva continua). */
    public static String describe(float level) {
        if (level == Math.floor(level)) {
            int i = (int) level;
            return i + " (" + LEVEL_LABELS[i] + ")";
        }
        return String.valueOf(level);
    }

    /** Messaggio di {@code /pbd} senza argomenti: mostra il livello attuale. */
    public static String showMessage() {
        return "PBD darkness level: " + describe(PbdLevels.level())
                + " — /pbd <0-5> to change (halves like 0.5 work too)";
    }

    /** Applica il livello e rende il messaggio di conferma. */
    public static String setAndDescribe(double level) {
        PbdLevels.setLevel(level);
        return "PBD darkness level set to " + describe(PbdLevels.level());
    }
}
