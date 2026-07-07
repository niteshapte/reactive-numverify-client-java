# Reactive Numverify Client (Java 25+)

A reactive Java client for the [Numverify](https://numverify.com) phone number validation and lookup API, built entirely with Project Reactor. The implementation validates phone numbers and retrieves country/dialing-code data through Numverify's REST API while keeping every call non-blocking, so it drops cleanly into any existing reactive pipeline.

## Features

1. **Reactive Programming**: Built entirely with Project Reactor (`Mono`/`Flux`) for non-blocking, asynchronous calls.
2. **Typed Domain Model**: Maps raw JSON responses into immutable Java records (`ValidatedPhoneNumber`, `Country`, `Countries`) instead of raw maps.
3. **Structured Error Handling**: A sealed `NumverifyError` hierarchy distinguishes API failures, network failures, and unexpected system errors.
4. **Framework-Agnostic HTTP**: Uses the JDK's native `HttpClient`, so no Spring or Netty dependency is required to use this library.
5. **Modular Design**: URL construction, HTTP execution, and JSON parsing are each isolated in their own class, independently testable.

## Overview

1. **URL Builder**:
   - Constructs and percent-encodes request URLs for the `validate` and `countries` endpoints.
   - Switches between HTTP and HTTPS base URLs.
2. **HTTP Executor**:
   - Issues the GET request via the JDK `HttpClient`, wrapped in a `Mono`.
   - Validates the HTTP status code and Numverify's API-level `success` flag.
3. **JSON Parser**:
   - Parses response bodies with Jackson.
   - Maps successful responses into domain records and failed responses into structured errors.
4. **Client**:
   - Composes the above into a small public API: `validatePhoneNumber(...)` and `getCountries()`.

## How It Works

- **Configuration**: Supply a Numverify access key and choose HTTP or HTTPS when constructing `NumverifyClient`.
- **Validation**: Call `validatePhoneNumber(phoneNumber)` to get a `Mono<ValidatedPhoneNumber>` describing the number's validity, carrier, line type, and country.
- **Country Lookup**: Call `getCountries()` to get a `Mono<Countries>`, which supports reactive lookups by country code or name.
- **Error Handling**: Any failure — API-level, HTTP-level, or unexpected — surfaces as a `NumverifyException` wrapping a sealed `NumverifyError`, which can be handled exhaustively with a `switch`.

## Key Technologies

- **Project Reactor**: For reactive, non-blocking request/response handling.
- **JDK `HttpClient`**: For dependency-light, framework-agnostic HTTP calls.
- **Jackson**: For JSON parsing and mapping into domain records.

## Example Use Case

This client is meant to be cloned as a starting point or added as a Maven dependency in projects that need to validate phone numbers or resolve country/dialing-code data without pulling in a full web framework. For example:

- User registration or checkout flows that verify a phone number before accepting it.
- Backend services enriching customer records with carrier or country metadata.
- Batch jobs cleaning up phone number datasets against a canonical country list.

## NumverifyClient

The `NumverifyClient` class is the main entry point for this library. It composes URL building, HTTP execution, and JSON parsing into a small reactive API. Here's the implementation:

```java
/**
 * Reactive, non-blocking client for the Numverify phone validation and
 * lookup API.
 *
 * <p>Wires together URL construction (NumverifyUrlBuilder), HTTP
 * execution (NumverifyHttpExecutor), and JSON parsing
 * (NumverifyJsonParser) behind a small typed API. Every call returns a
 * Mono, so results compose naturally into an existing Reactor or WebFlux
 * pipeline via flatMap — callers should avoid blocking on the result
 * except at a synchronous entry point.
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
     * @return a Mono emitting the validation result; completes with a
     *         NumverifyException on API, network, or parsing failure
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
     *                    numbers (e.g. "US"); pass an empty string if not
     *                    needed
     * @return a Mono emitting the validation result; completes with a
     *         NumverifyException on API, network, or parsing failure
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
     * @return a Mono emitting the complete Countries collection; completes
     *         with a NumverifyException on API, network, or parsing
     *         failure
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
```

### Explanation

1. **Purpose**: `NumverifyClient` is the single public entry point consumers interact with — it hides URL construction, HTTP execution, and JSON parsing behind two methods.
2. **Constructors**:
   - `NumverifyClient(accessKey, useHttps)`: full control over HTTP vs HTTPS.
   - `NumverifyClient(accessKey)`: convenience constructor defaulting to plain HTTP.
3. **`validatePhoneNumber`**:
   - Builds the request URL, executes it reactively, and maps the response into a `ValidatedPhoneNumber`.
   - Overload without a country code delegates to the two-argument version with an empty string.
4. **`getCountries`**:
   - Builds and executes the `countries` request, maps the response into a list of `Country` records, then collects them into an immutable `Countries` wrapper.
5. **Reactive Pattern**:
   - Uses `Mono.fromSupplier` to lazily build each URL only when subscribed.
   - Chains `flatMap`/`map` so the entire request lifecycle stays non-blocking end to end.

### Output Example

- Valid number:
```
Phone is valid. Running async lookup for country mapping...
Matched Dialing Prefix: 1
```

- API failure:
```
API Fault: invalid_access_key
```

## Getting Started

### Prerequisites

- Java 25 or higher (Developed on Java 25 but might work on previous versions as well - Test it yourself please).
- Build tool: Maven.
- A Numverify access key from [numverify.com](https://numverify.com).

### Running the Project

1. Clone the repository:
```
git clone https://github.com/<your-org>/reactive-numverify-client-java.git
```

2. Navigate to the project directory:
```
cd reactive-numverify-client-java
```

3. Compile and install:
```
mvn clean install
```

4. Add it as a dependency in your own `pom.xml`:
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>reactive-numverify-client-java</artifactId>
    <version>1.0</version>
</dependency>
```
