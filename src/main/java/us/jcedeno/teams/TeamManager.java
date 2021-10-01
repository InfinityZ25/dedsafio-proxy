package us.jcedeno.teams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.Getter;
import lombok.Setter;
import us.jcedeno.teams.exceptions.EmptyDatasetException;
import us.jcedeno.teams.exceptions.TeamAlreadyExistsException;
import us.jcedeno.teams.objects.Team;
import us.jcedeno.teams.sync.RedisSyncPipeline;

/**
 * A concurrent, multi nodal, multi threaded team manager.
 * 
 * @author jcedeno
 */
public abstract class TeamManager {
    /** Static Variables */
    private static Gson gson = new Gson();
    private static UUID nodeId = UUID.randomUUID();
    private static final String BACKUP_SET = "historical-sets";
    /** This is the name of the hashset on redis. */
    private @Getter @Setter String dataset = "ffa";
    /** Instance Variables */
    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> redisConnection;
    private ConcurrentHashMap<UUID, Team> teams;
    /** Synchronisation pipeline */
    private RedisSyncPipeline syncPipeline;
    private Logger logger;

    public TeamManager(String redisURI) {
        this.teams = new ConcurrentHashMap<>();
        this.redisClient = RedisClient.create(redisURI);
        this.redisConnection = this.redisClient.connect();
        this.syncPipeline = new RedisSyncPipeline(this);
        this.logger = Logger.getLogger("TeamManager-" + nodeId.toString().split("-")[0]);
    }

    /**
     * Updates the team with the given UUID.
     * 
     * @param team   The team to update.
     * @param nodeId The nodeId of the node that is updating the team.
     */
    public abstract void updateTeam(Team team, UUID nodeId);

    /**
     * Destroys the given team from the local copy.
     * 
     * @param team   The team to be destroyed.
     * @param nodeId The nodeId of the node that is updating the team.
     */
    public abstract void processDestroyTeam(Team team, UUID nodeId);

    /**
     * Processes the recieved command.
     * 
     * @param cmd    The command to process.
     * @param nodeId The nodeId of the node that is processing the command.
     */
    public abstract void processCommand(String cmd, UUID nodeId);

    /**
     * @return Returns the player team, it doesn't have one return null.
     */
    public Team getPlayerTeam(UUID uuid) {

        if (teams.isEmpty())
            return null;
        return teams.values().stream().filter(team -> team.isMember(uuid)).findFirst().orElse(null);
    }

    /**
     * @return The common redis sync connection used to send messages or use any
     *         other command. This object blocks the thread that executes it.
     */
    public RedisCommands<String, String> getRedisSyncConnection() {
        return redisConnection.sync();
    }

    /**
     * @return The concurrent map of teams currently in ram.
     */
    public ConcurrentHashMap<UUID, Team> getTeamsMap() {
        return this.teams;
    }

    /**
     * Just plainly puts the team object into the map. No security checks are taken,
     * so use this with caution.
     * 
     * @param team The team to add to the map.
     * @return The previous value associated with key, or null if there was no
     *         mapping for key
     */
    public Team put(Team team) {
        return this.teams.put(team.getTeamID(), team);
    }

    /**
     * Just plainly removes the team object from the map. No security checks are
     * taken, so use this with caution.
     * 
     * @param team The team to add to the map.
     * @return The previous value associated with key, or null if there was no
     *         mapping for key
     */
    public Team remove(Team team) {
        return this.teams.remove(team.getTeamID());
    }

    /**
     * Function that must be called at least once to guarantee data accuracy.
     */
    public void initialize() {
        // Ask redis if there is an ongoing sync in the db
        var syncConn = getRedisSyncConnection();
        // Pull the dataset name
        var datasetOnBackend = syncConn.get("dataset_name");
        if (datasetOnBackend != null) {
            this.dataset = datasetOnBackend;
        }
        if (syncConn.hlen(dataset) != 0) {
            // Restore all the current data
            syncConn.hgetall(dataset).forEach((k, v) -> {
                var team = gson.fromJson(v, Team.class);
                teams.put(team.getTeamID(), team);
            });
        }
    }

    /**
     * Util function that returns the status of the pipeline.
     * 
     * @return the status of the pipeline.
     */
    public boolean isPipelineUp() {
        return this.syncPipeline.isPipelineUp();
    }

