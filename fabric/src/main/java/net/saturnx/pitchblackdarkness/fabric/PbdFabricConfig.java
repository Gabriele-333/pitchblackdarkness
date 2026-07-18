package net.saturnx.pitchblackdarkness.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.saturnx.pitchblackdarkness.Pbd;
import net.saturnx.pitchblackdarkness.platform.PbdPlatform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Config client su Fabric. NeoForge ha {@code ModConfigSpec} built-in; Fabric non
 * ha nessun sistema di config, e tirarsi dentro una libreria per <b>quattro</b>
 * opzioni sarebbe sproporzionato — soprattutto per una mod che si vende come
 * leggera e senza dipendenze. Quindi: un file {@code .properties}, che è
 * leggibile a mano come il TOML di NeoForge.
 *
 * <p>Scrive solo quando un valore cambia davvero ({@link #setDarknessLevel}):
 * il comando {@code /pbd} è l'unico che tocca la config a runtime.</p>
 */
public final class PbdFabricConfig implements PbdPlatform {
    private static final String FILE_NAME = Pbd.MOD_ID + ".properties";

    private static final String KEY_LEVEL = "darknessLevel";
    private static final String KEY_MOON = "moonMatters";
    private static final String KEY_NETHER = "affectNether";
    private static final String KEY_END = "affectEnd";

    private static final String HEADER = """
            PBD - Pitch Black Darkness

            darknessLevel: how dark is dark. 0 = vanilla, 1 = mild, 3 = dark nights and
              black caves, 5 = absolute pitch black (moonless nights included). Daylight is
              never touched. Fractions work too: 0.5 keeps a hint of visibility even in
              unlit caves. In-game command: /pbd <0-5>
            moonMatters: if true, the moon phase matters -- full moon nights stay somewhat
              visible, new moon nights get the full darkness of your level.
            affectNether / affectEnd: apply darkness there too (cave axis only: no sky light).""";

    private final Path file;

    private double darknessLevel = 3.0;
    private boolean moonMatters = true;
    private boolean affectNether = false;
    private boolean affectEnd = false;

    public PbdFabricConfig() {
        this.file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        load();
    }

    // ===== PbdPlatform =====

    @Override
    public double darknessLevel() {
        return darknessLevel;
    }

    @Override
    public void setDarknessLevel(double level) {
        darknessLevel = level;
        save();
    }

    @Override
    public boolean moonMatters() {
        return moonMatters;
    }

    @Override
    public boolean affectNether() {
        return affectNether;
    }

    @Override
    public boolean affectEnd() {
        return affectEnd;
    }

    // ===== File =====

    /** Legge il file se c'è, altrimenti lo crea con i default. Su errore: default, mai crash. */
    private void load() {
        if (!Files.exists(file)) {
            save();
            return;
        }
        var props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            Pbd.LOGGER.warn("PBD: config illeggibile ({}), uso i default", file, e);
            return;
        }
        darknessLevel = clamp(parseDouble(props.getProperty(KEY_LEVEL), darknessLevel));
        moonMatters = parseBoolean(props.getProperty(KEY_MOON), moonMatters);
        affectNether = parseBoolean(props.getProperty(KEY_NETHER), affectNether);
        affectEnd = parseBoolean(props.getProperty(KEY_END), affectEnd);
    }

    private void save() {
        var props = new Properties();
        props.setProperty(KEY_LEVEL, String.valueOf(darknessLevel));
        props.setProperty(KEY_MOON, String.valueOf(moonMatters));
        props.setProperty(KEY_NETHER, String.valueOf(affectNether));
        props.setProperty(KEY_END, String.valueOf(affectEnd));
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, HEADER);
            }
        } catch (IOException e) {
            // Non è fatale: la mod continua col valore in memoria.
            Pbd.LOGGER.warn("PBD: non riesco a salvare la config in {}", file, e);
        }
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(5.0, v));
    }

    private static double parseDouble(String raw, double fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean parseBoolean(String raw, boolean fallback) {
        return raw == null ? fallback : Boolean.parseBoolean(raw.trim());
    }
}
