package com.example.numverify.json;

import java.util.ArrayList;
import java.util.List;

import com.example.numverify.error.NumverifyError;
import com.example.numverify.error.NumverifyException;
import com.example.numverify.record.Country;
import com.example.numverify.record.ValidatedPhoneNumber;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Centralizes all JSON parsing for the Numverify client using Jackson,
 * replacing brittle regex-based field extraction.
 *
 * <p>Responsible for three things: parsing raw response text into a
 * {@link JsonNode}, detecting Numverify's API-level
 * {@code "success": false} failures, and mapping successful responses
 * into this library's domain records ({@link ValidatedPhoneNumber},
 * {@link Country}).
 */
public final class NumverifyJsonParser {

    private final ObjectMapper objectMapper;

    /**
     * Creates a parser backed by a default, unconfigured
     * {@link ObjectMapper}.
     */
    public NumverifyJsonParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Creates a parser backed by the given {@link ObjectMapper}, allowing
     * callers to supply a shared or custom-configured mapper.
     *
     * @param objectMapper the Jackson mapper used to read JSON
     */
    public NumverifyJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses raw JSON text into a navigable {@link JsonNode} tree.
     *
     * @param json the raw response body to parse
     * @return the root node of the parsed JSON tree
     * @throws NumverifyException wrapping a
     *         {@link NumverifyError.SystemFailure} if {@code json} is not
     *         valid JSON
     */
    public JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new NumverifyException(new NumverifyError.SystemFailure(e));
        }
    }

    /**
     * Determines whether a parsed response represents an API-level
     * failure, i.e. Numverify returned {@code "success": false}.
     *
     * @param root the parsed response body
     * @return {@code true} if the response has a boolean {@code "success"}
     *         field set to {@code false}; {@code false} otherwise,
     *         including when the field is absent
     */
    public boolean isApiFailure(JsonNode root) {
        JsonNode success = root.get("success");
        return success != null && success.isBoolean() && !success.asBoolean();
    }

    /**
     * Extracts API failure details from a response already known to have
     * {@code "success": false}.
     *
     * @param root the parsed response body
     * @return the {@link NumverifyError.ApiFailure} described by the
     *         response's {@code "error"} object, or a generic
     *         "unknown_error" failure if that object is missing
     */
    public NumverifyError.ApiFailure toApiFailure(JsonNode root) {
        JsonNode error = root.get("error");
        if (error == null || error.isMissingNode()) {
            return new NumverifyError.ApiFailure(0, "unknown_error", "An unexpected API error occurred.");
        }
        int code = error.path("code").asInt(0);
        String type = error.path("type").asText("unknown_error");
        String info = error.path("info").asText("An unexpected API error occurred.");
        return new NumverifyError.ApiFailure(code, type, info);
    }

    /**
     * Maps a successful {@code validate} response into a
     * {@link ValidatedPhoneNumber}.
     *
     * <p>Missing fields default to {@code false} or an empty string rather
     * than throwing, since Numverify may omit fields it cannot determine.
     *
     * @param root the parsed, successful response body
     * @return the mapped {@link ValidatedPhoneNumber}
     */
    public ValidatedPhoneNumber toValidatedPhoneNumber(JsonNode root) {
        return new ValidatedPhoneNumber(
                root.path("valid").asBoolean(false),
                root.path("number").asText(""),
                root.path("local_format").asText(""),
                root.path("country_code").asText(""),
                root.path("country_prefix").asText(""),
                root.path("country_name").asText(""),
                root.path("location").asText(""),
                root.path("carrier").asText(""),
                root.path("line_type").asText("")
        );
    }

    /**
     * Maps a successful {@code countries} response into a list of
     * {@link Country} entries.
     *
     * <p>Numverify returns countries as a JSON object keyed by country
     * code rather than an array, so each property of {@code root} is
     * mapped to one {@link Country}.
     *
     * @param root the parsed, successful response body
     * @return the list of countries described by the response
     */
    public List<Country> toCountries(JsonNode root) {
        List<Country> countries = new ArrayList<>();
        root.properties().forEach(entry -> {
            String countryCode = entry.getKey();
            JsonNode value = entry.getValue();
            String countryName = value.path("country_name").asText("");
            String dialingCode = value.path("dialing_code").asText("");
            countries.add(new Country(countryCode, countryName, dialingCode));
        });
        return countries;
    }

    /**
     * Returns the {@link ObjectMapper} backing this parser, so callers
     * that need to reuse the same Jackson configuration elsewhere don't
     * have to construct their own.
     *
     * @return the underlying {@link ObjectMapper}
     */
    public ObjectMapper objectMapper() {
        return objectMapper;
    }
}