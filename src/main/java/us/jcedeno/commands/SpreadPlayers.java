package us.jcedeno.commands;

import java.util.ArrayList;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class SpreadPlayers implements SimpleCommand {

    private final ProxyServer proxy;
    private static MiniMessage mini = MiniMessage.get();

    public SpreadPlayers(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        var src = invocation.source();
        var args = invocation.arguments();
        if (args.length < 1) {
            src.sendMessage(mini.parse("&cUsage: /spread [servers]"));
            return;
        }

        if (src instanceof Player player) {
            var servers = new ArrayList<RegisteredServer>();
            for (var arg : args) {
                var server = proxy.getServer(arg);
                if (server.isPresent())
                    servers.add(server.get());
            }

            RegisteredServer senderServer = player.getCurrentServer().get().getServer();
            var players = senderServer.getPlayersConnected();
            player.sendMessage(mini.parse("&aSpreading " + players.size() + " players into &b" + servers.size()
                    + " &aservers [" + args + "]"));
            var iter = players.iterator();
            var count = 0;
            while (iter.hasNext()) {
                if (count >= servers.size())
                    count = 0;
                var nP = iter.next();
                var targetServer = servers.get(count);
                nP.createConnectionRequest(targetServer).fireAndForget();
                System.out.println("Sending " + nP.getUsername() + " to " + targetServer.getServerInfo().getName());
                count++;
            }
        }

    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("desafiocommand.admin");
    }

}
