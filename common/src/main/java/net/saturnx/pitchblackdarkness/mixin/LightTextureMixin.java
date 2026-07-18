package net.saturnx.pitchblackdarkness.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.dimension.DimensionType;
import net.saturnx.pitchblackdarkness.client.PbdState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hook 1 — <b>asse caverna</b>. Stessa impostazione validata in gioco su V112
 * (2026-07-17): iniezione al RETURN di {@code getBrightness}, moltiplicazione
 * dell'uscita per la curva. Clean-room: il codice è nostro, l'idea è collaudata.
 *
 * <p><b>Perché un mixin</b>: NeoForge 21.1 non espone alcun evento sulla lightmap
 * (verificato in V112: esistono solo {@code ViewportEvent.*}). E senza toccare la
 * lightmap qualsiasi buio è finto: basta alzare lo slider Luminosità.</p>
 *
 * <p><b>Perché batte lo slider</b>: la gamma vanilla interpola verso
 * {@code notGamma(x)}, e {@code notGamma(0) == 0}. Se la base è zero, nessuna
 * gamma la riporta su. Per questo la curva non lascia MAI un floor: a livello 5,
 * luce 0 = 0.0 esatto.</p>
 *
 * <p><b>Compatibilità</b> (ereditata dalla verifica V112): ✅ Sodium (consuma la
 * texture della lightmap, non sostituisce {@code LightTexture}); ❌ shader pack
 * Iris/OptiFine (pipeline propria, ignorano la lightmap — nessuna API cross-pack,
 * nemmeno la Darkness del Warden di Mojang ci riesce). Limite accettato e
 * documentato nella config e nella pagina della mod.</p>
 */
@Mixin(LightTexture.class)
public class LightTextureMixin {
    @Inject(method = "getBrightness", at = @At("RETURN"), cancellable = true)
    private static void pbd$crushCaveAxis(DimensionType dimensionType, int lightLevel,
                                          CallbackInfoReturnable<Float> cir) {
        if (!PbdState.active()) {
            return; // livello 0 e niente boost: vanilla intatto, costo un branch
        }
        if (!PbdState.affectsDimension(dimensionType)) {
            return;
        }
        cir.setReturnValue(cir.getReturnValueF() * PbdState.crushFactor(lightLevel));
    }

    /**
     * Hook 3 — <b>l'ammazza-floor</b>, post-gamma. Scoperto sul decompilato
     * (non era nella ricetta ereditata): dopo la gamma, {@code updateLightTexture}
     * fa due {@code lerp(0.75, 0.04)} e un {@code f*0.95+0.05} sul cielo — quindi
     * anche con la rampa a zero il nero risale a ~RGB 36 con lo slider al massimo,
     * e {@code notGamma(0)==0} non basta perché la base NON è più zero quando la
     * gamma viene applicata.
     *
     * <p>Rimedio: sul colore FINALE del texel (dopo gamma e floor) si moltiplica
     * un gate = {@code min(1, visibilitàTarget/0.1)}. Dove il texel deve
     * legittimamente vedersi (giorno, torce: visibilità ≥ 10%) il gate vale 1 e
     * il colore resta <b>bit-identico a vanilla</b>; dove il target è nero il
     * gate è 0 e il floor muore. Applicato dopo la gamma, nessuno slider può
     * annullarlo: è questo che rende il livello 5 davvero a prova di Splendente.</p>
     *
     * <p>{@code @WrapOperation} (MixinExtras, incluso in NeoForge) invece di
     * {@code @Redirect}: si compone con altre mod sullo stesso call-site.</p>
     */
    @WrapOperation(method = "updateLightTexture",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/NativeImage;setPixelRGBA(III)V"))
    private void pbd$killLightmapFloor(NativeImage image, int blockLevel, int skyLevel, int color,
                                       Operation<Void> original) {
        if (PbdState.active() && PbdState.affectsCurrentDimension()) {
            color = PbdState.gateLightmapColor(color, blockLevel, skyLevel);
        }
        original.call(image, blockLevel, skyLevel, color);
    }
}
