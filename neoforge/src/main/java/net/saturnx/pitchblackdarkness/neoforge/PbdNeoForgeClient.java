package net.saturnx.pitchblackdarkness.neoforge;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.saturnx.pitchblackdarkness.Pbd;
import net.saturnx.pitchblackdarkness.client.PbdCommands;
import net.saturnx.pitchblackdarkness.client.PbdState;

/**
 * Aggancio degli eventi client di NeoForge a {@code common}: il tick e il
 * comando {@code /pbd}. Nomi, range e messaggi vengono da {@link PbdCommands},
 * così restano identici a quelli di Fabric; qui c'è solo l'albero Brigadier,
 * che è tipizzato su {@code CommandSourceStack} e quindi non è condivisibile.
 */
@EventBusSubscriber(modid = Pbd.MOD_ID, value = Dist.CLIENT)
public final class PbdNeoForgeClient {
    private PbdNeoForgeClient() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        PbdState.clientTick();
    }

    @SubscribeEvent
    static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        for (String name : PbdCommands.NAMES) {
            event.getDispatcher().register(tree(name));
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> tree(String name) {
        return Commands.literal(name)
                .executes(ctx -> reply(ctx.getSource(), PbdCommands.showMessage()))
                .then(Commands.argument(PbdCommands.LEVEL_ARG,
                                DoubleArgumentType.doubleArg(PbdCommands.MIN_LEVEL, PbdCommands.MAX_LEVEL))
                        .executes(ctx -> reply(ctx.getSource(), PbdCommands.setAndDescribe(
                                DoubleArgumentType.getDouble(ctx, PbdCommands.LEVEL_ARG)))));
    }

    private static int reply(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
        return Command.SINGLE_SUCCESS;
    }
}
