package net.saturnx.pitchblackdarkness.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.saturnx.pitchblackdarkness.PitchBlackDarkness;

/**
 * Comando client {@code /pbd <0-5>} (alias {@code /pitchblack}): il livello si
 * cambia in gioco, senza riavvio, e funziona anche sui server altrui —
 * {@link RegisterClientCommandsEvent} è puramente client-side.
 *
 * <p>Senza argomenti mostra il livello attuale.</p>
 */
@EventBusSubscriber(modid = PitchBlackDarkness.MOD_ID, value = Dist.CLIENT)
public final class PbdCommands {
    private static final String[] LEVEL_LABELS = {
            "vanilla", "mild", "dark", "very dark", "black", "pitch black"};

    private PbdCommands() {}

    @SubscribeEvent
    static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(tree("pbd"));
        event.getDispatcher().register(tree("pitchblack"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> tree(String name) {
        return Commands.literal(name)
                .executes(ctx -> show(ctx.getSource()))
                .then(Commands.argument("level", DoubleArgumentType.doubleArg(0.0, 5.0))
                        .executes(ctx -> set(ctx.getSource(),
                                DoubleArgumentType.getDouble(ctx, "level"))));
    }

    /** "3 (dark)" per i livelli interi, "0.5" secco per i decimali (curva continua). */
    private static String describe(float level) {
        if (level == Math.floor(level)) {
            int i = (int) level;
            return i + " (" + LEVEL_LABELS[i] + ")";
        }
        return String.valueOf(level);
    }

    private static int show(CommandSourceStack source) {
        String desc = describe(PbdState.level());
        source.sendSuccess(() -> Component.literal(
                "PBD darkness level: " + desc + " — /pbd <0-5> to change (halves like 0.5 work too)"),
                false);
        return Command.SINGLE_SUCCESS;
    }

    private static int set(CommandSourceStack source, double level) {
        PbdState.setLevel(level);
        String desc = describe(PbdState.level());
        source.sendSuccess(() -> Component.literal("PBD darkness level set to " + desc),
                false);
        return Command.SINGLE_SUCCESS;
    }
}
