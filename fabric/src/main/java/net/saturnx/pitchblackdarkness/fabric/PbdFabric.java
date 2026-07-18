package net.saturnx.pitchblackdarkness.fabric;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.network.chat.Component;
import net.saturnx.pitchblackdarkness.Pbd;
import net.saturnx.pitchblackdarkness.client.PbdCommands;
import net.saturnx.pitchblackdarkness.client.PbdState;

/**
 * Entry point Fabric di <b>PBD - Pitch Black Darkness</b>.
 *
 * <p>Qui non c'è logica della mod: tutto il comportamento vive in {@code common}.
 * Questo modulo inietta la piattaforma (config su file properties) e aggancia
 * tick e comando alle API di Fabric.</p>
 *
 * <p><b>Client-only</b>: dichiarato {@code "environment": "client"} in
 * {@code fabric.mod.json}, quindi su un server dedicato la mod non viene proprio
 * caricata. La lightmap è client, non c'è niente da fare di là.</p>
 */
public final class PbdFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Prima di tutto: PbdState.refresh() legge la piattaforma.
        Pbd.init(new PbdFabricConfig());
        PbdState.refresh();

        // NeoForge ha ModConfigEvent per il primo load; su Fabric la config è già
        // letta nel costruttore qui sopra, quindi basta il refresh appena fatto.
        ClientTickEvents.END_CLIENT_TICK.register(mc -> PbdState.clientTick());

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            for (String name : PbdCommands.NAMES) {
                dispatcher.register(tree(name));
            }
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> tree(String name) {
        return ClientCommandManager.literal(name)
                .executes(ctx -> reply(ctx.getSource(), PbdCommands.showMessage()))
                .then(ClientCommandManager.argument(PbdCommands.LEVEL_ARG,
                                DoubleArgumentType.doubleArg(PbdCommands.MIN_LEVEL, PbdCommands.MAX_LEVEL))
                        .executes(ctx -> reply(ctx.getSource(), PbdCommands.setAndDescribe(
                                DoubleArgumentType.getDouble(ctx, PbdCommands.LEVEL_ARG)))));
    }

    private static int reply(FabricClientCommandSource source, String message) {
        source.sendFeedback(Component.literal(message));
        return Command.SINGLE_SUCCESS;
    }
}
