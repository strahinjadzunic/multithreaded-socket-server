package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Talks to one connected client on a pool thread.
 * The client first has to log in (max 3 attempts), then every message it sends
 * is returned according to its {@link ResponseMode}, until it sends "quit".
 */
class ClientHandler implements Runnable {

    static final String PASSWORD = "Raf123";
    static final int MAX_ATTEMPTS = 3;
    static final String QUIT_COMMAND = "quit";

    private final Socket socket;
    private final int clientId;
    private final ResponseMode mode;

    ClientHandler(Socket socket, int clientId) {
        this.socket = socket;
        this.clientId = clientId;
        this.mode = ResponseMode.forClient(clientId);
    }

    @Override
    public void run() {
        try (socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            out.println("Welcome! You are client #" + clientId + ".");

            if (login(in, out)) {
                Server.log("Client #" + clientId + " logged in (" + mode + ")");
                handleMessages(in, out);
            } else {
                Server.log("Client #" + clientId + " failed to log in");
            }
        } catch (IOException e) {
            Server.log("Client #" + clientId + " connection error: " + e.getMessage());
        }

        Server.log("Client #" + clientId + " disconnected");
    }

    // Gives the client MAX_ATTEMPTS tries to enter the correct password.
    private boolean login(BufferedReader in, PrintWriter out) throws IOException {
        out.println("Enter password:");

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String input = in.readLine();
            if (input == null) {
                return false; // client closed the connection
            }

            if (input.equals(PASSWORD)) {
                out.println("Login successful. Your messages will be returned " + mode.getDescription()
                        + ". Type '" + QUIT_COMMAND + "' to exit.");
                return true;
            }

            int attemptsLeft = MAX_ATTEMPTS - attempt;
            if (attemptsLeft > 0) {
                out.println("Wrong password (attempts left: " + attemptsLeft + "). Try again:");
            } else {
                out.println("Wrong password. No attempts left, closing connection.");
            }
        }
        return false;
    }

    // Sends every message back transformed, until the client sends "quit".
    private void handleMessages(BufferedReader in, PrintWriter out) throws IOException {
        String message;
        while ((message = in.readLine()) != null) {
            if (message.equalsIgnoreCase(QUIT_COMMAND)) {
                out.println("Goodbye!");
                return;
            }

            Server.log("Client #" + clientId + ": " + message);
            out.println(mode.apply(message));
        }
    }
}
