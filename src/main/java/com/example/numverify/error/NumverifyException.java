package com.example.numverify.error;

/**
 * Unchecked exception thrown for any failure while calling the Numverify
 * API — whether an API-level rejection, an HTTP/transport failure, or an
 * unexpected system error.
 *
 * <p>Carries the structured {@link NumverifyError} that caused it, so
 * callers can pattern-match on {@link #getErrorDetails()} instead of
 * parsing {@link #getMessage()}. The human-readable message is derived
 * from the error details for logging/debugging convenience only.
 */
public class NumverifyException extends RuntimeException {
	
    private static final long serialVersionUID = 1L;
	private final NumverifyError errorDetails;

	/**
     * Creates a new exception wrapping the given error details.
     *
     * <p>The exception message is derived from {@code errorDetails}, and
     * the original {@link Throwable} cause is preserved when
     * {@code errorDetails} is a {@link NumverifyError.SystemFailure}.
     *
     * @param errorDetails the structured cause of this failure
     */
    public NumverifyException(NumverifyError errorDetails) {
        super(switch (errorDetails) {
            case NumverifyError.ApiFailure api -> "API Error " + api.code() + ": " + api.info();
            case NumverifyError.NetworkFailure net -> "HTTP Status " + net.statusCode() + " - " + net.message();
            case NumverifyError.SystemFailure sys -> "System failure: " + sys.cause().getMessage();
        }, errorDetails instanceof NumverifyError.SystemFailure sys ? sys.cause() : null);
        
        this.errorDetails = errorDetails;
    }

    /**
     * Returns the structured error details describing why this exception
     * was thrown.
     *
     * @return the {@link NumverifyError} associated with this exception
     */
    public NumverifyError getErrorDetails() {
        return errorDetails;
    }
}
