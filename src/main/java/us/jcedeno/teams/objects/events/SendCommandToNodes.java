package us.jcedeno.teams.objects.events;

import java.util.UUID;

import us.jcedeno.teams.velocity.VTeamManager;

/**
 * This class represents a command to send to a node. A command is a string
 * command and by default {@link VTeamManager} will execute the command using
 * the console dispatcher.
 */
public class SendCommandToNodes {
    private String command;
    private UUID from;

    /**
     * @return The nodeId that that generated the request.
     */
    public UUID getFrom() {
        return from;
    }

    /**
     * @return The command to send to the nodes.
     */
    public String getCommand() {
        return command;
    }

    public SendCommandToNodes(String command, UUID from) {
        this.command = command;
        this.from = from;
    }

}
