# Distributed Key-Value Store

A Redis inspired distributed key-value store built from scratch in Java. Supports concurrent clients, data persistence via a write ahead log, and leader follower replication across multiple nodes.

---

## Features

- **GET / SET / DELETE** commands over a TCP connection
- **Concurrent clients** — thread pool handles up to 16 simultaneous connections
- **Persistence** — write ahead log (WAL) replays data on restart so nothing is lost
- **Leader-follower replication** — every write to the leader is automatically propagated to all followers
- **Fault tolerance** — leader continues operating if a follower goes down
- **Write protection** — followers reject direct writes and redirect clients to the leader

---

## Architecture

```
Client
  │
  ▼
KVServer (TCP, port 6379)
  │
  ├── CommandHandler   (parses GET / SET / DELETE / REPLICATE)
  │
  ├── KVStore          (thread-safe HashMap + ReadWriteLock)
  │     └── WalWriter  (appends every write to wal-{port}.log)
  │
  └── ReplicationClient (fans out writes to followers)
        └── Follower 1 (port 6380)
        └── Follower 2 (port 6381)
```

### Key design decisions

**Write-ahead log (WAL)** — every SET and DELETE is written to disk before the HashMap is updated. On restart the server replays the log line by line to restore state. Flushing on every write is intentionally safe over fast  a production improvement would be batched WAL flushes.

**ReadWriteLock** — multiple threads can read the HashMap simultaneously since reads don't change state. Writes acquire an exclusive lock, blocking all readers. This is more efficient than a single `synchronized` block for read heavy workloads.

**Replication model** — the leader opens a fresh TCP connection to each follower on every write and sends a `REPLICATE SET/DELETE` command. Followers apply it directly to their HashMap via `applyCommand()` without rewriting to their own WAL or triggering further replication, preventing infinite loops.

**Follower reads** — followers serve GET requests directly. This offloads read traffic from the leader at the cost of potential stale reads if replication lags.

---

## Getting started

### Prerequisites

- Java 11 or higher
- A terminal that supports multiple tabs (VS Code terminal, iTerm2, etc.)

### Build

```bash
mkdir -p out
javac -d out src/kvstore/*.java
```

### Run

**Terminal 1 — start the leader:**
```bash
java -cp out kvstore.Main leader 6379
```

**Terminal 2 — start a follower:**
```bash
java -cp out kvstore.Main follower 6380
```

### Connect and use

**Terminal 3 — connect to the leader:**
```bash
nc localhost 6379
```

**Terminal 4 — connect to the follower:**
```bash
nc localhost 6380
```

### Commands

| Command | Example | Response |
|---|---|---|
| `SET key value` | `SET name Alice` | `OK` |
| `GET key` | `GET name` | `Alice` or `(nil)` |
| `DELETE key` | `DELETE name` | `OK` or `(not found)` |
| `PING` | `PING` | `PONG` |

---

## Replication demo

```
# Write to leader (Terminal 3)
SET name Alice    → OK
SET city London   → OK

# Read from follower (Terminal 4)
GET name          → Alice
GET city          → London

# Try writing to follower
SET name Bob      → ERR not the leader — connect to port 6379
```

---

## Persistence demo

```
# Write some data
SET name Alice    → OK

# Stop the server (Ctrl+C) and restart it
java -cp out kvstore.Main leader 6379

# Output on restart:
# Replaying WAL...
# WAL replay complete.

# Data is still there
GET name          → Alice
```

---

## Project structure

```
src/kvstore/
├── Main.java               entry point, TCP server, thread pool
├── KVStore.java            thread-safe HashMap with WAL integration
├── CommandHandler.java     parses and routes client commands
├── WalWriter.java          write ahead log  append and replay
├── ReplicationClient.java  fans writes out to follower nodes
└── ServerRole.java         LEADER / FOLLOWER enum
```

---

## Tradeoffs and future improvements

| Area | Current approach | Production improvement |
|---|---|---|
| WAL flush | Flush on every write (safe, slow) | Batch flushes every N ms |
| Replication | New TCP connection per write | Persistent connection with heartbeat |
| Consistency | Eventual (async replication) | Synchronous replication with quorum |
| Leader election | Manual (set via CLI arg) | Raft consensus algorithm |
| Sharding | Single leader holds all keys | Consistent hashing across multiple leaders |
| Monitoring | Console logs | Prometheus metrics + Grafana dashboard |

---

## What I learned

Building this from scratch gave me direct experience with the core problems in distributed systems:

- **Race conditions** — why every shared data structure needs a lock and what happens when you forget one
- **Fault tolerance** — designing systems that degrade gracefully rather than crash entirely  
- **The WAL pattern** — why write order matters and how databases recover from crashes
- **CAP theorem in practice** — this system prioritises availability over strong consistency. A follower may briefly serve stale data after a write, which is an explicit tradeoff for higher read throughput

---

## Built with

- Java 11+
- No external dependencies — stdlib only
