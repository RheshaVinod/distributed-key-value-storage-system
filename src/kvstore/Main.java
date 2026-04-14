package kvstore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    
    static void handleClient(Socket client, KVStore store, ServerRole role) {
        try (
            BufferedReader in  = new BufferedReader(
                new InputStreamReader(client.getInputStream()));
            PrintWriter    out = new PrintWriter(
                client.getOutputStream(), true)
        ) {
            CommandHandler handler = new CommandHandler(store, role);
            String line;
            while ((line = in.readLine()) != null) {
                out.println(handler.handle(line));
            }
        } catch (IOException e) {
            System.out.println("Client disconnected");
        }
    }

    public static void main(String[] args) throws IOException {
    if (args.length < 2) {
        System.out.println("Usage: java Main [leader|follower|router] [port]");
        return;
    }

    String mode = args[0].toLowerCase();
    int    port = Integer.parseInt(args[1]);

    // router mode — starts a HashRing + Router, no KVStore needed
    if (mode.equals("router")) {
        HashRing ring = new HashRing();
        ring.addServer("localhost:6379");
        ring.addServer("localhost:6381");
        ring.printRing();
        new Router(port, ring).start();
        return;
    }

    // leader / follower mode — same as before
    ServerRole role = mode.equals("leader")
        ? ServerRole.LEADER
        : ServerRole.FOLLOWER;

    String walFile = "wal-" + port + ".log";

    ReplicationClient replication = null;
    if (role == ServerRole.LEADER) {
        List<String> followers = Arrays.asList("localhost:" + (port + 1));
        replication = new ReplicationClient(followers);
    }

    WalWriter wal   = new WalWriter(walFile);
    KVStore   store = new KVStore(wal, replication);
    WalWriter.replay(walFile, store);

    ExecutorService pool = Executors.newFixedThreadPool(32);
    try (ServerSocket server = new ServerSocket(port)) {
        System.out.println("[" + role + "] Listening on port " + port);
        while (true) {
            Socket client = server.accept();
            pool.submit(() -> handleClient(client, store, role));
        }
    }
}}
    

