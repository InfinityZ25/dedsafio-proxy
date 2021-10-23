package us.jcedeno;

import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import org.slf4j.Logger;

import lombok.Getter;
import us.jcedeno.commands.GetResourcePack;
import us.jcedeno.commands.SendCommand;
import us.jcedeno.commands.SpreadPlayers;
import us.jcedeno.commands.SpreadTeams;
import us.jcedeno.teams.velocity.VTeamManager;
import us.jcedeno.utils.JsonConfig;

/**
 * DedsafioProxy
 * 
 * @author jcedeno
 */
@Plugin(id = "dedsafio-bingo", name = "Dedsafio Bingo", version = "1.0", authors = { "jcedeno" })
public class DedsafioProxy {
    public static final String RESOURCEPACK_URL = "https://www.dropbox.com/s/qk79fbc9m8fihol/BINGO_FINAL.zip?dl=1";
    private final @Getter ProxyServer server;
    private final Logger logger;
    private @Getter VTeamManager teamManager;
    private @Getter JsonConfig jsonConfig;

    @Inject
    public DedsafioProxy(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e) {
        try {
            this.jsonConfig = new JsonConfig("secret.json");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        var redisUri = jsonConfig != null ? jsonConfig.getRedisUri() : null;
        // Hook the team , ensure no nulls
        this.teamManager = new VTeamManager(server, redisUri != null ? redisUri : "redis://147.182.135.68");
        // Register commands
        var cmdManager = server.getCommandManager();

        cmdManager.register(cmdManager.metaBuilder("send").build(), new SendCommand(server));

        cmdManager.register(cmdManager.metaBuilder("spread").build(), new SpreadPlayers(server));

        cmdManager.register(cmdManager.metaBuilder("spread-teams").build(), new SpreadTeams(this));

        cmdManager.register(cmdManager.metaBuilder("get-resourcepack").aliases("get-rp", "getrp").build(),
                new GetResourcePack(this));

    }

    public void sendResourcepack(Player player, String url) {
        player.sendResourcePackOffer(server.createResourcePackBuilder(url).setShouldForce(false).build());
    }

    @Subscribe
    public void onConnected(ServerConnectedEvent e) {
        var server = e.getServer();

        if (!e.getPreviousServer().isPresent()) {

            System.out.println("Server " + server.getServerInfo().getName());
            this.server.getScheduler().buildTask(this, () -> sendResourcepack(e.getPlayer(), RESOURCEPACK_URL))
                    .delay(3, TimeUnit.SECONDS).schedule();
        }

    }

}