    /**
     * It changes the dataset that is used to store the teams. both locally and
     * remotely. This method will block until the operation is completed.
     * 
     * @param newSet      The new dataset name.
     * @param communicate If true, it will communicate the change to the other
     *                    nodes.
     */
    public void changeDataset(String newSet, boolean communicate) {
        // Nill all the data, dump it somewhere.
        backupDataset();
        teams.clear();
        this.dataset = newSet;
        var syncCon = this.getRedisSyncConnection();
        if (syncCon.hlen(dataset) != 0) {
            // Restore all the current data
            syncCon.hgetall(dataset).forEach((k, v) -> {
                var team = gson.fromJson(v, Team.class);
                teams.put(team.getTeamID(), team);
            });
        }
        if (communicate) {
            syncCon.set("dataset_name", newSet);
            this.syncPipeline.communicateChangeOfDataset(newSet);
        }
    }

    /**
     * It will backup the current team dataset to the historical-sets hash.
     */
    private void backupDataset() {
        if (!teams.isEmpty() && redisConnection.isOpen()) {
            // Key in format dataset:timeStamp:nodeId
            var field = this.dataset + ":" + System.currentTimeMillis() + ":" + nodeId;
            // Value in format json, contains all teams as an array of teams.
            var value = gson.toJson(teams.values());
            // Connect and backup the old data set.
            var syncCon = getRedisSyncConnection();
            syncCon.hset(BACKUP_SET, field, value);
        }
    }

    /**
     * It will tell the other nodes of the update or creation that has taken place.
     * 
     * @param team The team that has been updated or created.
     */
    public void communicateUpdate(Team team) {
        this.syncPipeline.communicateCreationOrUpdate(team);
    }

    /**
     * It restores the old dataset. Once the update is deamed succesful, the
     * function will communicate to other nodes of the changes.
     * 
     * @throws EmptyDatasetException If the historical-sets hash is not present.
     * @param oldSet The old dataset name to be restored.
     */
    public void restoreOldDataset(String oldSet) throws EmptyDatasetException {
        // Create a synchronous connection to redis.
        var syncCon = getRedisSyncConnection();
        var keys = syncCon.hkeys(BACKUP_SET);
        // Check if the historical-sets is empty
        if (keys.isEmpty())
            throw new EmptyDatasetException(BACKUP_SET + " does not contain any hashes.");
        var matchedFields = new ArrayList<String>();
        // Iterate through to find those that match the old set.
        for (var key : keys) {
            var parsed = key.split(":");
            // If length greater than 1 means that the key is a valid key.
            if (parsed.length > 1 && parsed[0].equals(oldSet))
                matchedFields.add(key);
        }
        if (keys.isEmpty())
            throw new EmptyDatasetException(
                    "Dataset " + oldSet + " is not present in the backup set " + BACKUP_SET + ".");
        // Turn the matchedFields list into an array of strings.
        final var matchedFieldsArray = matchedFields.toArray(new String[0]);
        var matchedFieldValues = syncCon.hmget(BACKUP_SET, matchedFieldsArray);
        var newTeamsMap = new HashMap<UUID, Team>();
        // Iterate through the matchedFieldValues and parse them into teams.
        for (var fieldValues : matchedFieldValues) {
            var value = fieldValues.getValue();
            var teamList = gson.fromJson(value, Team[].class);
            // Parse all teams into a map.
            for (var team : teamList) {
                newTeamsMap.put(team.getTeamID(), team);
            }
        }
        // Backup current data in case of failure.
        backupDataset();
        // Perform the change of data
        teams.clear();
        teams.putAll(newTeamsMap);

        logger.info("Succesfully restored dataset: " + oldSet);
        // TODO: Communicate update to other nodes.
    }

    /**
     * Creates a new team and validates it
     * 
     * @param teamName The name of the team.
     * @param teamId   The UUID of the team.
     * @param uuids    The UUIDs of the players in the team.
     * @return The created team.
     * @throws TeamAlreadyExistsException If the team already exists.
     */
    public Team createTeam(String teamName, UUID teamId, UUID... uuids) throws TeamAlreadyExistsException {
        var team = registerTeam(new Team(teamId, Arrays.asList(uuids), teamName));
        // Communicate the update as succesful.
        communicateUpdate(team);
        return team;
    }

