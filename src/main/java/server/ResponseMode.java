package server;

import java.util.Locale;

/**
 * How the server answers a logged-in client:
 * odd client IDs get their message in uppercase, even client IDs get it reversed.
 */
enum ResponseMode {

    UPPERCASE("in UPPERCASE"),
    REVERSED("reversed");

    private final String description;

    ResponseMode(String description) {
        this.description = description;
    }

    static ResponseMode forClient(int clientId) {
        return clientId % 2 != 0 ? UPPERCASE : REVERSED;
    }

    String apply(String message) {
        // Locale.ROOT, so the result doesn't depend on the system language (e.g. Turkish "i")
        return switch (this) {
            case UPPERCASE -> message.toUpperCase(Locale.ROOT);
            case REVERSED -> new StringBuilder(message).reverse().toString();
        };
    }

    String getDescription() {
        return description;
    }
}
