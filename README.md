# Distributed Key-Value Store

A Redis inspired distributed key value store built from scratch in Java no external dependencies. Supports concurrent clients, data persistence via a write ahead log, leader follower replication, and consistent hashing based sharding across multiple nodes.

---

## Performance

Benchmarked on localhost with 10,000 operations (single node) and 1,000 operations (sharded via router):

| | Single node | Sharded via router |
|---|---|---|
| SET ops/sec | 6,028 | 3,846 |
| GET ops/sec | 42,918 | 6,993 |
| SET latency | 0.165ms | 0.256ms |
| GET latency | 0.023ms | 0.141ms |

Single node
<img width="756" height="116" alt="image" src="https://github.com/user-attachments/assets/4f96c227-57e6-4579-b815-137702502033" />

Router
<img width="762" height="118" alt="image" src="https://github.com/user-attachments/assets/f44945e0-42d5-488a-abb3-17c8cf6e639a" />



GET is ~7x faster than SET on a single node because SET writes to the write ahead log on every operation. The router adds ~2x overhead because each command makes two TCP hops (client→router, router→shard) instead of one.

---

## Architecture

```
         Client
           │
           ▼
      Router :8000
    (consistent hash ring)
      ↙            ↘
Leader :6379     Leader :6381
  (Shard 1)        (Shard 2)
     │                 │
     ▼                 ▼
Follower :6380    Follower :6382
     │                 │
     ▼                 ▼
wal-6379.log      wal-6381.log
```

### Key design decisions

**Write-ahead log (WAL)** — every SET and DELETE is written to disk before the HashMap is updated. On restart the server replays the log to restore state. Flushing on every write is intentionally safe over fast  a production improvement would be batched WAL flushes with configurable durability guarantees.

**Synchronized methods** — every KVStore method is `synchronized` so multiple threads cannot corrupt the HashMap. A `ReadWriteLock` would allow concurrent reads for higher throughput on read-heavy workloads  noted as a future improvement.

**Replication model** — the leader opens a TCP connection to each follower on every write and sends a `REPLICATE` command. Followers call `applyCommand()` directly  no WAL write, no further replication  preventing infinite loops.

**Consistent hashing** — the router uses a hash ring with virtual nodes to map keys to shards. When a shard is added or removed, only the keys in the affected range need to move. A regular `key % numShards` approach would reassign almost all keys on any topology change.

**Follower reads** — followers serve GET requests directly, offloading read traffic from leaders at the cost of potential stale reads if replication lags.

---

## Features

- GET / SET / DELETE commands over TCP
- PING / PONG health check
- Concurrent clients — thread pool handles multiple simultaneous connections
- Persistence — WAL replays data on restart
- Leader-follower replication — writes propagate to followers automatically
- Fault tolerance — leader keeps running if a follower goes down
- Write protection — followers reject direct writes
- Sharding — consistent hash ring distributes keys across multiple leader nodes
- Router — single entry point, clients never need to know about sharding

---

## Getting started

### Prerequisites

- Java 11 or higher
- Multiple terminal tabs (VS Code terminal, iTerm2)

### Build

```bash
mkdir -p out
javac -d out src/kvstore/*.java
```

### Run the full cluster

```bash
# Shard 1
java -cp out kvstore.Main leader   6379
java -cp out kvstore.Main follower 6380

# Shard 2
java -cp out kvstore.Main leader   6381
java -cp out kvstore.Main follower 6382

# Router
java -cp out kvstore.Main router   8000
```

### Connect

```bash
nc localhost 8000
```

### Commands

| Command | Example | Response |
|---|---|---|
| `SET key value` | `SET name Alice` | `OK` |
| `GET key` | `GET name` | `Alice` or `(nil)` |
| `DELETE key` | `DELETE name` | `OK` or `(not found)` |
| `PING` | `PING` | `PONG` |

---

## Demo

```bash
# Connect to router
nc localhost 8000

SET name Alice      # → OK  (routed to correct shard automatically)
SET city London     # → OK  (may go to a different shard)
GET name            # → Alice
GET city            # → London

# Try writing directly to a follower
nc localhost 6380
SET name Bob        # → ERR not the leader — connect to port 6379

# Verify replication — read from follower
nc localhost 6380
GET name            # → Alice  (replicated from leader)

# Restart the leader — data survives
# Ctrl+C on leader terminal, then:
java -cp out kvstore.Main leader 6379
# Output: Replaying WAL... WAL replay complete.
nc localhost 6379
GET name            # → Alice  (restored from WAL)
```

---

## Benchmark

```bash
# Single node
java -cp out kvstore.Benchmark localhost 6379 10000

# Sharded via router
java -cp out kvstore.Benchmark localhost 8000 1000
```

---

## Project structure

```
src/kvstore/
├── Main.java               entry point- leader, follower, or router mode
├── KVStore.java            thread-safe HashMap with WAL integration
├── CommandHandler.java     parses and routes client commands
├── WalWriter.java          write ahead log append and replay on boot
├── ReplicationClient.java  fans writes out to follower nodes
├── ServerRole.java         LEADER / FOLLOWER enum
├── HashRing.java           consistent hash ring with virtual nodes
├── Router.java             single entry point, forwards commands to shards
└── Benchmark.java          load testing  measures ops/sec and latency
```

---

## Tradeoffs and future improvements

| Area | Current | Production improvement |
|---|---|---|
| WAL flush | Per write (safe, slow) | Batched flushes, configurable durability |
| Locking | synchronized (simple) | ReadWriteLock for concurrent reads |
| Replication | New TCP connection per write | Persistent connection per follower |
| Consistency | Eventual (async) | Synchronous with quorum acknowledgement |
| Leader election | Manual CLI flag | Raft consensus algorithm |
| Router | Single point of failure | Replicated router with failover |
| Monitoring | Console logs | Prometheus metrics + Grafana dashboard |

---

## What I learned

- **Race conditions** — why every shared data structure needs a lock and what breaks without one
- **WAL pattern** — why write order to disk matters and how databases recover from crashes
- **Consistent hashing** — why `key % n` breaks on topology changes and how a hash ring fixes it
- **CAP theorem in practice** — this system prioritises availability over strong consistency; followers may briefly serve stale reads after a write, which is an explicit tradeoff for higher read throughput
- **Distributed fault tolerance** — designing components that degrade gracefully rather than taking down the whole system

---

## Built with

- Java 11+
- No external dependencies — stdlib only
