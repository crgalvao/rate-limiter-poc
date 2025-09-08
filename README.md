Distributed High-Throughput Rate Limiter API
📖 Overview

This project implements a distributed, high-throughput rate limiter in Java 21 using Spring Boot.
It was designed as a coding challenge but structured with a production-ready architecture for clarity, maintainability, and extensibility.

The rate limiter:

Supports 100M+ requests/minute across a fleet of servers.

Uses local batching with scheduled flushes to minimize network calls.

Ensures thread safety with LongAdder and ConcurrentHashMap.

Provides approximate accuracy: clients always get at least their configured limit, but may exceed it slightly.

Cleanly separates domain logic, application services, infrastructure, and API Controller.

🏗️ Architecture
domain/         → Business logic (rate limiter, KV store interface)
application/    → Service layer (uses rate limiter, applies configs)
infrastructure/ → Technical details (InMemory, Redis-ready placeholder)
controller/   → REST API controllers
config/         → Centralized client limit configuration


This structure follows Clean Architecture / Hexagonal principles:

Domain: pure Java, no framework dependencies.

Application: orchestrates use cases.

Infrastructure: provides implementations (in-memory by default).

Controller: REST API endpoints.

Config: loads client limits from YAML.

⚙️ Configuring Clients and Limits

Client IDs and rate limits are configured in src/main/resources/application.yml:

ratelimiter:
  clients:
    - id: "xyz"
      limit: 500
    - id: "abc"
      limit: 200
    - id: "premiumUser"
      limit: 2000


Limits are requests per 60 seconds.

Unknown clients are rejected by default.

This can later be extended to load from a database or a config service.

🚀 Running the Project
Option 1: Single Docker Command

Build and run in one line:

docker build -t ratelimiter-api . && docker run -p 8080:8080 ratelimiter-api

Option 2: Docker Compose

With the provided docker-compose.yml:

docker compose up --build


Both methods expose the API at:

http://localhost:8080/ratelimit?clientId=xyz

🌐 API Usage

Example request:

curl "http://localhost:8080/ratelimit?clientId=xyz"


Example response:

Client xyz allowed? true

✅ Testing

Run the full test suite:

mvn test

Coverage

Domain tests: batching, flush behavior, backend failures.

Application tests: client configs, unknown client handling.

Controller tests: REST API endpoints (MockMvc).

Concurrency stress tests: 10,000+ requests with multiple threads.

🔮 Future Enhancements

Add RedisDistributedKeyValueStore for real distributed production.

Expose metrics (Prometheus, Micrometer).

Support sliding window and token bucket algorithms.

Add management endpoints for updating limits dynamically.

💡 This project demonstrates a scalable, fault-tolerant, and maintainable rate limiter suitable for distributed systems, while remaining easy to run locally with a single Docker command or Docker Compose.