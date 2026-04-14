package kvstore;

import java.util.*;

public class HashRing {

    // sorted map of hash position → server address
    // e.g. {12045 → "localhost:6379", 67231 → "localhost:6380"}
    private final TreeMap<Integer, String> ring     = new TreeMap<>();
    private final int                      REPLICAS = 3; // virtual nodes per server

    // add a server to the ring
    public void addServer(String address) {
        for (int i = 0; i < REPLICAS; i++) {
            int hash = hash(address + "-" + i);
            ring.put(hash, address);
            System.out.println("[RING] Added " + address + "-" + i + " at position " + hash);
        }
    }

    // remove a server from the ring
    public void removeServer(String address) {
        for (int i = 0; i < REPLICAS; i++) {
            int hash = hash(address + "-" + i);
            ring.remove(hash);
        }
        System.out.println("[RING] Removed " + address);
    }

    // find which server owns a key
    public String getServer(String key) {
        if (ring.isEmpty()) return null;

        int hash = hash(key);

        // find the first server at or after this hash position
        Map.Entry<Integer, String> entry = ring.ceilingEntry(hash);

        // if none found, wrap around to the first server on the ring
        if (entry == null) {
            entry = ring.firstEntry();
        }

        return entry.getValue();
    }

    private int hash(String key) {
    int h = 17;
    for (char c : key.toCharArray()) {
        h = h * 31 + c;
    }
    return Math.abs(h) % 100_000;
}

    // print the full ring — useful for debugging
    public void printRing() {
        System.out.println("[RING] Current state:");
        ring.forEach((pos, addr) ->
            System.out.println("  " + pos + " → " + addr));
    }
}