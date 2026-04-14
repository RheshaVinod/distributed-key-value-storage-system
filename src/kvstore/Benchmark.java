package kvstore;

import java.io.*;
import java.net.*;

public class Benchmark {

    private final String host;
    private final int    port;
    private final int    ops;

    public Benchmark(String host, int port, int ops) {
        this.host = host;
        this.port = port;
        this.ops  = ops;
    }

    public void run() throws IOException {
        System.out.println("Benchmarking " + host + ":" + port
            + " with " + ops + " ops each\n");

        runBenchmark("SET");
        runBenchmark("GET");
    }

    private void runBenchmark(String type) throws IOException {
        try (
            Socket         s   = new Socket(host, port);
            PrintWriter    out = new PrintWriter(s.getOutputStream(), true);
            BufferedReader in  = new BufferedReader(
                new InputStreamReader(s.getInputStream()))
        ) {
            // warm up — 100 ops before measuring
            for (int i = 0; i < 100; i++) {
                out.println("SET warmup" + i + " val");
                in.readLine();
            }

            long   start      = System.currentTimeMillis();
            long   totalNanos = 0;

            for (int i = 0; i < ops; i++) {
                String cmd = type.equals("SET")
                    ? "SET key" + i + " value" + i
                    : "GET key" + i;

                long t0 = System.nanoTime();
                out.println(cmd);
                in.readLine();
                totalNanos += System.nanoTime() - t0;
            }

            long   elapsed   = System.currentTimeMillis() - start;
            double opsPerSec = ops * 1000.0 / elapsed;
            double avgMs     = (totalNanos / 1_000_000.0) / ops;

            System.out.printf("%-6s  ops/sec: %8.0f  avg latency: %.3f ms%n",
                type, opsPerSec, avgMs);
        }
    }

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int    port = args.length > 1 ? Integer.parseInt(args[1]) : 6379;
        int    ops  = args.length > 2 ? Integer.parseInt(args[2]) : 10000;

        new Benchmark(host, port, ops).run();
    }
}