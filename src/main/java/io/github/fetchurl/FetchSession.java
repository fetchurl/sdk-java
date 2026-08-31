package io.github.fetchurl;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * State machine driving the fetchurl client protocol.
 *
 * <p>Servers are tried first (with {@code X-Source-Urls} forwarded), then direct source URLs in
 * random order per the spec. Not thread-safe.
 *
 * <p>The public constructor reads {@code FETCHURL_SERVER} from the environment. For tests, use
 * {@link #withServers(List, String, String, List)}.
 */
public final class FetchSession {
    private final List<FetchAttempt> attempts;
    private int current;
    private final String algo;
    private final String hash;
    private boolean done;
    private boolean success;

    /**
     * Create a session using servers from the {@code FETCHURL_SERVER} environment variable.
     *
     * @param algo hash algorithm name (e.g. {@code "sha256"})
     * @param hash expected hash in hex
     * @param sourceUrls direct source URLs (required, non-empty)
     */
    public FetchSession(String algo, String hash, List<String> sourceUrls) {
        this(Sfv.parseFetchurlServer(System.getenv("FETCHURL_SERVER")), algo, hash, sourceUrls);
    }

    /**
     * Create a session with an explicit server list (does not read the environment).
     *
     * @param servers cache server base URLs (may be empty)
     * @param algo hash algorithm name
     * @param hash expected hash in hex
     * @param sourceUrls direct source URLs (required, non-empty)
     */
    public static FetchSession withServers(
            List<String> servers, String algo, String hash, List<String> sourceUrls) {
        return new FetchSession(servers, algo, hash, sourceUrls);
    }

    FetchSession(List<String> servers, String algo, String hash, List<String> sourceUrls) {
        if (sourceUrls == null || sourceUrls.isEmpty()) {
            throw new MissingSourceUrlsException();
        }
        // Fail early on null/blank entries — same spirit as hash validation before any I/O.
        List<String> sources = new ArrayList<>(sourceUrls.size());
        for (String sourceUrl : sourceUrls) {
            if (sourceUrl == null || sourceUrl.isBlank()) {
                throw new FetchUrlException("source URL must not be blank");
            }
            sources.add(sourceUrl);
        }
        String normalized = Algo.normalize(algo);
        if (!Algo.isSupported(normalized)) {
            throw new UnsupportedAlgorithmException(normalized);
        }
        this.algo = normalized;
        // Spec: hashes MUST be lowercase hex of the full digest. Fail early on garbage.
        this.hash = Algo.normalizeContentHash(normalized, hash);
        this.attempts = new ArrayList<>();

        List<String> serverList = servers != null ? servers : Collections.emptyList();
        String sourceHeader = Sfv.encodeSourceUrls(sources);

        for (String server : serverList) {
            // SFV can yield empty strings (e.g. ""); skip rather than building a relative path.
            if (server == null || server.isBlank()) {
                continue;
            }
            // Fail early on CR/LF/etc. — same class of misuse as source URLs in X-Source-Urls.
            // Blank entries stay skipped (SFV noise); non-blank garbage is operator config error.
            Sfv.requireNoControlChars(server, "server URL");
            String base = trimTrailingSlashes(server);
            if (base.isEmpty()) {
                continue;
            }
            String url = base + "/" + this.algo + "/" + this.hash;
            Map<String, String> headers = new LinkedHashMap<>();
            if (!sourceHeader.isEmpty()) {
                headers.put("X-Source-Urls", sourceHeader);
            }
            attempts.add(new FetchAttempt(url, headers));
        }

        List<String> direct = new ArrayList<>(sources);
        Collections.shuffle(direct);
        for (String url : direct) {
            attempts.add(new FetchAttempt(url));
        }
    }

    private static String trimTrailingSlashes(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '/') {
            end--;
        }
        return s.substring(0, end);
    }

    /**
     * Get the next attempt, or empty if the session is finished.
     *
     * <p>If an attempt fails without writing bytes, call this again to try the next source.
     */
    public Optional<FetchAttempt> nextAttempt() {
        if (done || current >= attempts.size()) {
            return Optional.empty();
        }
        return Optional.of(attempts.get(current++));
    }

    /** Mark the session as successful. Stops further attempts. */
    public void reportSuccess() {
        done = true;
        success = true;
    }

    /** Mark that bytes were written before failure. Stops further attempts (output is tainted). */
    public void reportPartial() {
        done = true;
    }

    /** Whether the session completed with a successful download. */
    public boolean succeeded() {
        return success;
    }

    /** Create a {@link HashVerifier} for this session's algorithm and expected hash. */
    public HashVerifier verifier(OutputStream out) {
        return new HashVerifier(algo, hash, out);
    }

    String getAlgo() {
        return algo;
    }

    String getHash() {
        return hash;
    }
}
