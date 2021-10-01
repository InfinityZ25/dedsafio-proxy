package us.jcedeno.teams.sync;

import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import us.jcedeno.teams.TeamManager;
import us.jcedeno.teams.objects.Team;
import us.jcedeno.teams.objects.events.PipelineChangeSet;
import us.jcedeno.teams.objects.events.SendCommandToNodes;
import us.jcedeno.teams.objects.events.TeamCreationUpdate;
import us.jcedeno.teams.objects.events.TeamDeletion;

/**
 * Redis pipeline for team synchronization. Uses a Redis PubSub connection to
 * achieve the goal.
 */
public class RedisSyncPipeline implements RedisPubSubListener<String, String> {
    private static Gson gson = new Gson();
    private TeamManager teamManager;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private Logger logger;

    public RedisSyncPipeline(TeamManager teamManager) {
        this.teamManager = teamManager;
        this.pubSubConnection = teamManager.getRedisClient().connectPubSub();
        /** Add the listener */
        this.pubSubConnection.addListener(this);
        this.pubSubConnection.async().subscribe(DedsafioChannels.getAllChannels());
        this.logger = Logger.getLogger("sync-" + teamManager.getNodeId().toString().split("-")[0]);
    }

    /**
     * Method that communicates to all the other nodes about the update and event
     * that should be called.
     * 
     * @param team The team that was created.
     */
    public void communicateCreationOrUpdate(Team team) {
        logger.info("Attempting to communicate creation or update for " + team);
        this.teamManager.getRedisSyncConnection().publish(DedsafioChannels.EVENTS.fullName(),
                "create@:@" + gson.toJson(new TeamCreationUpdate(team, teamManager.getNodeId())));
    }

    public void communicateDestructionOfTeam(Team team) {
        logger.info("Attempting to communicate creation or update for " + team);
        this.teamManager.getRedisSyncConnection().publish(DedsafioChannels.EVENTS.fullName(),
                "destroy@:@" + gson.toJson(new TeamDeletion(team, teamManager.getNodeId())));
    }

    /**
     * Method that communicates to all the other nodes about the change of a set.
     * 
     * @param newDataset The new dataset.
     */
    public void communicateChangeOfDataset(String newDataset) {
        logger.info("Attempting to communicate a change of dataset to " + newDataset);
        this.teamManager.getRedisSyncConnection().publish(DedsafioChannels.SYNC.fullName(),
                gson.toJson(new PipelineChangeSet(newDataset, teamManager.getNodeId())));
    }

    /**
     * Method that communicates to all the other nodes a command to be executed.
     * 
     * @param cmd The command to be executed.
     * @return Integer indicating how many nodes recieved the command.
     */
    public long communicateCommandExecution(String cmd) {
        logger.info("Attempting to communicate a command execution " + cmd + " to other nodes.");
        return this.teamManager.getRedisSyncConnection().publish(DedsafioChannels.CMD.fullName(),
                gson.toJson(new SendCommandToNodes(cmd, teamManager.getNodeId())));
    }

    /**
     * @return true if the pipeline is connected to the Redis server.
     */
    public boolean isPipelineUp() {
        return this.pubSubConnection.isOpen();
    }

    @Override
    public void message(String channel, String message) {
        // logger.info(String.format("Channel %s: %s", channel, message));
        // Now process the message into whatever it should represent
        var split = channel.split(":");
        if (split.length < 1) {
            logger.warning("Wrong format?");
            return;
        }
        if (split[0].equals("dedsafio")) {
            var dChannel = DedsafioChannels.valueOf(split[1].toUpperCase());
            switch (dChannel) {
                // Process logic based on the channel that recieved the message.
                case EVENTS: {
                    var splitMessage = message.split("@:@");
                    var type = splitMessage[0];
                    var json = splitMessage[1];
                    if (type.equalsIgnoreCase("create")) {
                        try {
                            var creationUpdate = gson.fromJson(json, TeamCreationUpdate.class);
                            // Check if message is not comming from this node
                            if (creationUpdate != null) {
                                if (creationUpdate.getFrom().compareTo(teamManager.getNodeId()) == 0) {
                                    logger.info(String.format(
                                            "Received creation update for %s, ignorning since it comes from ourselves.",
                                            creationUpdate.getTeam()));
                                } else {
                                    logger.info("Updating " + creationUpdate.getTeam() + " from node "
                                            + creationUpdate.getFrom());
                                    teamManager.updateTeam(creationUpdate.getTeam(), creationUpdate.getFrom());
                                }
                                break;
                            }

                        } catch (JsonSyntaxException ex) {
                            ex.printStackTrace();
                        }
                    } else if (type.equalsIgnoreCase("destroy")) {
                        try {
                            var creationUpdate = gson.fromJson(json, TeamDeletion.class);
                            // Check if message is not comming from this node
                            if (creationUpdate != null) {
                                if (creationUpdate.getFrom().compareTo(teamManager.getNodeId()) == 0) {
                                    logger.info(String.format(
                                            "Received deletion update for %s, ignorning since it comes from ourselves.",
                                            creationUpdate.getTeam()));
                                } else {
                                    logger.info("Deleting " + creationUpdate.getTeam() + " from node "
                                            + creationUpdate.getFrom());
                                    teamManager.processDestroyTeam(creationUpdate.getTeam(), creationUpdate.getFrom());
                                }
                                break;
                            }

                        } catch (JsonSyntaxException ex) {
                            ex.printStackTrace();
                        }

                    }

                    break;
                }
                case SYNC: {
                    try {
                        var changeSet = gson.fromJson(message, PipelineChangeSet.class);
                        // Check if message is not comming from this node
                        if (changeSet != null) {
                            if (changeSet.getFrom().compareTo(teamManager.getNodeId()) == 0) {
                                logger.info(String.format(
                                        "Received change set for %s, ignorning since it comes from ourselves.",
                                        changeSet.getNewDataset()));
                            } else {
                                logger.info("Changing to dataset " + changeSet.getNewDataset()
                                        + " as indicated from node " + changeSet.getFrom());
                                teamManager.changeDataset(changeSet.getNewDataset(), false);
                            }
                            break;
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
                }
                case CMD: {
                    try {
                        var cmd = gson.fromJson(message, SendCommandToNodes.class);
                        if (cmd != null) {
                            if (cmd.getFrom().compareTo(teamManager.getNodeId()) == 0) {
                                logger.info(String.format("Received command from ourselves, ignoring."));
                            } else {
                                logger.info("Received command " + cmd.getCommand() + " from node " + cmd.getFrom());
                                // Run the command
                                teamManager.processCommand(cmd.getCommand(), cmd.getFrom());

                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
                }
                case AUTH: {
                    break;
                }
                default: {
                    logger.warning("Unknown channel: " + channel);
                }
            }
        }

    }

    @Override
    public void message(String pattern, String channel, String message) {

    }

    @Override
    public void subscribed(String channel, long count) {

    }

    @Override
    public void psubscribed(String pattern, long count) {

    }

    @Override
    public void unsubscribed(String channel, long count) {

    }

    @Override
    public void punsubscribed(String pattern, long count) {

    }

    public void closePubSubConnection() {
        this.pubSubConnection.close();
    }

}
