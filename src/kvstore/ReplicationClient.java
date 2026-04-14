package kvstore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReplicationClient {
    
    private final List<String> followers;
    private final Map<String,Boolean> aliveMap= new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    public ReplicationClient(List<String> followers) {
        this.followers = followers;
        for (String f : followers) aliveMap.put(f, true);
        startHeartbeat();
    }
    private void startHeartbeat(){
        scheduler.scheduleAtFixedRate(()->{
            for (String address : followers){
                boolean alive = ping(address);
                boolean wasAlive = aliveMap.getOrDefault(address, false);
                if (!alive && wasAlive) {
                    System.out.println("[HEARTBEAT] Follower DOWN: " + address);
                }
                if (alive && !wasAlive) {
                    System.out.println("[HEARTBEAT] Follower BACK UP: " + address);
                }
                aliveMap.put(address, alive);
            }
        },5, 10, TimeUnit.SECONDS);
    
    }
    private boolean ping(String address) {
    try {
        String[] parts = address.split(":");
        Socket s = new Socket();
        s.connect(new InetSocketAddress(parts[0], 
            Integer.parseInt(parts[1])), 1000);
        s.setSoTimeout(1000);
        try (
            PrintWriter    out = new PrintWriter(s.getOutputStream(), true);
            BufferedReader in  = new BufferedReader(
                new InputStreamReader(s.getInputStream()))
        ) {
            out.println("PING");
            String response = in.readLine();
            s.close();
            return "PONG".equals(response);
        }
    } catch (IOException e) {
        return false;
    }
}


    public void replicate(String command) {
    for (String address : followers) {
        try {
            String[] parts = address.split(":");
            Socket s = new Socket();
            s.connect(new InetSocketAddress(parts[0],
                Integer.parseInt(parts[1])), 2000);
            s.setSoTimeout(2000);
            try (
                PrintWriter    out = new PrintWriter(s.getOutputStream(), true);
                BufferedReader in  = new BufferedReader(
                    new InputStreamReader(s.getInputStream()))
            ) {
                out.println("REPLICATE " + command);
                String response = in.readLine();
                System.out.println("[REPLICATION] " + address + " → " + response);
                s.close();
            }
        } catch (IOException e) {
            System.out.println("[REPLICATION] Failed to reach: " + address + " - " + e.getMessage());
        }
    }
}
    public boolean isAlive(String address) {
        return aliveMap.getOrDefault(address, false);
    }
    }

