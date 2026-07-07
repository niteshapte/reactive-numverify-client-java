package com.example.numverify;

import com.example.numverify.client.NumverifyClient;
import com.example.numverify.error.NumverifyError;
import com.example.numverify.error.NumverifyException;
import com.example.numverify.record.ValidatedPhoneNumber;

/**
 * Manual smoke-test entry point for {@link NumverifyClient}.
 *
 * <p>Demonstrates a typical reactive composition: validate a phone number,
 * then, if valid, look up the matching country's dialing prefix from the
 * full country list. This class is a runnable example only — it is not
 * part of the library's public API and should not be depended on by
 * consumers of this jar.
 *
 * <p>Replace {@code "YOUR_API_KEY"} with a real Numverify access key and
 * {@code "XXXXXXX"} with a real phone number before running.
 */
public class Main {
	
	/**
     * Runs the sample validation-then-lookup pipeline and blocks until it
     * completes.
     *
     * <p>{@code .block()} is used here only because this is a synchronous
     * script entry point; library consumers embedding {@link NumverifyClient}
     * in a reactive pipeline should never block on its {@code Mono} results.
     *
     * @param args unused
     * @throws InterruptedException declared for signature compatibility;
     *         not currently thrown by this implementation
     */
    public static void main(String[] args) throws InterruptedException {
    	
        var client = new NumverifyClient("YOUR_API_KEY", true);

        client.validatePhoneNumber("XXXXXXX")
            .filter(ValidatedPhoneNumber::isValid)
            .flatMap(res -> {
                System.out.println("Phone is valid. Running async lookup for country mapping...");
                return client.getCountries()
                        .flatMap(countries -> countries.findByCountryCode(res.countryCode()));
            })
            .doOnNext(country -> System.out.println("Matched Dialing Prefix: " + country.dialingCode()))
            .doOnError(NumverifyException.class, ex -> {
                switch (ex.getErrorDetails()) {
                    case NumverifyError.ApiFailure api -> System.err.println("API Fault: " + api.info());
                    case NumverifyError.NetworkFailure net -> System.err.println("HTTP Down: " + net.statusCode());
                    case NumverifyError.SystemFailure sys -> System.err.println("Crash: " + sys.cause().getMessage());
                }
            })
            .block();
    }
}