package io.github.fetchurl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

/**
 * High-level fetchurl client entry points.
 *
 * <p>Handles the full protocol loop: try each attempt, stream through a {@link HashVerifier},
 * report success/partial, or fail with {@link AllSourcesFailedException}.
 */
public final class Fetchurl {
    private static final int CHUNK_SIZE = 64 * 1024;

    private Fetchurl() {}

    /**
     * High-level sync fetch. Reads {@code FETCHURL_SERVER} via {@link FetchSession}.
     *
     * @param fetcher HTTP client
     * @param algo hash algorithm
     * @param hash expected hex hash
     * @param sourceUrls direct source URLs (required)
     * @param out destination stream (may receive partial data on {@link PartialWriteException})
     */
    public static void fetch(
            Fetcher fetcher,
            String algo,
            String hash,
            List<String> sourceUrls,
            OutputStream out) {
        FetchSession session = new FetchSession(algo, hash, sourceUrls);
        fetch(fetcher, session, out);
    }

    /**
     * High-level sync fetch with an explicit session (e.g. from {@link
     * FetchSession#withServers}).
     */
    public static void fetch(Fetcher fetcher, FetchSession session, OutputStream out) {
        Throwable lastError = null;
        Optional<FetchAttempt> opt;
        while ((opt = session.nextAttempt()).isPresent()) {
            FetchAttempt attempt = opt.get();
            FetchResponse response;
            try {
                response = fetcher.get(attempt.getUrl(), attempt.getHeaders());
            } catch (Exception e) {
                lastError = e;
                continue;
            }

            if (response.getStatusCode() != 200) {
                lastError = new FetchUrlException("unexpected status " + response.getStatusCode());
                closeQuietly(response.getBody());
                continue;
            }

            HashVerifier verifier = session.verifier(out);
            try (InputStream body = response.getBody()) {
                byte[] buf = new byte[CHUNK_SIZE];
                int n;
                while ((n = body.read(buf)) != -1) {
                    verifier.write(buf, 0, n);
                }
                verifier.finish();
                session.reportSuccess();
                return;
            } catch (Exception e) {
                lastError = e;
                if (verifier.getBytesWritten() > 0) {
                    session.reportPartial();
                    throw new PartialWriteException(e);
                }
            }
        }
        throw new AllSourcesFailedException(lastError);
    }

    private static void closeQuietly(InputStream in) {
        if (in == null) {
            return;
        }
        try {
            in.close();
        } catch (IOException ignored) {
            // ignore
        }
    }
}
