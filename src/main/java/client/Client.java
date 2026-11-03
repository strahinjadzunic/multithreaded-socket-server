package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Console client for the server.
 * A separate listener thread prints everything the server sends, while the main thread
 * sends whatever the user types. When the server closes the connection
 * (after "quit" or 3 wrong passwords), the client program exits.
 */
public class Client {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 2026;

    public static void main(String[] args) throws InterruptedException {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        try (Socket socket = new Socket(host, port);
             Scanner keyboard = new Scanner(System.in)) {

            // in/out don't need their own try-with-resources: closing the socket closes them too
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            System.out.println("Connected to " + host + ":" + port);

            Thread listener = new Thread(() -> printServerMessages(in), "server-listener");
            listener.start();

            while (keyboard.hasNextLine()) {
                out.println(keyboard.nextLine());
            }

            // Keyboard input ended (Ctrl+D or the end of piped input). Tell the server we're done
            // sending, but keep reading, so the replies that are still on their way get printed.
            socket.shutdownOutput();
            listener.join();
        } catch (ConnectException e) {
            System.out.println("Could not connect to " + host + ":" + port + ". Is the server running?");
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    // Runs on the listener thread and prints server messages until the connection is closed.
    private static void printServerMessages(BufferedReader in) {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Server: " + line);
            }
        } catch (IOException e) {
            // connection was closed while reading - nothing more to print
        }

        System.out.println("Disconnected from server.");
        // The main thread is still blocked waiting for keyboard input, so stop the whole program.
        System.exit(0);
    }
}
