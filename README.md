# Bit Industrial IoT

基于 Spring Boot 3.2.2、Spring Cloud Alibaba、Apache Flink、MQTT、TDEngine 的工业物联网平台。

## 环境要求

- JDK 21
- Maven 3.6+
- MySQL 8.x
- Nacos 2.x
- TDEngine 3.2.7
- Flink 2.2.0
- MQTT Broker

> 当前工程使用 Java 21。构建前请确认 `JAVA_HOME` 指向 JDK 21。

## 模块结构

| 模块 | 端口 | 说明 |
| --- | --- | --- |
| `iot-gateway-service` | 9000 | API 网关，统一 JWT 校验和路由 |
| `iot-system-service` | 9002 | 用户、角色、权限、登录 |
| `iot-device-service` | 9001 | 设备、目录、事件、测点管理 |
| `iot-integration-service` | 9003 | 第三方集成与插件管理 |
| `iot-rule-service` | 9004 | 规则配置、算法管理、Flink 编排 |
| `iot-data-service` | 9005 | 时序查询网关，统一 TDEngine 查询能力 |
| `iot-device-simulator-service` | 9006 | 设备模拟、随机时序生成、协议发送任务 |
| `iot-flink-job` | - | Flink 可执行 Job Fat JAR |
| `iot-common/common-core` | - | 通用响应、异常、JWT 工具 |
| `iot-common/common-security` | - | 用户上下文与权限切面 |
| `iot-common/common-flink` | - | 规则/Flink 共享模型与算法接口 |
| `iot-common/common-flink-connector` | - | Flink Source/Sink/窗口工具 |

## 核心链路

### 请求链路

```text
Client
  -> iot-gateway-service
  -> downstream service
  -> UserContextInterceptor
  -> controller / service
```

### 规则执行链路

```text
rule-config
  -> iot-rule-service
  -> FlinkJobManager
  -> Flink REST API
  -> iot-flink-job
  -> MQTT / TDEngine Source
  -> Window
  -> Algorithm
  -> TDEngine / MySQL Sink
```

### 本地调试链路

```text
iot-rule-service (local mode)
  -> iot-data-service
  -> TDEngine query
  -> AlgorithmLoader
  -> rule_execution_log
```

## 主要能力

- 双模式规则运行：`flink` 生产模式、`local` 调试模式
- `iot-data-service` 统一承接时序查询，供本地规则模式和后续高频查询复用
- 算法上传支持 JAR 和 Python，带文件类型/大小与路径校验
- 插件上传仅允许受控 JAR，外部接口不再开放任意方法调用
- 四个业务服务的 Controller 已切换为 request/vo 边界，避免直接暴露 Entity
- `iot-device-simulator-service` 支持模拟设备、测点、发送任务管理，并按频率生成随机时序数据
- 协议发送通道支持 HTTP、MQTT、Kafka，Modbus 采用寄存器快照模拟并提供查询接口

## 关键配置

### `iot-rule-service`

- `rule.engine.type`: `flink` / `local`
- `rule.algorithm.upload-path`: 本地算法存储目录
- `rule.algorithm.shared-path`: Flink 可访问的共享算法目录
- `data.service.base-url`: `iot-data-service` 访问地址
- `tdengine.result-stable`: 规则结果超级表名

### `iot-data-service`

- `tdengine.jdbc-url`
- `tdengine.username`
- `tdengine.password`
- `tdengine.super-table`
- `tdengine.max-limit-per-query`

### `iot-device-simulator-service`

- `simulator.scheduler.pool-size`
- `simulator.http.connect-timeout-seconds`
- `simulator.http.read-timeout-seconds`
- `spring.datasource.*`

### `iot-integration-service`

- `plugin.upload.path`
- `plugin.upload.max-file-size-bytes`

## 新增接口

### `iot-data-service`

- `POST /iot/timeseries/query/raw`
- `POST /iot/timeseries/query/rule-window`
- `GET /iot/timeseries/health`

### `iot-device-simulator-service`

- `GET /iot/simulator/device/list`
- `POST /iot/simulator/device`
- `GET /iot/simulator/point/list?deviceId=...`
- `POST /iot/simulator/task`
- `PUT /iot/simulator/task/{id}/start`
- `PUT /iot/simulator/task/{id}/stop`
- `PUT /iot/simulator/task/{id}/trigger`
- `GET /iot/simulator/data/latest?deviceId=...`
- `POST /iot/simulator/data/history`
- `GET /iot/simulator/data/send-log`
- `GET /iot/simulator/modbus/{deviceId}/registers`

## 构建

```bash
# 全量构建
mvn clean install -DskipTests

# 构建规则服务
mvn clean install -f iot-rule-service/pom.xml -DskipTests

# 构建数据服务
mvn clean install -f iot-data-service/pom.xml -DskipTests

# 构建设备模拟服务
mvn clean install -f iot-device-simulator-service/pom.xml -DskipTests

# 构建 Flink Job Fat JAR
mvn clean package -f iot-flink-job/pom.xml -DskipTests
```

## 启动顺序

1. 启动 Nacos
2. 启动 MySQL
3. 启动 TDEngine
4. 启动 MQTT Broker
5. 启动 `iot-data-service`
6. 启动 `iot-system-service`、`iot-device-service`、`iot-integration-service`、`iot-device-simulator-service`
7. 启动 `iot-rule-service`
8. 启动 `iot-gateway-service`
9. 准备 Flink 集群并构建/上传 `iot-flink-job`

## 当前实现说明

- `iot-auth-service` 已从聚合模块中移除，登录仍由 `iot-system-service` 承担
- 空壳公共模块 `common-feign`、`common-mq`、`common-tdengine` 已移出聚合构建
- 规则执行日志写入采用 `window_key` 语义，数据库需要同步补充对应字段和唯一索引

  建议唯一索引：`(rule_id, window_key, start_time, end_time)`
- 设备模拟模块初始化 SQL 位于 [device_simulator.sql](/Users/chenhao/IdeaProjects/bit-industrial-iot/iot-device-simulator-service/src/main/resources/sql/device_simulator.sql)
- Modbus 当前实现为寄存器快照模拟，用于单机联调；若需要真实 Modbus TCP Slave，可在该模块协议层继续扩展
