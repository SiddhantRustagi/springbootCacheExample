# Redis Cache Sharding + H2 Database — Updated Plan v3

## Changelog (All changes since branch clone)

### ✅ Fully Completed

| # | Change | File | Details |
|---|---|---|---|
| 1 | Added `spring-boot-starter-data-redis` dependency | [pom.xml](file:///d:/workspace/MyLearning/springbootCacheExample/pom.xml) | Lines 58-62 |
| 2 | Added `h2` database dependency | [pom.xml](file:///d:/workspace/MyLearning/springbootCacheExample/pom.xml) | Lines 63-68 |
| 3 | Fixed indentation on new dependencies | [pom.xml](file:///d:/workspace/MyLearning/springbootCacheExample/pom.xml) | Aligned with existing `<dependency>` blocks |
| 4 | Renamed config to `.bak` | `application.properties` → `application.properties.bak` | Spring no longer auto-loads it |
| 5 | Created YAML config file | [springCache-dev.yml](file:///d:/workspace/MyLearning/springbootCacheExample/src/main/resources/springCache-dev.yml) | H2, JPA, H2 console, Redis shards, cache type |
| 6 | Set custom config name in main class | [SpringbootCacheExampleApplication.java](file:///d:/workspace/MyLearning/springbootCacheExample/src/main/java/com/redis/SpringbootCacheExampleApplication.java) | `Map.of("spring.config.name", "springCache-dev")` |
| 7 | Added `import java.util.Map` | [SpringbootCacheExampleApplication.java](file:///d:/workspace/MyLearning/springbootCacheExample/src/main/java/com/redis/SpringbootCacheExampleApplication.java) | Line 3 |
| 8 | Added learning comments for alt config methods | [SpringbootCacheExampleApplication.java](file:///d:/workspace/MyLearning/springbootCacheExample/src/main/java/com/redis/SpringbootCacheExampleApplication.java) | Lines 17-24 |
| 9 | Removed stray `\` character | [SpringbootCacheExampleApplication.java](file:///d:/workspace/MyLearning/springbootCacheExample/src/main/java/com/redis/SpringbootCacheExampleApplication.java) | Was on line 24 |
| 10 | Removed `DataSourceAutoConfiguration` exclusion | [SpringbootCacheExampleApplication.java](file:///d:/workspace/MyLearning/springbootCacheExample/src/main/java/com/redis/SpringbootCacheExampleApplication.java) | Removed the `exclude` parameter and its import |
| 11 | Uncommented `@Entity` | [Product.java](file:///d:/workspace/MyLearning/springbootCacheExample/src/main/java/com/redis/entity/Product.java) | Line 8 |
| 12 | Uncommented `@GeneratedValue(strategy = GenerationType.AUTO)` | [Product.java](file:///d:/workspace/MyLearning/springbootCacheExample/src/main/java/com/redis/entity/Product.java) | Line 12 |
| 13 | Created `docker-compose.yml` with 2 Redis nodes | [docker-compose.yml](file:///d:/workspace/MyLearning/springbootCacheExample/docker-compose.yml) | Ports 6379 + 6380 |
| 14 | Commented out `app` service in Docker Compose | [docker-compose.yml](file:///d:/workspace/MyLearning/springbootCacheExample/docker-compose.yml) | Running app locally during dev |
| 15 | Fixed Dockerfile port from 8080 → 8089 | [Dockerfile](file:///d:/workspace/MyLearning/springbootCacheExample/Dockerfile) | Line 14 |
| 16 | Fixed duplicate redis host/port in YAML | [springCache-dev.yml](file:///d:/workspace/MyLearning/springbootCacheExample/src/main/resources/springCache-dev.yml) | Changed to `redis.shards` list |
| 17 | Started `redis-node-1` Docker container | Docker | Running on port 6380 |

---

## Current Issues Still to Fix

### 🔴 Issue 1: [Product.java](file:///d:/workspace/MyLearning/springbootCacheExample/src/main/java/com/redis/entity/Product.java) — Missing `Serializable` + no-arg constructor

**What's wrong:** Redis serializes objects to byte arrays. Without `Serializable`, you'll get a serialization error at runtime when caching a `Product`. Also, JPA needs a no-arg constructor.

**What to do:**
1. Add `implements Serializable` to the class declaration
2. Add `private static final long serialVersionUID = 1L;` as the first field
3. Add `import java.io.Serializable;`
4. Add a no-arg constructor: `public Product() {}`

---

### 🟡 Issue 2: [SpringbootCacheExampleApplication.java](file:///d:/workspace/MyLearning/springbootCacheExample/src/main/java/com/redis/SpringbootCacheExampleApplication.java) — Redundant `@EnableAutoConfiguration()`

Line 12 has `@EnableAutoConfiguration()` with empty parentheses. This is harmless but redundant — `@SpringBootApplication` already includes `@EnableAutoConfiguration`. You can safely delete line 12 and remove the import on line 6.

---

### 🟡 Issue 3: [springCache-dev.yml](file:///d:/workspace/MyLearning/springbootCacheExample/src/main/resources/springCache-dev.yml) — `redis.shards` indentation

Currently `redis:` is at **2-space indent** (lines 23-28), which means it's `spring.redis.shards` — it's nested under `spring:`. For custom properties, this should be at **root level** (0-indent) so it becomes just `redis.shards`.

Current (wrong):
```yaml
spring:
  # ...other config...
  redis:           # ← indented = spring.redis.shards
    shards:
      - host: localhost
        port: 6379
```

Should be:
```yaml
spring:
  # ...other config...
  cache:
    type: redis

# Custom properties — at root level
redis:             # ← no indent = redis.shards
  shards:
    - host: localhost
      port: 6379
    - host: localhost
      port: 6380

server:
  port: 8089
```

---

## Dockerfile — Multi-Stage Build Explained

Your current Dockerfile has one problem: it requires you to run `mvn package` **before** building the Docker image (because it does `COPY target/...jar`). A multi-stage build solves this — Maven runs **inside Docker**.

### How it works

A multi-stage Dockerfile has multiple `FROM` lines. Each `FROM` starts a new "stage". You can copy files between stages. The final image only keeps the last stage.

```
┌─────────────────────────────────┐
│  STAGE 1: "build"               │
│  Image: maven:3.9-eclipse-      │
│         temurin-17              │
│                                 │
│  1. Copy pom.xml                │
│  2. Download dependencies       │
│  3. Copy source code            │
│  4. Run mvn package → JAR       │
│                                 │
│  Result: target/*.jar exists    │
│  (but stage is ~800MB with      │
│   Maven + source + deps)        │
└──────────────┬──────────────────┘
               │ COPY --from=build
               ▼
┌─────────────────────────────────┐
│  STAGE 2: final (runtime)       │
│  Image: eclipse-temurin-17-jre  │
│                                 │
│  1. Copy ONLY the JAR from      │
│     stage 1                     │
│  2. Run java -jar app.jar       │
│                                 │
│  Result: ~300MB image with      │
│  only the JRE + your JAR        │
└─────────────────────────────────┘
```

### What to write

Replace your entire Dockerfile with this:

```dockerfile
# ── STAGE 1: Build ──
# Uses a Maven image that already has Java 17 + Maven installed
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy ONLY pom.xml first and download dependencies
# Why? Docker caches layers. If pom.xml hasn't changed, 
# Docker skips the dependency download on subsequent builds.
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN mvn dependency:resolve -B

# Now copy source code and build
# This layer is only re-run when YOUR code changes
COPY src ./src
RUN mvn package -DskipTests -B

# ── STAGE 2: Run ──
# Uses a smaller JRE-only image (no Maven, no source code)
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy ONLY the built JAR from stage 1
COPY --from=build /app/target/springbootCacheExample-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]

EXPOSE 8089
```

### Key concepts

| Concept | Explanation |
|---|---|
| `FROM ... AS build` | Names the first stage "build" so stage 2 can reference it |
| `COPY pom.xml .` before `COPY src ./src` | **Layer caching trick** — Docker re-runs a layer only if the copied files changed. By copying `pom.xml` first and resolving deps, dependency download is cached as long as `pom.xml` doesn't change. Your code changes (in `src/`) only trigger the `mvn package` step. |
| `COPY --from=build` | Copies a file from the "build" stage into the current stage. Only the JAR crosses over — Maven, source code, `.class` files are all discarded. |
| `eclipse-temurin:17-jre` vs `openjdk:17-jdk-slim` | JRE is smaller (~300MB vs ~400MB) because it doesn't include the Java compiler. You only need the compiler in stage 1. |
| `-DskipTests -B` | `-DskipTests` skips running tests in Docker (run them locally). `-B` is batch mode (no interactive prompts). |

### After updating the Dockerfile

You can uncomment the `app` service in `docker-compose.yml` and run everything with:

```bash
docker-compose up --build
```

This will: build your app (Maven inside Docker) → start both Redis nodes → start your app. No local Java/Maven needed.

---

## Remaining Steps

| # | What | File | Status |
|---|---|---|---|
| 18 | Fix `Product.java` — add `Serializable` + no-arg constructor | [Product.java](file:///d:/workspace/MyLearning/springbootCacheExample/src/main/java/com/redis/entity/Product.java) | 🔲 |
| 19 | Fix `springCache-dev.yml` — move `redis.shards` to root level | [springCache-dev.yml](file:///d:/workspace/MyLearning/springbootCacheExample/src/main/resources/springCache-dev.yml) | 🔲 |
| 20 | Remove redundant `@EnableAutoConfiguration()` | [SpringbootCacheExampleApplication.java](file:///d:/workspace/MyLearning/springbootCacheExample/src/main/java/com/redis/SpringbootCacheExampleApplication.java) | 🔲 |
| 21 | Update Dockerfile to multi-stage build | [Dockerfile](file:///d:/workspace/MyLearning/springbootCacheExample/Dockerfile) | 🔲 |
| 22 | Create `ProductRepository` interface | `com.redis.repository.ProductRepository.java` [NEW] | 🔲 |
| 23 | Create `RedisCacheConfig` with 2 `LettuceConnectionFactory` beans | `com.redis.config.RedisCacheConfig.java` [NEW] | 🔲 |
| 24 | Create `ShardCacheResolver` for `id % 2` routing | `com.redis.config.ShardCacheResolver.java` [NEW] | 🔲 |
| 25 | Create `MaxKeysCache` decorator for 5-key limit | `com.redis.config.MaxKeysCache.java` [NEW] | 🔲 |
| 26 | Rewrite `ProductService` — use repository + shard resolver | [ProductService.java](file:///d:/workspace/MyLearning/springbootCacheExample/src/main/java/com/redis/service/ProductService.java) | 🔲 |
| 27 | Add `CommandLineRunner` to seed H2 on startup | [SpringbootCacheExampleApplication.java](file:///d:/workspace/MyLearning/springbootCacheExample/src/main/java/com/redis/SpringbootCacheExampleApplication.java) | 🔲 |
| 28 | Update controller to return created product | [ProductController.java](file:///d:/workspace/MyLearning/springbootCacheExample/src/main/java/com/redis/controller/ProductController.java) | 🔲 |
| 29 | Start both Redis nodes | `docker-compose up -d redis-node-0 redis-node-1` | 🔲 |
| 30 | Run app & test full flow | — | 🔲 |
