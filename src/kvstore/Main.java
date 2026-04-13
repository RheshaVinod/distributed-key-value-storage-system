package kvstore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketPermission;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    
    static void handleClient(Socket client, KVStore store) {
        try (
            BufferedReader in  = new BufferedReader(
                new InputStreamReader(client.getInputStream()));
            PrintWriter    out = new PrintWriter(
                client.getOutputStream(), true)
        ) {
            CommandHandler handler = new CommandHandler(store);
            String line;
            while ((line = in.readLine()) != null) {
                out.println(handler.handle(line));
            }
        } catch (IOException e) {
            System.out.println("Client disconnected");
        }
    }

    public static void main(String[] args) throws IOException{
        WalWriter wal = new WalWriter("wal.log");;
        KVStore store = new KVStore(wal);
        WalWriter.replay("wal.log", store); 
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try (ServerSocket server = new ServerSocket(6379)){
        System.out.println("Listening to server port 6379 ..");
        while (true){
            Socket client = server.accept();
            System.out.println("Client connected");
            pool.submit(() -> handleClient(client, store));
        }
        
       
        }
        
    }}
    

