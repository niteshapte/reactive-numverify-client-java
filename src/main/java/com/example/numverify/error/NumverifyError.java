package com.example.numverify.error;

/**
 * Represents the distinct ways a call to the Numverify API can fail.
 *
 * <p>Sealed so that callers handling a
 * {@link com.example.numverify.error.NumverifyException} can exhaustively
 * {@code switch} over every failure case without a default branch,
 * distinguishing API-level failures, transport/HTTP failures, and
 * unexpected system errors.
 */
public sealed interface NumverifyError {
	/**
     * Indicates that Numverify processed the request but reported failure
     * (an HTTP 200 response with {@code "success": false}), e.g. an
     * invalid access key, an exhausted usage quota, or an invalid phone
     * number.
     *
     * @param code the Numverify-specific error code
     * @param type a short machine-readable error type, e.g.
     *             {@code "invalid_access_key"}
     * @param info a human-readable description of the failure
     */
    record ApiFailure(int code, String type, String info) implements NumverifyError {}
    
    /**
     * Indicates that the HTTP call itself did not succeed — the response
     * status code was not {@code 200}.
     *
     * @param statusCode the HTTP status code returned by the server
     * @param message    the raw response body, used as failure context
     *                   since a non-200 response is not guaranteed to be
     *                   parseable Numverify JSON
     */
    record NetworkFailure(int statusCode, String message) implements NumverifyError {}
    
    /**
     * Indicates an unexpected failure unrelated to the API contract or
     * HTTP transport — e.g. malformed JSON, a connection error, or a
     * timeout.
     *
     * @param cause the underlying exception that triggered this failure
     */
    record SystemFailure(Throwable cause) implements NumverifyError {}
}