package net.saturnx.pitchblackdarkness.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.saturnx.pitchblackdarkness.Pbd;

/**
 * Schermata di config, scritta in <b>vanilla puro</b>: nessuna libreria di GUI.
 *
 * <p>Per quattro opzioni, Cloth Config sarebbe una dipendenza obbligatoria in più
 * a carico dell'utente — contro l'identità della mod (leggera, senza dipendenze),
 * e per la stessa ragione per cui su Fabric la config è un {@code .properties}
 * scritto a mano. {@code Screen}, {@code AbstractSliderButton} e
 * {@code CycleButton} sono classi di Minecraft, quindi questa schermata vive in
 * {@code common} e vale su entrambi i loader: oggi la apre ModMenu su Fabric,
 * domani può sostituire anche la {@code ConfigurationScreen} di NeoForge.</p>
 */
public final class PbdConfigScreen extends Screen {
    private static final String KEY = "pitchblackdarkness.configuration.";

    private static final int ROW_HEIGHT = 24;
    private static final int WIDGET_WIDTH = 310;
    private static final int WIDGET_HEIGHT = 20;

    private final Screen parent;

    private LevelSlider slider;

    public PbdConfigScreen(Screen parent) {
        super(Component.translatable(KEY + "title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        var platform = Pbd.platform();
        int x = this.width / 2 - WIDGET_WIDTH / 2;
        int y = this.height / 4;

        slider = addRenderableWidget(new LevelSlider(x, y, platform.darknessLevel()));
        y += ROW_HEIGHT;

        addRenderableWidget(toggle("moonMatters", x, y, platform.moonMatters(),
                (button, value) -> apply(() -> Pbd.platform().setMoonMatters(value))));
        y += ROW_HEIGHT;

        addRenderableWidget(toggle("affectNether", x, y, platform.affectNether(),
                (button, value) -> apply(() -> Pbd.platform().setAffectNether(value))));
        y += ROW_HEIGHT;

        addRenderableWidget(toggle("affectEnd", x, y, platform.affectEnd(),
                (button, value) -> apply(() -> Pbd.platform().setAffectEnd(value))));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 100, this.height - 32, 200, WIDGET_HEIGHT)
                .build());
    }

    private CycleButton<Boolean> toggle(String key, int x, int y, boolean initial,
                                        CycleButton.OnValueChange<Boolean> onChange) {
        return CycleButton.onOffBuilder(initial)
                .withTooltip(value -> Tooltip.create(Component.translatable(KEY + key + ".tooltip")))
                .create(x, y, WIDGET_WIDTH, WIDGET_HEIGHT,
                        Component.translatable(KEY + key), onChange);
    }

    /** Scrive la config e ricalcola subito: l'effetto si vede senza chiudere la schermata. */
    private static void apply(Runnable write) {
        write.run();
        PbdState.refresh();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 4 - 24, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        // Persiste il valore corrente dello slider PRIMA di rileggere la config.
        // Serve per la tastiera: le frecce passano da applyValue() (che è solo
        // anteprima) e non emettono mai onRelease, quindi senza questo il valore
        // scelto da tastiera verrebbe buttato via. Con il mouse è idempotente.
        if (slider != null) {
            PbdState.setLevel(slider.level());
        }
        PbdState.refresh();
        this.minecraft.setScreen(parent);
    }

    /**
     * Slider del livello 0–5. La scala è continua, ma si aggancia a step di 0.1:
     * sotto quella soglia la differenza non si vede e il numero diventa illeggibile.
     */
    private static final class LevelSlider extends AbstractSliderButton {
        private static final double STEP = 0.1;

        private LevelSlider(int x, int y, double level) {
            super(x, y, WIDGET_WIDTH, WIDGET_HEIGHT, Component.empty(),
                    level / PbdCommands.MAX_LEVEL);
            setTooltip(Tooltip.create(Component.translatable(KEY + "darknessLevel.tooltip")));
            updateMessage();
        }

        private double level() {
            double raw = this.value * PbdCommands.MAX_LEVEL;
            double snapped = Math.round(raw / STEP) * STEP;
            return Mth.clamp(snapped, PbdCommands.MIN_LEVEL, PbdCommands.MAX_LEVEL);
        }

        @Override
        protected void updateMessage() {
            double level = level();
            String shown = level == Math.floor(level)
                    ? String.valueOf((int) level)
                    : String.format("%.1f", level);
            setMessage(Component.translatable(KEY + "darknessLevel")
                    .append(Component.literal(": " + shown)));
        }

        /** Trascinamento: solo anteprima, niente scrittura su disco (vedi PbdState.previewLevel). */
        @Override
        protected void applyValue() {
            PbdState.previewLevel(level());
        }

        /** Rilascio: adesso il valore diventa definitivo e viene salvato. */
        @Override
        public void onRelease(double mouseX, double mouseY) {
            PbdState.setLevel(level());
        }
    }
}
