package us.jcedeno.teams.objects;

import java.util.List;
import java.util.UUID;

/**
 * Team intended to have more information then just the basics of a team, but
 * also scores and many more variables that are required in the different
 * applications of teams in different gamemodes.
 * 
 * @author jcedeno
 */
public class Team extends BaseTeam {
    protected Integer points;
    protected Long lastObtainedPoints;

    public Team(UUID teamID, List<UUID> members, String teamName) {
        super(teamID, members, teamName);
    }

    /**
     * 
     * @return The last time the team obtained points.
     */
    public Long getLastObtainedPoints() {
        return lastObtainedPoints;
    }

    /**
     * @return the points
     * @param lastObtainedPoints The last milliseconds the points were obtained
     */
    public void setLastObtainedPoints(Long lastObtainedPoints) {
        this.lastObtainedPoints = lastObtainedPoints;
    }

    /*
     * @return Team's current points.
     */
    public Integer getPoints() {
        return points != null ? points : 0;
    }

    /**
     * Setter for the points of the team.
     * 
     * <b>Note</b>: This won't update the database. Intended to be used in pair with
     * other functions that actually do update the database.
     * 
     * @param points
     */
    public void setPoints(Integer newPoints) {
        this.points = newPoints;
    }

    public void addPoints(Integer extraPoints) {
        if (points != null) {
            points += extraPoints;
        } else {
            points = extraPoints;
        }
    }

    public boolean isMember(UUID uuid) {
        return members.stream().anyMatch(member -> member.getMostSignificantBits() == uuid.getMostSignificantBits());
    }
    

    @Override
    public String toString() {
        return "Team [points=" + points + ", lastObtainedPoints=" + lastObtainedPoints + ", teamID=" + teamID
                + ", members=" + members + ", teamName=" + teamName + "]";
    }

}