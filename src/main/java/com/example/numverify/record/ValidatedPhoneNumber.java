package com.example.numverify.record;

/**
 * The result of validating a phone number via Numverify's {@code validate}
 * endpoint.
 *
 * <p>Fields Numverify cannot determine for a given number (e.g. carrier
 * or location for some countries) are mapped to an empty string rather
 * than {@code null} by {@link com.example.numverify.json.NumverifyJsonParser}.
 *
 * @param isValid       whether Numverify considers the number valid
 * @param number        the number in international format
 * @param localNumber   the number in local format
 * @param countryCode   the ISO country code, e.g. {@code "US"}
 * @param countryPrefix the dialing prefix, e.g. {@code "+1"}
 * @param countryName   the full country name
 * @param location      the approximate registered location, if available
 * @param carrier       the carrier name, if available
 * @param lineType      the line type, e.g. {@code "mobile"} or {@code "landline"}
 */
public record ValidatedPhoneNumber(
    boolean isValid,
    String number,
    String localNumber,
    String countryCode,
    String countryPrefix,
    String countryName,
    String location,
    String carrier,
    String lineType
) {}