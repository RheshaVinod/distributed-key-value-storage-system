package kvstore;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Router {

    private final int             port;
    private final HashRing        ring;
    private final ExecutorService pool = Executors.newFixedThreadPool(16);

    public Router(int port, HashRing ring) {
        this.port = port;
        this.ring = ring;
    }

    public void start() throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[ROUTER] Listening on port " + port);
            while (true) {
                Socket client = server.accept();
                pool.submit(() -> handle(client));
            }
        }
    }

    private void handle(Socket client) {
        try (
            BufferedReader in  = new BufferedReader(
                new InputStreamReader(client.getInputStream()));
            PrintWriter    out = new PrintWriter(
                client.getOutputStream(), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                String response = forward(line);
                out.println(response);
            }
        } catch (IOException e) {
            System.out.println("[ROUTER] Client disconnected");
        }
    }

    private String forward(String line) {
        if (line == null || line.isBlank()) return "ERR empty";
        String[] parts = line.trim().split(" ", 3);
        String   cmd   = parts[0].toUpperCase();

        // PING doesn't need a key — just respond directly
        if (cmd.equals("PING")) return "PONG";

        // all other commands need a key to route
        if (parts.length < 2) return "ERR missing key";

        String key    = parts[1];
        String server = ring.getServer(key);

        if (server == null) return "ERR no servers available";

        System.out.println("[ROUTER] " + cmd + " '" + key + "' → " + server);

        // forward the command to the correct server and return its response
        return sendToServer(server, line);
    }

    private String sendToServer(String address, String command) {
        try {
            String[] parts = address.split(":");
            try (
                Socket         s   = new Socket(parts[0],
                    Integer.parseInt(parts[1]));
                PrintWriter    out = new PrintWriter(
                    s.getOutputStream(), true);
                BufferedReader in  = new BufferedReader(
                    new InputStreamReader(s.getInputStream()))
            ) {
                out.println(command);
                return in.readLine();
            }
        } catch (IOException e) {
            return "ERR server unreachable: " + address;
        }
    }
}