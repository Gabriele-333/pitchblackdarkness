package net.saturnx.pitchblackdarkness.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.saturnx.pitchblackdarkness.client.PbdState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hook 2 — <b>asse notte</b>, la parte nuova rispetto a V112 (ed era la feature
 * identitaria di True Darkness: le notti senza luna nere).
 *
 * <p>Dal decompilato 1.21.1 (verificato, non a memoria): {@code getSkyDarken(float)}
 * è definito su <b>ClientLevel</b> (quindi niente rischio di toccare l'integrated
 * server) e restituisce {@code f1 * 0.8F + 0.2F} — ecco perché la notte vanilla si
 * vede sempre: il fattore cielo non scende mai sotto 0.2, e la rampa dei livelli
 * di luce non c'entra niente (sky-light 15 resta 15 anche a mezzanotte).</p>
 *
 * <p>La rimappatura di {@link PbdState#remapSkyDarken(float)} abbassa quel floor
 * secondo livello e fase lunare, lasciando il giorno (1.0) come punto fisso e
 * l'interpolazione lineare in mezzo: alba e tramonto restano morbidi per
 * costruzione.</p>
 *
 * <p><b>Chi altro lo chiama</b> (verificato sul decompilato con grep): solo
 * {@code LightTexture.updateLightTexture}. Cielo e nebbia usano altre curve →
 * le stelle e la luna restano visibili sopra un terreno nero. Scelta consapevole:
 * è così anche in una vera notte senza luna.</p>
 */
@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    @Inject(method = "getSkyDarken(F)F", at = @At("RETURN"), cancellable = true)
    private void pbd$darkenNightSky(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (!PbdState.active()) {
            return;
        }
        if (!PbdState.affectsDimension(((ClientLevel) (Object) this).dimensionType())) {
            return;
        }
        cir.setReturnValue(PbdState.remapSkyDarken(cir.getReturnValueF()));
    }
}
