# fetchurl Java SDK

Protocol-level client for [fetchurl](https://github.com/fetchurl/spec) content-addressable cache servers.

Zero runtime dependencies — uses only the JDK (`MessageDigest`, `java.net.http.HttpClient`). Works with any HTTP library via the `Fetcher` interface.

## Install

Maven:

```xml
<dependency>
  <groupId>io.github.fetchurl</groupId>
  <artifactId>fetchurl-sdk</artifactId>
  <version>0.1.0</version>
</dependency>
```

Gradle:

```gradle
implementation 'io.github.fetchurl:fetchurl-sdk:0.1.0'
```

Requires **Java 11+** (17+ recommended).

## Protocol

Normative behavior: **[fetchurl/spec](https://github.com/fetchurl/spec)** (`SPEC.md`).

Reference server: **[fetchurl/fetchurl](https://github.com/fetchurl/fetchurl)**.

## Usage

```java
import io.github.fetchurl.Fetchurl;
import io.github.fetchurl.JdkHttpClientFetcher;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

// High-level: reads FETCHURL_SERVER, tries servers then direct sources, verifies hash
try (OutputStream out = Files.newOutputStream(Path.of("file.bin"))) {
    Fetchurl.fetch(
        new JdkHttpClientFetcher(),
        "sha256",
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        List.of("https://cdn.example.com/file.bin"),
        out
    );
}
```

Drive the state machine yourself with any HTTP client:

```java
import io.github.fetchurl.FetchAttempt;
import io.github.fetchurl.FetchSession;
import io.github.fetchurl.HashVerifier;

import java.util.Optional;

FetchSession session = new FetchSession("sha256", expectedHash, sourceUrls);
Optional<FetchAttempt> opt;
while ((opt = session.nextAttempt()).isPresent()) {
    FetchAttempt attempt = opt.get();
    // GET attempt.getUrl() with attempt.getHeaders()
    // stream body through session.verifier(out), then verifier.finish()
    // session.reportSuccess() or session.reportPartial()
}
```

Clients **must** treat the server as untrusted and verify the hash (this SDK does that for you).

## Environment

| Variable | Meaning |
|----------|---------|
| `FETCHURL_SERVER` | Server base URL(s) per the [spec](https://github.com/fetchurl/spec/blob/main/SPEC.md). Empty/absent disables server use. |

## Development

```bash
mise install   # Java 17, Maven, svu
mvn -B test
```

## Related

| Repo | Role |
|------|------|
| [fetchurl/spec](https://github.com/fetchurl/spec) | Protocol |
| [fetchurl/fetchurl](https://github.com/fetchurl/fetchurl) | Go server |
| [fetchurl/sdk-js](https://github.com/fetchurl/sdk-js) | JavaScript SDK |
| [fetchurl/sdk-python](https://github.com/fetchurl/sdk-python) | Python SDK |
| [fetchurl/sdk-rust](https://github.com/fetchurl/sdk-rust) | Rust SDK |
