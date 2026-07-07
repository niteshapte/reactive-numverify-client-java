package com.example.numverify.client;

import java.net.http.HttpClient;

import com.example.numverify.http.NumverifyHttpExecutor;
import com.example.numverify.json.NumverifyJsonParser;
import com.example.numverify.record.Countries;
import com.example.numverify.record.ValidatedPhoneNumber;
import com.example.numverify.url.NumverifyUrlBuilder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive, non-blocking client for the Numverify phone validation and
 * lookup API.
 *
 * <p>Wires together URL construction ({@link NumverifyUrlBuilder}), HTTP
 * execution ({@link NumverifyHttpExecutor}), and JSON parsing
 * ({@link NumverifyJsonParser}) behind a small typed API. Every call
 * returns a {@link Mono}, so results compose naturally into an existing
 * Reactor or WebFlux pipeline via {@code flatMap} — callers should avoid
 * blocking on the result except at a synchronous entry point.
 *
 * <p>Instances are immutable and safe to share/reuse across calls.
 */
public final class NumverifyClient {

    private final NumverifyUrlBuilder urlBuilder;
    private final NumverifyHttpExecutor httpExecutor;
    private final NumverifyJsonParser jsonParser;

    /**
     * Creates a client using the given Numverify access key.
     *
     * @param accessKey the Numverify API access key
     * @param useHttps  whether to call Numverify's HTTPS endpoint instead
     *                  of HTTP; HTTPS access requires a paid Numverify plan
     */
    public NumverifyClient(String accessKey, boolean useHttps) {
        this.urlBuilder = new NumverifyUrlBuilder(accessKey, useHttps);
        this.jsonParser = new NumverifyJsonParser();
        this.httpExecutor = new NumverifyHttpExecutor(HttpClient.newBuilder().build(), jsonParser);
    }

    /**
     * Creates a client using plain HTTP, matching Numverify's free-tier
     * default.
     *
     * @param accessKey the Numverify API access key
     */
    public NumverifyClient(String accessKey) {
        this(accessKey, false);
    }

    /**
     * Validates a phone number without a country code hint.
     *
     * @param phoneNumber the phone number to validate, in any format
     *                    Numverify accepts
     * @return a {@link Mono} emitting the validation result; completes
     *         with a {@link com.example.numverify.error.NumverifyException}
     *         on API, network, or parsing failure
     */
    public Mono<ValidatedPhoneNumber> validatePhoneNumber(String phoneNumber) {
        return validatePhoneNumber(phoneNumber, "");
    }

    /**
     * Validates a phone number, optionally scoping the lookup to a
     * specific country.
     *
     * @param phoneNumber the phone number to validate, in any format
     *                    Numverify accepts
     * @param countryCode ISO country code used to disambiguate local-format
     *                    numbers (e.g. {@code "US"}); pass an empty string
     *                    if not needed
     * @return a {@link Mono} emitting the validation result; completes
     *         with a {@link com.example.numverify.error.NumverifyException}
     *         on API, network, or parsing failure
     */
    public Mono<ValidatedPhoneNumber> validatePhoneNumber(String phoneNumber, String countryCode) {
        return Mono.fromSupplier(() -> urlBuilder.validateUrl(phoneNumber, countryCode))
                .flatMap(httpExecutor::get)
                .map(jsonParser::toValidatedPhoneNumber);
    }

    /**
     * Fetches the full list of countries supported by Numverify, including
     * each country's dialing code.
     *
     * @return a {@link Mono} emitting the complete {@link Countries}
     *         collection; completes with a
     *         {@link com.example.numverify.error.NumverifyException} on
     *         API, network, or parsing failure
     */
    public Mono<Countries> getCountries() {
        return Mono.fromSupplier(urlBuilder::countriesUrl)
                .flatMap(httpExecutor::get)
                .map(jsonParser::toCountries)
                .flatMapMany(Flux::fromIterable)
                .collectList()
                .map(Countries::new);
    }
}