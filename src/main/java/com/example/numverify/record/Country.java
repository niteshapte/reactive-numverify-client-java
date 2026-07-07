package com.example.numverify.record;

/**
 * A single country entry as returned by Numverify's {@code countries}
 * endpoint.
 *
 * @param countryCode  the ISO country code, e.g. {@code "US"}
 * @param countryName  the full country name, e.g. {@code "United States"}
 * @param dialingCode  the international dialing code, e.g. {@code "1"}
 */
public record Country(String countryCode, String countryName, String dialingCode) {}