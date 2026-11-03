package server;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multithreaded TCP server.
 * Every new connection gets the next client ID (1, 2, 3, ...) and is handled
 * by a {@link ClientHandler} on a thread from a fixed-size thread pool.
 */
public class Server implements AutoCloseable {

    public static final int DEFAULT_PORT = 2026;
    private static final int MAX_CLIENTS = 10;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ServerSocket serverSocket;
    private final ExecutorService pool = Executors.newFixedThreadPool(MAX_CLIENTS);
    private final AtomicInteger clientCounter = new AtomicInteger(0);

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        try (Server server = new Server(port)) {
            server.start();
        } catch (BindException e) {
            System.out.println("Port " + port + " is already in use. Is the server already running?");
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    /** Accepts new clients until the server is closed. */
    public void start() {
        log("Server started on port " + getPort());

        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                int clientId = clientCounter.incrementAndGet();
                log("Client #" + clientId + " connected (" + socket.getInetAddress().getHostAddress() + ")");
                pool.execute(new ClientHandler(socket, clientId));
            } catch (IOException e) {
                // accept() also fails when close() is called, which is the normal way to stop the server
                if (!serverSocket.isClosed()) {
                    log("Failed to accept a connection: " + e.getMessage());
                }
            }
        }

        log("Server stopped");
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
        pool.shutdownNow();
    }

    // Prints a message with the current time and the name of the thread that logged it.
    static void log(String message) {
        String time = LocalTime.now().format(TIME_FORMAT);
        System.out.printf("[%s] [%s] %s%n", time, Thread.currentThread().getName(), message);
    }
}
