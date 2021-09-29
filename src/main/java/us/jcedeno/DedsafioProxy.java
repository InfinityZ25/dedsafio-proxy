package us.jcedeno;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import org.slf4j.Logger;

import us.jcedeno.commands.SendCommand;
import us.jcedeno.commands.SpreadPlayers;

/**
 * DedsafioProxy
 * 
 * @author jcedeno
 */
@Plugin(id = "dedsafio-bingo", name = "Dedsafio Bingo", version = "1.0", authors = { "jcedeno" })
public class DedsafioProxy {
    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public DedsafioProxy(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e) {
        var cmdManager = server.getCommandManager();
        var sendCmdMeta = cmdManager.metaBuilder("send").build();
        cmdManager.register(sendCmdMeta, new SendCommand(server));
        // server.getCommandManager().register(BingoCommands.getBrigadierCommand());

        var spreadCmdMeta = cmdManager.metaBuilder("spread").build();
        cmdManager.register(spreadCmdMeta, new SpreadPlayers(server));
    }

    public void sendResourcepack(Player player, String url) {
        player.sendResourcePackOffer(server.createResourcePackBuilder(url).setShouldForce(true).build());
    }

    @Subscribe
    public void onConnected(ServerConnectedEvent e) {
        var server = e.getServer();

        if (!e.getPreviousServer().isPresent()) {
            System.out.println("Server " + server.getServerInfo().getName());
            sendResourcepack(e.getPlayer(), "https://www.dropbox.com/s/erka9far2yu8eho/BINGO_F_5.zip?dl=1");
        }

    }

}
