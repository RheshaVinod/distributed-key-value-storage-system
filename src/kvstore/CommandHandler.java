package kvstore;

public class CommandHandler {
    
    private final KVStore store;
    public CommandHandler(KVStore store){
        this.store=store;
    }
    public String handle(String line) {
        if (line == null || line.isBlank()) return "ERR empty";
        String[] parts = line.trim().split(" ", 3);
        String result;
        String cmd = parts[0].toUpperCase();

        try {
            switch (cmd) {
                case "SET":
                    store.set(parts[1], parts[2]);
                    result = "OK";
                    break;
                case "GET":
                    result = store.get(parts[1]);
                    break;
                case "DELETE":
                    result = store.delete(parts[1]) ? "OK" : "(not found)";
                    break;
                case "PING":
                    result = "PONG";
                    break;
                default:
                    result = "ERR unknown command";
                    break;
            }
        } catch (Exception e) {
            return "ERR " + e.getMessage();
        }

        return result;
    }

}
