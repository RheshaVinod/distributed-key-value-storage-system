package kvstore;

import java.io.IOException;
import java.util.HashMap;

public class KVStore {
    private HashMap<String,String> data = new HashMap<>();// it is kept private so that only this class can access this hashmap but outsiders can use it only throught hte below functions- encapsulation
    WalWriter wal;
    public KVStore(WalWriter wal) {
        this.wal = wal;
    }
    public synchronized  String get(String key)// so that 2 clients dont write at the same time and corrupt the data.
    {
        return data.getOrDefault(key, "(nil)");
    }
    public synchronized void set(String key, String value) throws IOException {
        wal.append("SET " + key + " " + value); // write to disk FIRST
        data.put(key,value);
    }
    public synchronized boolean delete(String key) throws IOException{
    boolean existed = data.remove(key) != null;
        if (existed) wal.append("DELETE " + key);{
        return data.remove(key) != null;
    }
}
    public synchronized void applyCommand(String line) {
        String[] parts = line.trim().split(" ", 3);
       switch (parts[0].toUpperCase()) {
        case "SET":
            data.put(parts[1], parts[2]);
            break;
        case "DELETE":
            data.remove(parts[1]);
            break;
    }


    }

}


