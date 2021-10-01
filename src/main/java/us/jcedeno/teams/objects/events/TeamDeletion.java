package us.jcedeno.teams.objects.events;

import java.util.UUID;

import us.jcedeno.teams.objects.Team;

/**
 * An objected intended to represent a team deletion event for all nodes to
 * read.
 */
public class TeamDeletion {
    private Team team;
    private UUID from;

    /**
     * @return The {@link Team} that should be deleted.
     */
    public Team getTeam() {
        return team;
    }

    /**
     * @return The nodeId that that generated the request.
     */
    public UUID getFrom() {
        return from;
    }

    public TeamDeletion(Team team, UUID from) {
        this.team = team;
        this.from = from;
    }

}
