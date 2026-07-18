package net.saturnx.pitchblackdarkness.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.saturnx.pitchblackdarkness.client.PbdVideoOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Arrays;

/**
 * Appende il livello di buio alle Impostazioni Video, accanto a Luminosità.
 *
 * <p>Vanilla costruisce quella schermata da un {@code OptionInstance<?>[]} reso
 * da un metodo statico, quindi basta allungare l'array: nessuna riscrittura di
 * schermate, nessuna dipendenza da API di loader — è un mixin su codice vanilla
 * e vale identico su NeoForge e Fabric.</p>
 *
 * <p><b>Il nome del metodo cambia con la versione</b>: {@code options} su 1.21.1,
 * {@code displayOptions} su 26.2 (dove le opzioni sono state divise in
 * qualità/schermo/preferenze). Per questo il mixin è per-versione, anche se il
 * corpo è lo stesso.</p>
 */
@Mixin(VideoSettingsScreen.class)
public class VideoSettingsScreenMixin {
    @ModifyReturnValue(method = "options", at = @At("RETURN"))
    private static OptionInstance<?>[] pbd$addDarknessOption(OptionInstance<?>[] original) {
        OptionInstance<?>[] withDarkness = Arrays.copyOf(original, original.length + 1);
        withDarkness[original.length] = PbdVideoOption.create();
        return withDarkness;
    }
}
