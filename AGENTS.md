# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

Industrial IoT platform built on microservices + Apache Flink + MQTT + TDEngine. Written in Java 21 with Spring Boot 3.2.2.

## Build Commands

```bash
# Build entire project
mvn clean install -DskipTests

# Build specific module
mvn clean install -f iot-system-service/pom.xml -DskipTests

# Run tests (note: skipTests=true is default in parent pom)
mvn test -f iot-system-service/pom.xml

# Run a specific test class
mvn test -Dtest=MyTestClass -f iot-system-service/pom.xml

# Build the Flink Fat JAR
mvn clean package -f iot-flink-job/pom.xml -DskipTests
```

## Architecture

### Service Map

| Module | Port | Purpose |
|--------|------|---------|
| `iot-gateway-service` | 9000 | API Gateway — JWT validation, routing |
| `iot-system-service` | 9002 | User/role/permission management |
| `iot-device-service` | 9001 | Device registry CRUD |
| `iot-integration-service` | 9003 | Third-party protocol adapters |
| `iot-rule-service` | 9004 | Rule CRUD + Flink job orchestration |
| `iot-flink-job` | — | Flink stream processing (Fat JAR deployed to cluster) |
| `iot-data-service` | 9005 | Time-series query gateway |
| `iot-device-simulator-service` | 9006 | Device simulator, random telemetry, multi-protocol sender |

### Common Libraries (`iot-common/`)

- **common-core**: `Result<T>` response wrapper, `BaseController`, Knife4j config, JJWT utilities
- **common-security**: `UserContextHolder`, `@RequirePermission` AOP annotation, `UserContextInterceptor`
- **common-flink**: Shared data models for rule engine (used by both `iot-rule-service` and `iot-flink-job`)
- **common-flink-connector**: Flink source/sink models and connectors

### Request Flow

```
Client → iot-gateway-service (TokenValidationFilter: JWT → headers X-User-Id, X-Username)
       → downstream service (UserContextInterceptor extracts headers into UserContextHolder)
       → controller methods (optional @RequirePermission AOP check)
```

### Flink Data Flow

```
MQTT Devices (Eclipse Paho v5)
  → iot-flink-job DataStream source
  → Window aggregation + rule algorithm evaluation
  → TDEngine sink (time-series storage) / MySQL sink (event log)
        ↑
  Rule configs fetched from iot-rule-service at job startup
```

### Local Rule Data Flow

```
iot-rule-service (local mode)
  → iot-data-service
  → TDEngine query
  → AlgorithmLoader
```

### Key Technology Stack

- **Service discovery & config**: Alibaba Nacos (default: `127.0.0.1:8848`)
- **ORM**: MyBatis Plus 3.5.10.1
- **Database**: MySQL 8.x (`jdbc:mysql://127.0.0.1:3306/bit_iot`)
- **Time-series DB**: TDEngine 3.2.7
- **Stream processing**: Apache Flink 2.2.0
- **Auth**: JWT HS256 via JJWT 0.13.0
- **API docs**: Knife4j 4.5.0 (Swagger 3 / OpenAPI)
- **Logging**: Log4j2 (Logback excluded across all services)
- **Build tooling**: Lombok, Maven Shade plugin (for Flink Fat JAR)

### Package Naming

- Services: `com.bit.iot.<service>.*`
- Common libraries: `bit.iot.<module>.*`

### Known Issues

- `iot-device-service/pom.xml` has incorrect `mainClass` pointing to `GatewayApplication` — should be the device service main class.
- JWT secret key is hardcoded in `TokenUtil.java`; database passwords are in plain `application.yml`.
