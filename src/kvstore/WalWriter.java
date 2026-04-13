package kvstore;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class WalWriter {
    private final BufferedWriter writer;
    public WalWriter(String path) throws IOException {
        writer = Files.newBufferedWriter(
            Paths.get(path),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }
    public synchronized void append(String entry) throws IOException{
        writer.write(entry);
        writer.newLine();
        writer.flush();//adds to disk immediatly
    }
    public static void replay(String path, KVStore store) throws IOException{
        Path p = Paths.get(path);
        if (!Files.exists(p)) return; // no log yet, skip
        System.out.println("Replaying WAL...");
        for (String line : Files.readAllLines(p)) {
            store.applyCommand(line); // re-run each saved command
        }
        System.out.println("WAL replay complete.");
    }
    
}
