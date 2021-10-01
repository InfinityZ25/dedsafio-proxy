package us.jcedeno.commands;

import java.util.ArrayList;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.minimessage.MiniMessage;
import us.jcedeno.DedsafioProxy;

public class SpreadTeams implements SimpleCommand {

    private final DedsafioProxy proxy;
    private static MiniMessage mini = MiniMessage.get();

    public SpreadTeams(DedsafioProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        var src = invocation.source();
        var args = invocation.arguments();
        if (args.length < 1) {
            src.sendMessage(mini.parse("&cUsage: /spread-teams [servers]"));
            return;
        }

        if (src instanceof Player player) {
            var servers = new ArrayList<RegisteredServer>();
            for (var arg : args) {
                var server = proxy.getServer().getServer(arg);
                if (server.isPresent())
                    servers.add(server.get());
            }

            var senderServer = player.getCurrentServer().get().getServer();
            var teams = proxy.getTeamManager().getTeamsOnlineList();
            player.sendMessage(mini.parse(
                    "&aSpreading " + teams.size() + " teams into &b" + servers.size() + " &aservers [" + args + "]"));
            var iter = teams.iterator();

            var count = 0;
            while (iter.hasNext()) {
                if (count >= servers.size())
                    count = 0;
                var nP = iter.next();
                var targetServer = servers.get(count);
                for (var m : nP.getMembers()) {
                    proxy.getServer().getPlayer(m).ifPresent(present -> {
                        var current = present.getCurrentServer();
                        if (current.isPresent() && current.get().getServer().equals(senderServer)) {
                            player.sendMessage(mini.parse("Sending " + present.getUsername() + " to "
                                    + targetServer.getServerInfo().getName()));
                            present.sendMessage(
                                    mini.parse("&aYou have been moved to &b" + targetServer.getServerInfo().getName()));
                            present.createConnectionRequest(targetServer).fireAndForget();
                        }

                    });

                }
                count++;
            }
        }

    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("desafiocommand.admin");
    }

}
