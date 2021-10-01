
package us.jcedeno.teams.sync;

public enum DedsafioChannels {
    EVENTS, SYNC, AUTH, CMD;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }

    /**
     * @return The full pipeline name of the channel
     */
    public String fullName() {
        return "dedsafio:" + this.toString();
    }

    /**
     * @return An array containing all the channels in the enum
     */
    public static String[] getAllChannels(){
        return new String[]{
                EVENTS.fullName(),
                SYNC.fullName(),
                AUTH.fullName(),
                CMD.fullName()
        };
    }
}
