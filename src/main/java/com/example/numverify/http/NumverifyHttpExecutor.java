package com.example.numverify.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.example.numverify.error.NumverifyError;
import com.example.numverify.error.NumverifyException;
import com.example.numverify.json.NumverifyJsonParser;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

/**
 * Executes HTTP GET requests against Numverify and turns the raw response
 * into a validated {@link JsonNode}.
 *
 * <p>Wraps the JDK's asynchronous {@link HttpClient} in a {@link Mono},
 * and layers three checks on top of the raw response: the HTTP status
 * code must be {@code 200}, the body must be parseable JSON, and the
 * API-level {@code "success"} flag must not be {@code false}. Any of
 * these failing surfaces as a
 * {@link com.example.numverify.error.NumverifyException}.
 */
public final class NumverifyHttpExecutor {

    private final HttpClient httpClient;
    private final NumverifyJsonParser jsonParser;

    /**
     * Creates an executor that issues requests via the given HTTP client
     * and parses responses via the given JSON parser.
     *
     * @param httpClient the HTTP client used to send requests
     * @param jsonParser the parser used to read and validate response bodies
     */
    public NumverifyHttpExecutor(HttpClient httpClient, NumverifyJsonParser jsonParser) {
        this.httpClient = httpClient;
        this.jsonParser = jsonParser;
    }

    /**
     * Performs a GET request against the given URL, validating the HTTP
     * status and the API-level {@code "success"} flag before returning
     * the parsed JSON tree.
     *
     * @param url the fully constructed request URL, including query
     *            parameters
     * @return a {@link Mono} emitting the parsed response body on success;
     *         completes with a
     *         {@link com.example.numverify.error.NumverifyException}
     *         wrapping a {@link NumverifyError.NetworkFailure} on a
     *         non-200 status, a {@link NumverifyError.ApiFailure} when
     *         Numverify reports {@code "success": false}, or a
     *         {@link NumverifyError.SystemFailure} for any other error
     */
    public Mono<JsonNode> get(String url) {
        return Mono.defer(() -> {
                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                    return Mono.fromFuture(() -> httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()));
                })
                .flatMap(response -> response.statusCode() == 200
                        ? Mono.just(response.body())
                        : Mono.error(new NumverifyException(
                                new NumverifyError.NetworkFailure(response.statusCode(), response.body()))))
                .map(jsonParser::parse)
                .flatMap(this::validateOrError)
                .onErrorMap(err -> err instanceof NumverifyException
                        ? err
                        : new NumverifyException(new NumverifyError.SystemFailure(err)));
    }

    /**
     * Checks the parsed response for an API-level failure and converts it
     * into an error signal if present.
     *
     * @param root the parsed JSON response body
     * @return a {@link Mono} emitting {@code root} unchanged if Numverify
     *         reported success, otherwise a {@link Mono} that errors with
     *         a {@link com.example.numverify.error.NumverifyException}
     *         wrapping a {@link NumverifyError.ApiFailure}
     */
    private Mono<JsonNode> validateOrError(JsonNode root) {
        if (jsonParser.isApiFailure(root)) {
            return Mono.error(new NumverifyException(jsonParser.toApiFailure(root)));
        }
        return Mono.just(root);
    }
}