package kvstore;

public class CommandHandler {
    
    private final KVStore store;
    private final ServerRole role;
    public CommandHandler(KVStore store,ServerRole role){
        this.store=store;
        this.role=role;
    }
    public String handle(String line) {
        if (line == null || line.isBlank()) return "ERR empty";
        String[] parts = line.trim().split(" ", 3);
        String result;
        String cmd = parts[0].toUpperCase();
        if (role == ServerRole.FOLLOWER &&
            (cmd.equals("SET") || cmd.equals("DELETE"))) {
            return "ERR not the leader — connect to port 6379";
        }
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
                case "REPLICATE":
                    String inner = line.trim().substring("REPLICATE ".length());
                    store.applyCommand(inner);
                    result = "OK";
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
