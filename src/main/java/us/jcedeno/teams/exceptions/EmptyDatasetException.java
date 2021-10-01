package us.jcedeno.teams.exceptions;

/**
 * Exception thrown when a dataset is empty.
 * 
 * @author jcedeno
 */
public class EmptyDatasetException extends Exception {

    /**
     * Simple exception to be thrown when a dataset is empty or no matches are
     * found.
     * 
     * @param message The message to be displayed.
     */
    public EmptyDatasetException(String message) {
        super(message);
    }

    /**
     * Static constructor for EmptyDatasetException. Useful to quickly throw and
     * exception
     * 
     * @param message The message to be displayed in the exception.
     * @return a new EmptyDatasetException
     */
    public static EmptyDatasetException of(String message) {
        return new EmptyDatasetException(message);
    }
}
