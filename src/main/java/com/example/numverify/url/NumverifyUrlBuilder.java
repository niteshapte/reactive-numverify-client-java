package com.example.numverify.url;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds Numverify API request URLs. Extracted from {@code NumverifyClient}
 * so URL/query-string construction is independently testable.
 *
 * <p>All query parameter values are percent-encoded, so callers may pass
 * raw, unescaped values.
 */
public final class NumverifyUrlBuilder {

    private static final String BASE_URL_HTTP = "http://apilayer.net/api/";
    private static final String BASE_URL_HTTPS = "https://apilayer.net/api/";

    private final String baseUrl;
    private final String accessKey;

    /**
     * Creates a URL builder for the given access key and protocol.
     *
     * @param accessKey the Numverify API access key to embed in every
     *                  generated URL
     * @param useHttps  whether to build URLs against Numverify's HTTPS
     *                  endpoint instead of HTTP; HTTPS access requires a
     *                  paid Numverify plan
     */
    public NumverifyUrlBuilder(String accessKey, boolean useHttps) {
        this.accessKey = accessKey;
        this.baseUrl = useHttps ? BASE_URL_HTTPS : BASE_URL_HTTP;
    }

    /**
     * Builds the URL for a {@code validate} request.
     *
     * @param phoneNumber the phone number to validate, in any format
     *                    Numverify accepts
     * @param countryCode ISO country code used to disambiguate local-format
     *                    numbers; pass an empty string if not needed
     * @return the fully constructed, percent-encoded request URL
     */
    public String validateUrl(String phoneNumber, String countryCode) {
        return "%svalidate?access_key=%s&number=%s&country_code=%s"
                .formatted(baseUrl, encode(accessKey), encode(phoneNumber), encode(countryCode));
    }

    /**
     * Builds the URL for a {@code countries} request.
     *
     * @return the fully constructed, percent-encoded request URL
     */
    public String countriesUrl() {
        return "%scountries?access_key=%s".formatted(baseUrl, encode(accessKey));
    }

    /**
     * Percent-encodes a single query parameter value using UTF-8.
     *
     * @param value the raw value to encode
     * @return the percent-encoded value, safe to embed in a URL query string
     */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}