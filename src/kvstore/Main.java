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

    public static void main(String[] args) throws IOException{
        if (args.length < 2) {
        System.out.println("Usage: java Main [leader|follower] [port]");
        return;
    }
    ServerRole role = args[0].equalsIgnoreCase("leader")
        ? ServerRole.LEADER
        : ServerRole.FOLLOWER;

    int port = Integer.parseInt(args[1]);
    String walFile = "wal-" + port + ".log";
    ReplicationClient replication = null;
    if (role == ServerRole.LEADER) {
        List<String> followers = Arrays.asList("localhost:6380");
        replication = new ReplicationClient(followers);
        System.out.println("Replicating to: " + followers);
    }


        WalWriter wal = new WalWriter(walFile);;
        KVStore store = new KVStore(wal,replication);
        WalWriter.replay(walFile, store); 
        ExecutorService pool = Executors.newFixedThreadPool(32);
        try (ServerSocket server = new ServerSocket(port)){
        System.out.println("[" + role + "] Listening on port " + port);
        while (true){
            Socket client = server.accept();
           System.out.println("[" + role + "] Client connected");
            pool.submit(() -> handleClient(client, store,role));
        }
        
       
        }
        
    }}
    

