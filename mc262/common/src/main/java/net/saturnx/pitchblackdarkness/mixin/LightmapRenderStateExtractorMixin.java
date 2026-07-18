package net.saturnx.pitchblackdarkness.mixin;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.saturnx.pitchblackdarkness.client.PbdState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * L'<b>unico</b> hook della mod su Minecraft 26.2.
 *
 * <p>Su 1.21.1 servivano tre mixin: la rampa dei livelli, il fattore notturno e
 * un ammazza-floor per battere lo slider Luminosità. Qui basta questo, perché la
 * lightmap è passata sulla GPU e tutti i suoi ingressi transitano da un solo
 * punto: {@code extract()}, che riempie il {@code LightmapRenderState} poi
 * caricato nell'UBO dello shader. Tutti i campi sono pubblici e mutabili, quindi
 * intervenire al RETURN è sufficiente e non richiede di riscrivere il metodo.</p>
 *
 * <p><b>La guardia su {@code needsUpdate} non è un'ottimizzazione, è
 * correttezza</b>: vanilla riscrive i campi solo quando è vero. Se agissimo
 * anche quando è falso, moltiplicheremmo di nuovo valori già nostri e la
 * scalatura si accumulerebbe frame dopo frame, spegnendo progressivamente tutta
 * la luce. Così invece lavoriamo sempre su valori vanilla freschi.</p>
 */
@Mixin(LightmapRenderStateExtractor.class)
public class LightmapRenderStateExtractorMixin {
    @Inject(method = "extract", at = @At("RETURN"))
    private void pbd$applyDarkness(LightmapRenderState state, float partialTicks, CallbackInfo ci) {
        // A livello 0 si esce subito: vanilla resta intatto al 100%.
        if (!state.needsUpdate || !PbdState.active()) {
            return;
        }
        PbdState.apply(state);
    }
}
