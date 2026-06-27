package io.github.fetchurl;

/** The requested hash algorithm is not supported. */
public class UnsupportedAlgorithmException extends FetchUrlException {
    private final String algo;

    public UnsupportedAlgorithmException(String algo) {
        super("unsupported algorithm: " + algo);
        this.algo = algo;
    }

    public String getAlgo() {
        return algo;
    }
}