    /**
     * Creates a new team and validates it. Overflow method for easier use that
     * assigns random team id.
     * 
     * @param teamName The name of the team.
     * @param uuids    The UUIDs of the players in the team.
     * @return The created team.
     * @throws TeamAlreadyExistsException If the team already exists.
     */
    public Team createTeam(String teamName, UUID... uuids) throws TeamAlreadyExistsException {
        return createTeam(teamName, UUID.randomUUID(), uuids);
    }

    /**
     * A method that registers a new team in the database. Throws an exception if
     * the team already exists.
     * 
     * @param team The team to register.
     * @throws TeamAlreadyExistsException
     * 
     * @return The team that was registered.
     */
    public Team registerTeam(Team team) throws TeamAlreadyExistsException {
        if (!validateTeam(team)) {
            throw TeamAlreadyExistsException.of(team);
        }
        var syncCon = getRedisSyncConnection();
        syncCon.hset(dataset, team.getTeamID().toString(), gson.toJson(team));

        teams.put(team.getTeamID(), team);

        return team;
    }

    /**
     * A method that destroys a team and communicates update to other nodes.
     * 
     * @param team The team to destroy.
     * @return The destroyed team or null if not present.
     */
    public Team destroyTeam(Team team) {
        var affected = getRedisSyncConnection().hdel(dataset, team.getTeamID().toString());
        this.syncPipeline.communicateDestructionOfTeam(team);
        return affected > 0 ? teams.remove(team.getTeamID()) : null;
    }

    /**
     * A method that validates if a team already exists in some part of the state.
     * 
     * @param team The team to validate.
     * @return True if the team exists, false otherwise.
     */
    private boolean validateTeam(Team team) {
        /**
         * Should check locally, then remotely. Also, ensure a user is not present in
         * two teams at once.
         */
        if (redisConnection.isOpen())
            return !getRedisSyncConnection().hexists(dataset, team.getTeamID().toString());
        // Check if player is already member of a team
        for (var teamEntry : teams.entrySet()) {
            var queryTeam = teamEntry.getValue();
            if (queryTeam.getMembers().contains(team.getTeamID()))
                return false;
        }

        return team != null;
    }

    /**
     * A method that write an update of a team to the database.
     * 
     * @param team The team to update.
     * @return true if field is a new field in the hash and value was set. false if
     *         field already exists in the hash and the value was updated.
     */
    public boolean writeTeamUpdate(Team team) {
        return getRedisSyncConnection().hset(dataset, team.getTeamID().toString(), gson.toJson(team));
    }

    /**
     * Function intended to be called to forcibly update a team to the backend.
     * NOTE: No checks are performed when this is called. Use with caution.
     * 
     * @param team The team to update.
     */
    public void modifyTeam(Team team) {
        this.writeTeamUpdate(team);
        this.communicateUpdate(team);
    }

    /**
     * A method that communicates other nodes of a command to be executed.
     * 
     * @param cmd The command to be executed.
     * @return Integer indicating how many nodes recieved the command.
     */
    public long sendCommandToNodes(String cmd) {
        return this.syncPipeline.communicateCommandExecution(cmd);
    }

    /**
     * @return The redis client.
     */
    public RedisClient getRedisClient() {
        return this.redisClient;
    }

    /**
     * @return The node's UUID.
     */
    public UUID getNodeId() {
        return nodeId;
    }

    /**
     * Helper function to pretty print the current state of the system.
     */
    public void printContentsOfSet() {
        logger.info(new GsonBuilder().setPrettyPrinting().create().toJson(teams.values()));
    }

    /**
     * A method that performs a total disconnection from the redis server.
     */
    public void disconect() {
        this.redisConnection.close();
        this.syncPipeline.closePubSubConnection();
    }

    /**
     * Ejemplo de como hacer un team usando completable futures para hacerlo
     * non-blocking.
     */
    void comoHacerUnTeamXd() {
        CompletableFuture.supplyAsync(() -> {
            try {
                return createTeam("teamName", UUID.randomUUID(), new UUID[] { UUID.randomUUID() });
            } catch (TeamAlreadyExistsException e) {
                e.printStackTrace();
            }
            return null;
        }).thenAccept(teamOrNull -> {
            System.out.println(teamOrNull.getTeamName() + " fue creado.");

        });
    }

}
