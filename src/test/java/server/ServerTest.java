package server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests: start a real server on a free port and talk to it over sockets.
 */
class ServerTest {

    private Server server;

    @BeforeEach
    void startServer() throws IOException {
        server = new Server(0); // port 0 = let the OS pick any free port
        new Thread(server::start).start();
    }

    @AfterEach
    void stopServer() throws IOException {
        server.close();
    }

    @Test
    void clientsGetSequentialIds() throws IOException {
        try (TestClient first = connect();
             TestClient second = connect()) {
            assertEquals("Welcome! You are client #1.", first.readLine());
            assertEquals("Welcome! You are client #2.", second.readLine());
        }
    }

    @Test
    void oddClientGetsUppercaseAndEvenClientGetsReversed() throws IOException {
        try (TestClient odd = connect();
             TestClient even = connect()) {
            odd.login();
            even.login();

            odd.send("Hello World");
            even.send("Hello World");

            assertEquals("HELLO WORLD", odd.readLine());
            assertEquals("dlroW olleH", even.readLine());
        }
    }

    @Test
    void connectionIsClosedAfterThreeWrongPasswords() throws IOException {
        try (TestClient client = connect()) {
            client.readLine(); // "Welcome! You are client #1."
            client.readLine(); // "Enter password:"

            for (int i = 1; i <= ClientHandler.MAX_ATTEMPTS; i++) {
                client.send("wrong" + i);
                assertTrue(client.readLine().startsWith("Wrong password"));
            }

            assertNull(client.readLine(), "server should close the connection");
        }
    }

    @Test
    void quitEndsTheSession() throws IOException {
        try (TestClient client = connect()) {
            client.login();
            client.send("quit");

            assertEquals("Goodbye!", client.readLine());
            assertNull(client.readLine(), "server should close the connection");
        }
    }

    private TestClient connect() throws IOException {
        return new TestClient(server.getPort());
    }

    // Small socket client used only in tests.
    private static class TestClient implements AutoCloseable {

        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;

        TestClient(int port) throws IOException {
            socket = new Socket("localhost", port);
            socket.setSoTimeout(3000); // fail the test instead of hanging forever if the server stops answering
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        }

        void send(String message) {
            out.println(message);
        }

        String readLine() throws IOException {
            return in.readLine();
        }

        // Skips the greeting and logs in with the correct password.
        void login() throws IOException {
            readLine(); // "Welcome! You are client #N."
            readLine(); // "Enter password:"
            send(ClientHandler.PASSWORD);
            assertTrue(readLine().startsWith("Login successful"));
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
