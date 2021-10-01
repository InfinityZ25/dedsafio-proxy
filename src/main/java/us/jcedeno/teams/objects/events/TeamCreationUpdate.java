package us.jcedeno.teams.objects.events;

import java.util.UUID;

import us.jcedeno.teams.objects.Team;

/**
 * An object to represent an abstract update of data across the many different
 * nodes. Intended to me used with GSON to quickly convert JSON to Java objects
 * and do more logic.
 */
public class TeamCreationUpdate {
    private Team team;
    private UUID from;

    /**
     * @return The {@link Team} that should be updated.
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

    public TeamCreationUpdate(Team team, UUID from) {
        this.team = team;
        this.from = from;
    }

}
