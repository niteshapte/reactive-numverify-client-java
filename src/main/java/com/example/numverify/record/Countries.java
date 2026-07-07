package com.example.numverify.record;

import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * An immutable collection of the countries supported by Numverify.
 *
 * @param list the full list of countries; defensively copied into an
 *             unmodifiable list
 */
public record Countries(List<Country> list) {
	
	/**
     * Defensively copies {@code list} into an unmodifiable list so this
     * record remains immutable regardless of what the caller does with
     * the original list afterward.
     */
    public Countries {
        list = List.copyOf(list);
    }

    /**
     * Looks up a country by its ISO country code, ignoring case.
     *
     * @param countryCode the country code to match, e.g. {@code "US"}
     * @return a {@link Mono} emitting the matching {@link Country}, or
     *         empty if no country in this collection matches
     */
    public Mono<Country> findByCountryCode(String countryCode) {
        return Flux.fromIterable(list)
                .filter(c -> c.countryCode().equalsIgnoreCase(countryCode))
                .next();
    }

    /**
     * Looks up a country by its full name, ignoring case.
     *
     * @param countryName the country name to match, e.g.
     *                    {@code "United States"}
     * @return a {@link Mono} emitting the matching {@link Country}, or
     *         empty if no country in this collection matches
     */
    public Mono<Country> findByCountryName(String countryName) {
        return Flux.fromIterable(list)
                .filter(c -> c.countryName().equalsIgnoreCase(countryName))
                .next();
    }
}