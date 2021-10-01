package us.jcedeno.teams.velocity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.velocitypowered.api.proxy.ProxyServer;

import us.jcedeno.teams.TeamManager;
import us.jcedeno.teams.objects.Team;

/**
 * Simple class to make a TeamManager for Bukkit.
 * 
 * @author jcedeno
 */
public class VTeamManager extends TeamManager {

    ProxyServer proxy;

    /**
     * Constructor for the BTeamManager class.
     * 
     * @param plugin   A Bukkit plugin instance.
     * @param redisURI The URI of the Redis server. Include password and port if
     *                 needed.
     */
    public VTeamManager(ProxyServer proxy, String redisURI) {
        super(redisURI);
        this.proxy = proxy;
        // Connect to the chain and pull current data.
        this.initialize();
    }

    /**
     * Do not use this method. It is only for internal use.
     */
    @Override
    public void updateTeam(Team team, UUID from) {
        put(team);
    }

    @Override
    public void processCommand(String cmd, UUID nodeId) {
        // Called when a command is received from a node.
        proxy.getCommandManager().executeImmediatelyAsync(proxy.getConsoleCommandSource(), cmd);

    }

    @Override
    public void processDestroyTeam(Team team, UUID nodeId) {
        remove(team);

    }

    /**
     * Gets a list of online Teams, if at least one of the members is online is
     * considered as team online.
     * 
     * @return A list of online Teams.
     */
    public List<Team> getTeamsOnlineList() {
        List<Team> teamsOnline = new ArrayList<>();
        proxy.getAllPlayers().forEach(player -> {
            var uuid = player.getUniqueId();
            var team = getPlayerTeam(uuid);
            if (team != null) {
                teamsOnline.add(team);
            }
        });
        return teamsOnline;
    }

    /**
     * Adds points to a team and broadcasts update to other nodes. This method is
     * blocking.
     * 
     * @param team   The team to add points to.
     * @param points The amount of points to add.
     */
    public void addPoints(Team team, int points) {
        // Update the local copy.
        team.addPoints(points);
        team.setLastObtainedPoints(System.currentTimeMillis());
        // Communicate to the backend and propagate the update
        this.writeTeamUpdate(team);
        this.communicateUpdate(team);
    }

}
