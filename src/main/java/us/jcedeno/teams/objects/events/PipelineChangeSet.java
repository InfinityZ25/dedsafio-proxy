package us.jcedeno.teams.objects.events;

import java.util.UUID;

/**
 * An event representing a change in the sync pipeline of the system. Used to
 * keep all nodes up to date.
 */
public class PipelineChangeSet {
    private String newDataset;
    private UUID from;

    public PipelineChangeSet(String newDataset, UUID from) {
        this.newDataset = newDataset;
        this.from = from;
    }

    /**
     * @return The name of the new dataset to be used
     */
    public String getNewDataset() {
        return newDataset;
    }

    /**
     * @return The nodeId that that generated the request.
     */
    public UUID getFrom() {
        return from;
    }

}
