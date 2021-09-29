package us.jcedeno.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class BingoCommands {

    public static BrigadierCommand getBrigadierCommand() {
        var dispatched = new CommandDispatcher<CommandSource>();
/*
        LiteralArgumentBuilder<CommandSource> c = dispatched.register(LiteralArgumentBuilder.<CommandSource>literal("te")
                .then(RequiredArgumentBuilder.argument("bar", StringArgumentType.word()).executes(c -> {
                    if (c.getSource()instanceof CommandSource source) {
                        source.sendMessage(MiniMessage.get().parse("<red>" + c.getArgument("bar", String.class)));
                    }
                    return 1;
                })));
*/
        return null;
    }

}
