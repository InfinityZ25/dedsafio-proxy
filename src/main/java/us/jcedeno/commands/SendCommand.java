package us.jcedeno.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class SendCommand implements SimpleCommand {

    private final ProxyServer proxy;
    private static MiniMessage mini = MiniMessage.get();

    public SendCommand(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        var src = invocation.source();
        var args = invocation.arguments();
        if (args.length < 2) {
            src.sendMessage(mini.parse("&cUsage: /send <player> <message>"));
            return;
        }

        var targetPlayer = args[0];
        var optionalPlayer = proxy.getPlayer(targetPlayer);
        var targetServer = args[1];
        var optionalServer = proxy.getServer(targetServer);
        if (optionalPlayer.isPresent()) {
            var player = optionalPlayer.get();
            if (optionalServer.isPresent()) {
                var server1 = optionalServer.get();
                player.createConnectionRequest(server1).fireAndForget();
                src.sendMessage(mini.parse("Created a send request for " + player.getUsername() + " to server "
                        + server1.getServerInfo().getName()));
                return;
            }
        }

        src.sendMessage(mini.parse("Couldn't send player " + targetPlayer + " to server " + targetServer));

    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("desafiocommand.admin");
    }

}
