# 规则引擎 × Flink 融合详细设计文档

> 版本：1.0
> 日期：2026-03-27
> 作者：chenhao

---

## 一、现状分析与问题

### 1.1 当前架构

```
Controller → Service → RuleEngineManager（ScheduledExecutorService 线程池）
                                ↓
                        AlgorithmLoader（URLClassLoader / Python 子进程）
                                ↓
                        TDEngine 数据读取（TODO，未实现）
```

### 1.2 核心问题

| # | 问题 | 影响 |
|---|------|------|
| 1 | 用 Java 线程池调度，不是流计算引擎 | 无法处理高吞吐实时流数据，无水位线（Watermark）、无窗口语义 |
| 2 | TDEngine 数据读取为空占位 | 规则执行实际无数据输入 |
| 3 | 算法在 Spring Boot 进程内执行 | 无资源隔离，算法 OOM 会拖垮整个服务 |
| 4 | 无 Checkpoint / Savepoint | 故障后无法恢复，数据丢失 |
| 5 | 多设备多测点并行度为 1 | 无法水平扩展 |

---

## 二、目标架构总览

### 2.1 系统全景

```
┌──────────────────────────────────────────────────────────────────┐
│                    iot-flink-service (Spring Boot :9004)          │
│                                                                  │
│  ┌────────────┐  ┌────────────────┐  ┌───────────────────────┐  │
│  │ Controller  │→│    Service      │→│  FlinkJobManager      │  │
│  │ (REST API)  │  │ (规则CRUD)      │  │ (Flink REST Client)   │  │
│  └────────────┘  └────────────────┘  └───────────┬───────────┘  │
│                                                   │              │
│                            ┌──────────────────────┘              │
│                            │  HTTP (Flink REST API)              │
└────────────────────────────┼────────────────────────────────────┘
                             ↓
┌──────────────────────────────────────────────────────────────────┐
│                    Flink Cluster（独立部署）                        │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    Flink Job（每条规则一个 Job）             │   │
│  │                                                          │   │
│  │  ┌────────────┐   ┌──────────────┐   ┌──────────────┐   │   │
│  │  │ TDEngine   │ → │  Window      │ → │ Algorithm    │   │   │
│  │  │ Source     │   │  Assigner    │   │ ProcessFn    │   │   │
│  │  │ (JDBC轮询/ │   │ (Tumbling/   │   │ (JAR/Python  │   │   │
│  │  │  CDC/MQTT) │   │  Sliding/    │   │  动态加载)    │   │   │
│  │  └────────────┘   │  Session)    │   └──────┬───────┘   │   │
│  │                   └──────────────┘          │           │   │
│  │                                             ↓           │   │
│  │                                   ┌──────────────┐      │   │
│  │                                   │  Result Sink │      │   │
│  │                                   │ (TDEngine/   │      │   │
│  │                                   │  Kafka/MySQL)│      │   │
│  │                                   └──────────────┘      │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  TaskManager-1   TaskManager-2   TaskManager-3  ...             │
└──────────────────────────────────────────────────────────────────┘
```

### 2.2 职责划分

| 组件 | 职责 | 技术选型 |
|------|------|----------|
| **iot-flink-service** | 规则管理（CRUD）、算法管理、Flink Job 提交/监控、执行日志 | Spring Boot 3.2 |
| **Flink Cluster** | 流/批计算执行、窗口聚合、算法运行、状态管理、Checkpoint | Apache Flink 1.18+ |
| **iot-flink-job** (新模块) | Flink Job 可执行 JAR，包含 Source/Sink/ProcessFunction | Flink DataStream API |
| **TDEngine** | 时序数据存储、计算结果回写 | TDEngine 3.x |
| **MySQL** | 规则元数据、执行日志 | MySQL 8.x |

---

## 三、模块拆分设计

### 3.1 新增模块：`iot-flink-job`

当前所有代码在 `iot-flink-service` 一个模块内。融合 Flink 后需要拆出一个独立模块，打包为 Fat JAR 提交到 Flink 集群：

```
bit-industrial-iot/
├── iot-flink-service/          ← Spring Boot 管理端（保留）
│   ├── controller/             ← REST API
│   ├── service/                ← 业务逻辑
│   ├── dao/                    ← MyBatis Mapper
│   └── flink/
│       └── FlinkJobManager.java ← Flink REST Client（新增）
│
├── iot-flink-job/              ← ★ 新增模块：Flink Job 可执行包
│   ├── source/
│   │   ├── TDEngineSource.java
│   │   └── MqttSource.java
│   ├── process/
│   │   ├── RuleProcessFunction.java
│   │   ├── JarAlgorithmInvoker.java
│   │   └── PythonAlgorithmInvoker.java
│   ├── sink/
│   │   ├── TDEngineSink.java
│   │   ├── MySQLLogSink.java
│   │   └── KafkaSink.java
│   ├── window/
│   │   └── DynamicWindowAssigner.java
│   ├── model/
│   │   ├── DeviceDataEvent.java
│   │   └── AlgorithmOutputEvent.java
│   └── RuleJobEntrypoint.java  ← main()，接收规则配置 JSON
│
└── iot-common/
    └── common-flink/           ← ★ 新增公共模块
        ├── IRuleAlgorithm.java ← 算法接口（从 engine 包移出）
        ├── AlgorithmResult.java
        └── DataPoint.java
```

### 3.2 为什么要拆分

| 理由 | 说明 |
|------|------|
| **ClassPath 隔离** | Flink 依赖（flink-streaming-java 等）与 Spring Boot 依赖有冲突（Netty、Jackson 版本），必须分开打包 |
| **部署独立性** | iot-flink-service 是管理面，7×24 运行；Flink Job 在集群侧运行，独立伸缩 |
| **热更新** | 更新算法只需重新提交 Job JAR，无需重启管理服务 |
| **资源隔离** | 算法 OOM / CPU 密集运算在 Flink TaskManager 中，不影响 Spring Boot |

---

## 四、Flink 数据管道详细设计

### 4.1 数据流全链路

```
 ┌─────────────┐     ┌─────────────────┐     ┌────────────────┐     ┌──────────────┐
 │  TDEngine    │     │    KeyBy         │     │   Window       │     │  Algorithm   │
 │  Source      │ ──→ │  (deviceId +     │ ──→ │  Assigner      │ ──→ │  Process     │
 │  (多设备多测点)│     │   pointCode)     │     │  (配置化)       │     │  Function    │
 └─────────────┘     └─────────────────┘     └────────────────┘     └──────┬───────┘
                                                                           │
                                                                           ↓
                                                                    ┌──────────────┐
                                                                    │  Side Output │
                                                                    │  (告警/结果)  │
                                                                    └──────┬───────┘
                                                                      ↙        ↘
                                                              ┌──────────┐  ┌──────────┐
                                                              │ TDEngine │  │  MySQL   │
                                                              │ Sink     │  │  Log     │
                                                              │ (结果回写) │  │  Sink   │
                                                              └──────────┘  └──────────┘
```

### 4.2 Source 设计：多设备多测点

#### 4.2.1 两种 Source 模式

**模式A：JDBC 轮询 Source（批流一体，推荐起步方案）**

```java
/**
 * 定时从 TDEngine 拉取增量数据。
 * 每个 Source 实例负责一批设备的数据读取，通过 parallelism 控制并发度。
 */
public class TDEngineJdbcSource extends RichParallelSourceFunction<DeviceDataEvent> {

    /** 规则配置（含多设备多测点信息） */
    private final RuleJobConfig ruleConfig;

    /** 轮询间隔（毫秒），由 windowSize × windowUnit 推导 */
    private final long pollIntervalMs;

    /** 每个并行实例分配到的设备子集 */
    private transient List<DataSourceConfig> myDevices;

    /** 上一次拉取的最大时间戳（增量游标） */
    private transient volatile long lastTimestamp;

    @Override
    public void open(Configuration parameters) {
        // 1. 按 subtaskIndex 分片，每个并行度负责一部分设备
        int subtaskIndex = getRuntimeContext().getIndexOfThisSubtask();
        int parallelism  = getRuntimeContext().getNumberOfParallelSubtasks();
        myDevices = distributeDevices(ruleConfig.getDataSources(), subtaskIndex, parallelism);

        // 2. 初始化 TDEngine JDBC 连接
        initConnection();
    }

    @Override
    public void run(SourceContext<DeviceDataEvent> ctx) throws Exception {
        while (isRunning) {
            for (DataSourceConfig ds : myDevices) {
                // 3. 构造 SQL：支持多测点
                // SELECT ts, device_id, point_code, value, quality
                // FROM {superTable}
                // WHERE device_id = ? AND point_code IN (?, ?, ...)
                //   AND ts > ? AND ts <= NOW
                // ORDER BY ts ASC
                String sql = buildIncrementalQuery(ds, lastTimestamp);
                ResultSet rs = stmt.executeQuery(sql);

                while (rs.next()) {
                    DeviceDataEvent event = new DeviceDataEvent(
                        rs.getString("device_id"),
                        rs.getString("point_code"),
                        rs.getTimestamp("ts").getTime(),
                        rs.getDouble("value"),
                        rs.getInt("quality")
                    );
                    ctx.collectWithTimestamp(event, event.getTimestamp());
                    lastTimestamp = Math.max(lastTimestamp, event.getTimestamp());
                }
            }
            Thread.sleep(pollIntervalMs);
        }
    }
}
```

**模式B：MQTT 实时 Source（真实时，后续升级方案）**

```java
/**
 * 订阅 MQTT 主题，实时接收设备上报数据。
 * 主题格式：devices/{deviceId}/telemetry
 */
public class MqttRealtimeSource extends RichParallelSourceFunction<DeviceDataEvent> {

    @Override
    public void run(SourceContext<DeviceDataEvent> ctx) {
        mqttClient.subscribe(topicPattern, (topic, message) -> {
            DeviceDataEvent event = parseMessage(topic, message);
            // 仅处理本规则关注的设备和测点
            if (isRelevant(event)) {
                ctx.collectWithTimestamp(event, event.getTimestamp());
            }
        });
    }
}
```

#### 4.2.2 设备分片策略（多设备并行读取）

```
规则配置了 N 个设备数据源：
  Device-A: [P001, P002, T001]
  Device-B: [P001, P003]
  Device-C: [T001, T002, T003]
  Device-D: [P001]

Flink Source parallelism = 2：
  Subtask-0 负责: Device-A, Device-C    ← Round-Robin 分配
  Subtask-1 负责: Device-B, Device-D
```

### 4.3 KeyBy：多设备多测点分组

```java
// 按 deviceId + pointCode 组合键分组
// 每个 (设备, 测点) 组合独立计算窗口
dataStream
    .keyBy(event -> event.getDeviceId() + "#" + event.getPointCode())
```

**为什么用 deviceId + pointCode 作为 Key：**
- 保证同一测点的数据有序进入同一窗口
- 不同设备的同名测点独立计算
- 窗口内数据量可控，避免数据倾斜

**可选：仅按 deviceId 分组**
```java
// 如果算法需要同时处理同一设备的多个测点
dataStream.keyBy(DeviceDataEvent::getDeviceId)
```

这由规则配置中新增的 `key_strategy` 字段控制（见第六节数据库变更）。

### 4.4 Window：配置化时间窗口

```java
public class DynamicWindowAssigner {

    /**
     * 根据规则配置动态应用窗口策略
     */
    public static <T> WindowedStream<T, String, TimeWindow> applyWindow(
            KeyedStream<T, String> keyedStream,
            RuleJobConfig config) {

        long windowSizeMs = config.getWindowSizeMs();
        String windowType = config.getWindowType();

        return switch (windowType) {
            case "tumbling" ->
                keyedStream.window(TumblingEventTimeWindows.of(Time.milliseconds(windowSizeMs)));

            case "sliding" -> {
                // 滑动窗口步长默认为窗口大小的 1/4
                long slideMs = config.getWindowSlideMs() > 0
                        ? config.getWindowSlideMs()
                        : windowSizeMs / 4;
                yield keyedStream.window(
                    SlidingEventTimeWindows.of(Time.milliseconds(windowSizeMs), Time.milliseconds(slideMs)));
            }

            case "session" ->
                keyedStream.window(EventTimeSessionWindows.withGap(Time.milliseconds(windowSizeMs)));

            default -> throw new IllegalArgumentException("不支持的窗口类型: " + windowType);
        };
    }
}
```

### 4.5 ProcessFunction：算法执行

```java
/**
 * 窗口处理函数，在窗口触发时加载并执行用户上传的算法。
 */
public class AlgorithmWindowFunction
        extends ProcessWindowFunction<DeviceDataEvent, AlgorithmOutputEvent, String, TimeWindow> {

    /** 算法文件路径（从 HDFS / 本地 / S3 加载） */
    private final String algorithmPath;
    private final String algorithmType;  // jar / python
    private final String algorithmClass;
    private final Map<String, String> ruleParams;

    /** 算法实例（JAR 类型，懒加载） */
    private transient IRuleAlgorithm algorithmInstance;

    @Override
    public void open(Configuration parameters) {
        if ("jar".equals(algorithmType)) {
            // 动态加载 JAR（与现有 AlgorithmLoader 逻辑一致）
            algorithmInstance = loadJarAlgorithm(algorithmPath, algorithmClass);
        }
    }

    @Override
    public void process(String key,
                        ProcessWindowFunction.Context context,
                        Iterable<DeviceDataEvent> elements,
                        Collector<AlgorithmOutputEvent> out) {

        // 1. 转换为 DataPoint 列表
        List<DataPoint> dataPoints = new ArrayList<>();
        for (DeviceDataEvent event : elements) {
            dataPoints.add(new DataPoint(
                event.getDeviceId(),
                event.getPointCode(),
                new Date(event.getTimestamp()),
                event.getValue(),
                event.getQuality()
            ));
        }

        // 2. 执行算法
        AlgorithmResult result;
        long startMs = System.currentTimeMillis();

        if ("jar".equals(algorithmType)) {
            result = algorithmInstance.execute(dataPoints, ruleParams);
        } else {
            result = PythonAlgorithmInvoker.execute(algorithmPath, dataPoints, ruleParams);
        }

        long durationMs = System.currentTimeMillis() - startMs;

        // 3. 输出结果
        AlgorithmOutputEvent output = new AlgorithmOutputEvent();
        output.setKey(key);
        output.setWindowStart(context.window().getStart());
        output.setWindowEnd(context.window().getEnd());
        output.setSuccess(result.isSuccess());
        output.setResultData(result.getData());
        output.setErrorMsg(result.getErrorMsg());
        output.setDurationMs(durationMs);
        output.setProcessTime(System.currentTimeMillis());

        out.collect(output);
    }
}
```

### 4.6 Sink 设计：计算结果输出

**Sink-1：TDEngine 结果回写**
```java
/**
 * 将算法计算结果写回 TDEngine 的结果超级表。
 * 表结构：ts, rule_id, key, metric_name, metric_value
 */
public class TDEngineResultSink extends RichSinkFunction<AlgorithmOutputEvent> {

    @Override
    public void invoke(AlgorithmOutputEvent event, Context ctx) {
        if (!event.isSuccess() || event.getResultData() == null) return;

        // 将结果 Map 的每个 KV 写入一行
        for (Map.Entry<String, Object> entry : event.getResultData().entrySet()) {
            String sql = "INSERT INTO rule_result_" + event.getRuleId()
                       + " USING rule_result_stable TAGS ('" + event.getRuleId() + "') "
                       + " VALUES (NOW, '" + entry.getKey() + "', " + entry.getValue() + ")";
            stmt.execute(sql);
        }
    }
}
```

**Sink-2：MySQL 执行日志回写**
```java
/**
 * 将每个窗口的执行结果写入 MySQL rule_execution_log 表，
 * 供 iot-flink-service 的管理界面查询。
 */
public class MySQLExecutionLogSink extends RichSinkFunction<AlgorithmOutputEvent> {

    @Override
    public void invoke(AlgorithmOutputEvent event, Context ctx) {
        String sql = "INSERT INTO rule_execution_log "
                   + "(id, rule_id, start_time, end_time, exec_status, result_data, error_msg, duration_ms) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        // ...
    }
}
```

---

## 五、Flink Job 入口与配置传递

### 5.1 Job 入口类

```java
/**
 * Flink Job 唯一入口。
 * 通过 --ruleConfig <base64_json> 接收完整规则配置。
 */
public class RuleJobEntrypoint {

    public static void main(String[] args) throws Exception {
        // 1. 解析参数
        ParameterTool params = ParameterTool.fromArgs(args);
        String configJson = new String(
            Base64.getDecoder().decode(params.getRequired("ruleConfig")),
            StandardCharsets.UTF_8
        );
        RuleJobConfig config = JSON.parseObject(configJson, RuleJobConfig.class);

        // 2. 构建执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(config.getParallelism());

        // 事件时间语义
        env.getConfig().setAutoWatermarkInterval(200L);

        // Checkpoint：每 60s，Exactly-Once
        env.enableCheckpointing(60_000L, CheckpointingMode.EXACTLY_ONCE);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(30_000L);
        env.getCheckpointConfig().setCheckpointTimeout(120_000L);
        env.getCheckpointConfig().setExternalizedCheckpointCleanup(
            ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

        // 3. Source
        DataStream<DeviceDataEvent> source;
        if ("realtime".equals(config.getTriggerType())) {
            source = env.addSource(new MqttRealtimeSource(config))
                        .assignTimestampsAndWatermarks(
                            WatermarkStrategy.<DeviceDataEvent>forBoundedOutOfOrderness(
                                Duration.ofSeconds(5))
                            .withTimestampAssigner((e, t) -> e.getTimestamp()));
        } else {
            source = env.addSource(new TDEngineJdbcSource(config))
                        .assignTimestampsAndWatermarks(
                            WatermarkStrategy.<DeviceDataEvent>forBoundedOutOfOrderness(
                                Duration.ofSeconds(5))
                            .withTimestampAssigner((e, t) -> e.getTimestamp()));
        }

        // 4. KeyBy（根据策略选择分组方式）
        KeyedStream<DeviceDataEvent, String> keyed;
        if ("device".equals(config.getKeyStrategy())) {
            keyed = source.keyBy(DeviceDataEvent::getDeviceId);
        } else {
            // 默认：deviceId#pointCode
            keyed = source.keyBy(e -> e.getDeviceId() + "#" + e.getPointCode());
        }

        // 5. Window + Process
        DataStream<AlgorithmOutputEvent> result = DynamicWindowAssigner
            .applyWindow(keyed, config)
            .process(new AlgorithmWindowFunction(
                config.getAlgorithmPath(),
                config.getAlgorithmType(),
                config.getAlgorithmClass(),
                config.getRuleParams()
            ));

        // 6. Sink（扇出多路输出）
        result.addSink(new TDEngineResultSink(config.getTdengineConfig()));
        result.addSink(new MySQLExecutionLogSink(config.getMysqlConfig(), config.getRuleId()));

        // 7. 启动
        env.execute("RuleJob-" + config.getRuleId() + "-" + config.getRuleName());
    }
}
```

### 5.2 规则配置传递结构 `RuleJobConfig`

```java
/**
 * 传递给 Flink Job 的完整配置（JSON 序列化后 Base64 编码传入）
 */
@Data
public class RuleJobConfig implements Serializable {

    // ---- 规则基础 ----
    private String ruleId;
    private String ruleName;
    private String triggerType;       // periodic / realtime

    // ---- 窗口配置 ----
    private String windowType;        // tumbling / sliding / session
    private long   windowSizeMs;      // 窗口大小（毫秒）
    private long   windowSlideMs;     // 滑动步长（仅 sliding 有效，0 则默认 1/4 窗口）

    // ---- 分组策略 ----
    private String keyStrategy;       // "device_point"(默认) / "device"

    // ---- 算法配置 ----
    private String algorithmType;     // jar / python
    private String algorithmPath;     // 算法文件路径（集群可访问的 HDFS/NFS 路径）
    private String algorithmClass;    // JAR 入口类
    private Map<String, String> ruleParams;   // 自定义参数

    // ---- 数据源配置（多设备多测点） ----
    private List<DataSourceConfig> dataSources;

    // ---- 并行度 ----
    private int parallelism = 2;

    // ---- 连接配置 ----
    private TDEngineConfig tdengineConfig;
    private MysqlConfig    mysqlConfig;
    private MqttConfig     mqttConfig;   // realtime 模式用

    @Data
    public static class DataSourceConfig implements Serializable {
        private String deviceId;
        private String deviceName;
        private List<String> pointCodes;  // 空 = 全量
        private String timeRangeStart;
        private String timeRangeEnd;
    }

    @Data
    public static class TDEngineConfig implements Serializable {
        private String jdbcUrl;     // jdbc:TAOS-RS://host:6041/db
        private String username;
        private String password;
        private String superTable;  // 超级表名
    }

    @Data
    public static class MysqlConfig implements Serializable {
        private String jdbcUrl;
        private String username;
        private String password;
    }

    @Data
    public static class MqttConfig implements Serializable {
        private String brokerUrl;
        private String topicPattern;
        private String username;
        private String password;
    }
}
```

---

## 六、FlinkJobManager：管理面与 Flink 集群交互

### 6.1 交互方式：Flink REST API

Flink 集群暴露 REST API（默认 `http://flink-master:8081`），支持：

| REST 端点 | 用途 | 对应操作 |
|-----------|------|---------|
| `POST /jars/upload` | 上传 Job JAR | 服务启动时上传一次 |
| `POST /jars/{jarId}/run` | 提交运行 Job | 启动规则 |
| `PATCH /jobs/{jobId}` | 取消 Job | 停止规则 |
| `GET /jobs/{jobId}` | 查询 Job 状态 | 监控规则运行 |
| `GET /jobs/{jobId}/exceptions` | 查询异常 | 故障诊断 |
| `POST /jobs/{jobId}/savepoints` | 触发 Savepoint | 升级/恢复 |

### 6.2 FlinkJobManager 核心实现

```java
/**
 * Flink 集群交互管理器。
 * 替代现有 RuleEngineManager 中的 ScheduledExecutorService 调度逻辑。
 */
@Slf4j
@Component
public class FlinkJobManager {

    @Value("${flink.rest.url:http://localhost:8081}")
    private String flinkRestUrl;

    @Value("${flink.job.jar-path}")
    private String flinkJobJarPath;   // iot-flink-job 的 Fat JAR 路径

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 已提交的 Job 映射：ruleId → Flink jobId */
    private final Map<String, String> ruleJobMapping = new ConcurrentHashMap<>();

    /** 上传后的 Flink JAR ID（upload 一次即可复用） */
    private volatile String uploadedJarId;

    // =======================================================================
    // 启动规则 → 提交 Flink Job
    // =======================================================================

    public String submitJob(RuleJobConfig config) throws Exception {
        // 1. 确保 JAR 已上传
        ensureJarUploaded();

        // 2. 将配置 JSON Base64 编码作为 program-args
        String configJson = objectMapper.writeValueAsString(config);
        String base64Config = Base64.getEncoder().encodeToString(
            configJson.getBytes(StandardCharsets.UTF_8));

        // 3. 调用 Flink REST API 提交 Job
        String url = flinkRestUrl + "/jars/" + uploadedJarId + "/run";
        Map<String, Object> body = new HashMap<>();
        body.put("entryClass", "com.bit.iot.flink.job.RuleJobEntrypoint");
        body.put("programArgs", "--ruleConfig " + base64Config);
        body.put("parallelism", config.getParallelism());

        ResponseEntity<Map> resp = restTemplate.postForEntity(url, body, Map.class);
        String jobId = (String) resp.getBody().get("jobid");

        ruleJobMapping.put(config.getRuleId(), jobId);
        log.info("规则 {} 已提交 Flink Job: {}", config.getRuleId(), jobId);
        return jobId;
    }

    // =======================================================================
    // 停止规则 → 取消 Flink Job（可选 Savepoint）
    // =======================================================================

    public void cancelJob(String ruleId, boolean withSavepoint) throws Exception {
        String jobId = ruleJobMapping.get(ruleId);
        if (jobId == null) {
            throw new RuntimeException("未找到规则对应的 Flink Job: " + ruleId);
        }

        if (withSavepoint) {
            // 触发 Savepoint 后取消（用于升级场景）
            String savepointUrl = flinkRestUrl + "/jobs/" + jobId + "/savepoints";
            Map<String, Object> body = Map.of(
                "cancel-job", true,
                "target-directory", "hdfs:///flink/savepoints"
            );
            restTemplate.postForEntity(savepointUrl, body, Map.class);
        } else {
            // 直接取消
            String cancelUrl = flinkRestUrl + "/jobs/" + jobId + "?mode=cancel";
            restTemplate.patchForObject(cancelUrl, null, Map.class);
        }

        ruleJobMapping.remove(ruleId);
        log.info("规则 {} 的 Flink Job {} 已取消", ruleId, jobId);
    }

    // =======================================================================
    // 查询 Job 状态
    // =======================================================================

    public FlinkJobStatus getJobStatus(String ruleId) {
        String jobId = ruleJobMapping.get(ruleId);
        if (jobId == null) return FlinkJobStatus.NOT_FOUND;

        try {
            String url = flinkRestUrl + "/jobs/" + jobId;
            Map resp = restTemplate.getForObject(url, Map.class);
            String state = (String) resp.get("state");
            return FlinkJobStatus.valueOf(state);
        } catch (Exception e) {
            log.warn("查询 Flink Job 状态失败: {}", e.getMessage());
            return FlinkJobStatus.UNKNOWN;
        }
    }

    // =======================================================================
    // JAR 上传
    // =======================================================================

    private synchronized void ensureJarUploaded() throws Exception {
        if (uploadedJarId != null) return;

        String url = flinkRestUrl + "/jars/upload";
        File jarFile = new File(flinkJobJarPath);
        if (!jarFile.exists()) {
            throw new RuntimeException("Flink Job JAR 文件不存在: " + flinkJobJarPath);
        }

        // multipart upload
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("jarfile", new FileSystemResource(jarFile));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<Map> resp = restTemplate.postForEntity(
            url, new HttpEntity<>(body, headers), Map.class);

        String filename = (String) resp.getBody().get("filename");
        // filename 格式: /flink-web-upload/xxx.jar → 取最后部分
        uploadedJarId = filename.substring(filename.lastIndexOf('/') + 1);
        log.info("Flink Job JAR 上传成功: {}", uploadedJarId);
    }
}
```

### 6.3 FlinkJobStatus 枚举

```java
public enum FlinkJobStatus {
    CREATED,
    RUNNING,
    FAILING,
    FAILED,
    CANCELLING,
    CANCELED,
    FINISHED,
    RESTARTING,
    SUSPENDED,
    RECONCILING,
    NOT_FOUND,
    UNKNOWN
}
```

---

## 七、Service 层改造

### 7.1 RuleConfigServiceImpl 改造对比

```java
// ======================== 改造前 ========================
// startRule() 调用 RuleEngineManager（线程池调度）

ruleEngineManager.startRule(config, algorithm, dataSources, params, logConsumer);

// ======================== 改造后 ========================
// startRule() 构建 RuleJobConfig 并提交 Flink Job

public boolean startRule(String id) {
    RuleConfig config = this.getById(id);
    RuleAlgorithm algorithm = algorithmService.getById(config.getAlgorithmId());
    List<RuleDataSource> dataSources = ...;
    List<RuleParam> params = ...;

    // 1. 组装 Flink Job 配置
    RuleJobConfig jobConfig = buildRuleJobConfig(config, algorithm, dataSources, params);

    // 2. 提交到 Flink 集群
    String flinkJobId = flinkJobManager.submitJob(jobConfig);

    // 3. 记录 Flink Job ID
    config.setFlinkJobId(flinkJobId);    // ← 新增字段
    config.setRuleStatus(1);
    config.setUpdateTime(new Date());
    this.updateById(config);
    return true;
}

public boolean stopRule(String id) {
    RuleConfig config = this.getById(id);

    // 取消 Flink Job
    flinkJobManager.cancelJob(id, false);

    config.setRuleStatus(0);
    config.setFlinkJobId(null);
    config.setUpdateTime(new Date());
    this.updateById(config);
    return true;
}
```

### 7.2 新增接口：查询 Flink Job 运行状态

```java
// RuleConfigController 新增
@GetMapping("/{id}/flink-status")
@Operation(summary = "查询规则对应的 Flink Job 实时状态")
public Result<FlinkJobStatus> getFlinkJobStatus(@PathVariable String id) {
    return success(flinkJobManager.getJobStatus(id));
}
```

---

## 八、算法文件分发策略

算法文件（JAR / Python）需要让 Flink 集群的所有 TaskManager 都能访问到。

### 8.1 三种策略对比

| 策略 | 实现 | 优点 | 缺点 | 推荐场景 |
|------|------|------|------|---------|
| **NFS 共享存储** | 所有节点挂载同一 NFS 目录 | 简单 | 单点故障 | 开发/测试 |
| **HDFS 分布式存储** | 上传到 HDFS，Flink Job 读取 | 高可用、扩展性好 | 需要 Hadoop 集群 | 生产环境 |
| **打包到 Job JAR** | 算法文件打进 Fat JAR 的 resources | 无需外部存储 | 每次更新需重新打包 | 算法固定不变 |

**推荐方案：NFS（起步） → HDFS（生产）**

```yaml
# application.yml 新增配置
flink:
  rest:
    url: http://flink-master:8081
  job:
    jar-path: /opt/flink-jobs/iot-flink-job.jar
  algorithm:
    storage-type: nfs           # nfs / hdfs
    nfs-path: /mnt/shared/algorithms
    hdfs-path: hdfs:///iot/algorithms
```

上传算法文件时同步写入共享存储：

```java
// RuleAlgorithmServiceImpl.uploadAlgorithm() 改造
Path localPath  = Paths.get(uploadPath, uniqueFilename);
Path sharedPath = Paths.get(sharedAlgorithmPath, uniqueFilename);

file.transferTo(localPath.toFile());
Files.copy(localPath, sharedPath, StandardCopyOption.REPLACE_EXISTING);

algorithm.setAlgorithmPath(sharedPath.toAbsolutePath().toString()); // 存共享路径
```

---

## 九、数据库 Schema 变更

### 9.1 `rule_config` 表新增字段

```sql
ALTER TABLE rule_config
    ADD COLUMN `flink_job_id`   VARCHAR(64)  COMMENT 'Flink Job ID（运行时填充）' AFTER rule_status,
    ADD COLUMN `key_strategy`   VARCHAR(20)  DEFAULT 'device_point' COMMENT '分组策略：device_point / device' AFTER window_unit,
    ADD COLUMN `window_slide`   BIGINT       COMMENT '滑动窗口步长（仅 sliding 有效）' AFTER window_size,
    ADD COLUMN `parallelism`    INT          DEFAULT 2 COMMENT 'Flink Job 并行度' AFTER key_strategy;
```

### 9.2 新增 TDEngine 结果超级表

```sql
-- 在 TDEngine 中创建（非 MySQL）
CREATE STABLE IF NOT EXISTS rule_result_stable (
    ts        TIMESTAMP,
    metric_name  NCHAR(100),
    metric_value DOUBLE
) TAGS (
    rule_id NCHAR(32)
);
```

### 9.3 新增系统配置表（存放连接信息）

```sql
CREATE TABLE IF NOT EXISTS `rule_engine_config` (
    `id`           VARCHAR(32)  NOT NULL,
    `config_key`   VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(500) COMMENT '配置值',
    `config_desc`  VARCHAR(200) COMMENT '配置说明',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则引擎系统配置';

INSERT INTO rule_engine_config VALUES
    (UUID(), 'flink.rest.url',        'http://localhost:8081',                    'Flink REST 地址'),
    (UUID(), 'tdengine.jdbc.url',     'jdbc:TAOS-RS://localhost:6041/bit_iot_ts', 'TDEngine JDBC URL'),
    (UUID(), 'tdengine.username',     'root',                                     'TDEngine 用户名'),
    (UUID(), 'tdengine.password',     'taosdata',                                 'TDEngine 密码'),
    (UUID(), 'tdengine.super.table',  'device_data',                              'TDEngine 设备数据超级表'),
    (UUID(), 'mqtt.broker.url',       'tcp://localhost:1883',                     'MQTT Broker 地址');
```

---

## 十、Checkpoint 与容错

### 10.1 Checkpoint 配置

```java
// RuleJobEntrypoint 中
env.enableCheckpointing(60_000L, CheckpointingMode.EXACTLY_ONCE);
env.getCheckpointConfig().setMinPauseBetweenCheckpoints(30_000L);
env.getCheckpointConfig().setCheckpointTimeout(120_000L);
env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
env.getCheckpointConfig().setExternalizedCheckpointCleanup(
    ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

// 状态后端：RocksDB（生产推荐）
env.setStateBackend(new EmbeddedRocksDBStateBackend());
env.getCheckpointConfig().setCheckpointStorage("hdfs:///flink/checkpoints");
```

### 10.2 故障恢复流程

```
Job 失败
    ↓
Flink 自动重启（配置重启策略：fixedDelayRestart, 3 次, 间隔 10s）
    ↓
从最近一次 Checkpoint 恢复状态
    ↓
TDEngine Source 从 lastTimestamp 继续读取
    ↓
窗口内已累积的数据从状态恢复，不丢失
```

### 10.3 规则升级流程（Savepoint）

```
1. iot-flink-service 调用 Flink REST API 触发 Savepoint 并取消 Job
2. 上传新的算法文件到共享存储
3. 重新提交 Job，指定 --fromSavepoint <path>
4. Job 从 Savepoint 恢复，窗口状态无损
```

---

## 十一、iot-flink-job 模块 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
    <parent>
        <groupId>com.bit</groupId>
        <artifactId>bit-industrial-iot</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>iot-flink-job</artifactId>
    <packaging>jar</packaging>

    <properties>
        <flink.version>2.2.0</flink.version>
    </properties>

    <dependencies>
        <!-- Flink Core（provided，由集群提供） -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-streaming-java</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-clients</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-statebackend-rocksdb</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- TDEngine JDBC Driver（打包进 JAR） -->
        <dependency>
            <groupId>com.taosdata.jdbc</groupId>
            <artifactId>taos-jdbcdriver</artifactId>
            <version>3.2.7</version>
        </dependency>

        <!-- MQTT Client（实时 Source 用） -->
        <dependency>
            <groupId>org.eclipse.paho</groupId>
            <artifactId>org.eclipse.paho.mqttv5.client</artifactId>
            <version>1.2.5</version>
        </dependency>

        <!-- MySQL JDBC（日志 Sink） -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>8.4.0</version>
        </dependency>

        <!-- 公共算法接口 -->
        <dependency>
            <groupId>com.bit</groupId>
            <artifactId>common-flink</artifactId>
            <version>0.0.1-SNAPSHOT</version>
        </dependency>

        <!-- JSON -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
            <version>2.0.47</version>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- 打包为 Fat JAR（shade 插件） -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                        <configuration>
                            <transformers>
                                <transformer implementation=
                                    "org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>com.bit.iot.flink.job.RuleJobEntrypoint</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 十二、部署拓扑

### 12.1 开发环境

```
┌───────────────────────────────────────────────┐
│              开发者本机 (Mac / Linux)            │
│                                               │
│  iot-flink-service (:9004)  ← Spring Boot     │
│  Flink MiniCluster          ← 单机嵌入式       │
│  MySQL (:3306)                                │
│  TDEngine (:6041)                             │
│  MQTT Broker (:1883)        ← EMQX/Mosquitto │
│  Nacos (:8848)                                │
└───────────────────────────────────────────────┘
```

### 12.2 生产环境

```
┌──────────┐     ┌──────────────────┐     ┌──────────────────────┐
│  Nginx   │────→│ iot-flink-service│────→│   Flink Cluster      │
│  Gateway │     │  (K8s × 2)      │     │   (Standalone/K8s)   │
│          │     │  :9004           │     │                      │
└──────────┘     └──────────────────┘     │  JobManager × 2 (HA) │
                         │                │  TaskManager × N     │
                         ↓                │  (按需伸缩)           │
                 ┌───────────────┐        └──────────┬───────────┘
                 │   MySQL (主从) │                    │
                 │   :3306       │                    ↓
                 └───────────────┘        ┌──────────────────────┐
                                          │   TDEngine Cluster   │
                 ┌───────────────┐        │   (3 节点)            │
                 │  NFS / HDFS   │←───────│                      │
                 │  (算法文件)    │        └──────────────────────┘
                 └───────────────┘
                                          ┌──────────────────────┐
                                          │   EMQX Cluster       │
                                          │   (MQTT Broker)      │
                                          └──────────────────────┘
```

---

## 十三、实施路线

### Phase 1（第 1-2 周）：基础框架

| 步骤 | 工作内容 | 产出 |
|------|---------|------|
| 1.1 | 创建 `iot-flink-job` 模块，配置 pom.xml | 可编译的空模块 |
| 1.2 | 创建 `common-flink` 公共模块，迁移 IRuleAlgorithm / DataPoint / AlgorithmResult | 公共接口包 |
| 1.3 | 实现 `DeviceDataEvent`、`AlgorithmOutputEvent` 数据模型 | POJO |
| 1.4 | 实现 `RuleJobConfig` 配置传递结构 | 序列化结构 |
| 1.5 | 实现 `RuleJobEntrypoint.main()`（不含算法逻辑，仅打通 Source → Print Sink） | 可提交到 Flink 的 Job |

### Phase 2（第 3-4 周）：Source + Sink

| 步骤 | 工作内容 | 产出 |
|------|---------|------|
| 2.1 | 实现 `TDEngineJdbcSource`，含多设备分片、增量游标 | 可读取 TDEngine 数据 |
| 2.2 | 实现 `TDEngineResultSink`，结果回写 | 计算结果落库 |
| 2.3 | 实现 `MySQLExecutionLogSink`，日志回写 | 执行日志可查 |
| 2.4 | 实现 `DynamicWindowAssigner` | Tumbling/Sliding/Session 三种窗口 |

### Phase 3（第 5-6 周）：算法执行 + 管理面对接

| 步骤 | 工作内容 | 产出 |
|------|---------|------|
| 3.1 | 实现 `AlgorithmWindowFunction`，JAR 动态加载 + Python 子进程 | 算法可在窗口内执行 |
| 3.2 | 实现 `FlinkJobManager`（REST Client） | 可通过管理面提交/取消 Job |
| 3.3 | 改造 `RuleConfigServiceImpl`，startRule/stopRule 对接 FlinkJobManager | 端到端打通 |
| 3.4 | 数据库 Schema 变更 + RuleConfig 实体新增字段 | 持久化 Flink 状态 |

### Phase 4（第 7-8 周）：高可用 + 监控

| 步骤 | 工作内容 | 产出 |
|------|---------|------|
| 4.1 | 配置 Checkpoint + RocksDB 状态后端 | 故障可恢复 |
| 4.2 | 实现 Savepoint 升级流程 | 无损升级算法 |
| 4.3 | 实现 `MqttRealtimeSource` | 真实时处理 |
| 4.4 | 新增 Flink 监控面板（Job 状态、吞吐量、延迟） | 可观测性 |
| 4.5 | 压测 + 调优（并行度、窗口大小、Checkpoint 间隔） | 性能基线 |

---

## 十四、关键数据流举例

### 场景：风机振动异常检测

**规则配置：**
```json
{
    "ruleName": "风机振动异常检测",
    "algorithmType": "jar",
    "algorithmClass": "com.example.VibrationAnalyzer",
    "triggerType": "realtime",
    "windowType": "sliding",
    "windowSize": 60, "windowUnit": "s",
    "windowSlide": 15,
    "keyStrategy": "device_point",
    "parallelism": 4,
    "dataSources": [
        {"deviceId": "WT-001", "pointCodes": ["VIB_X", "VIB_Y", "VIB_Z"]},
        {"deviceId": "WT-002", "pointCodes": ["VIB_X", "VIB_Y", "VIB_Z"]},
        {"deviceId": "WT-003", "pointCodes": ["VIB_X", "VIB_Y", "VIB_Z"]}
    ],
    "ruleParams": {
        "threshold": "5.0",
        "alertLevel": "warning"
    }
}
```

**运行时数据流：**

```
                   Source (parallelism=4)
                   ├── Subtask-0: WT-001 (VIB_X,VIB_Y,VIB_Z)
                   ├── Subtask-1: WT-002 (VIB_X,VIB_Y,VIB_Z)
                   ├── Subtask-2: WT-003 (VIB_X,VIB_Y,VIB_Z)
                   └── Subtask-3: (空闲)
                           ↓
                   KeyBy: deviceId#pointCode
                   产生 9 个 Key:
                   WT-001#VIB_X, WT-001#VIB_Y, WT-001#VIB_Z
                   WT-002#VIB_X, WT-002#VIB_Y, WT-002#VIB_Z
                   WT-003#VIB_X, WT-003#VIB_Y, WT-003#VIB_Z
                           ↓
                   SlidingWindow(60s, slide 15s)
                   每个 Key 独立窗口，每 15s 触发一次
                           ↓
                   AlgorithmWindowFunction:
                   VibrationAnalyzer.execute(
                     [DataPoint(WT-001, VIB_X, t1, 3.2), DataPoint(WT-001, VIB_X, t2, 4.8), ...],
                     {"threshold": "5.0", "alertLevel": "warning"}
                   )
                           ↓
                   输出: AlgorithmOutputEvent {
                     key: "WT-001#VIB_X",
                     windowStart: 1711526400000,
                     windowEnd:   1711526460000,
                     resultData: {"rms": 4.2, "peak": 6.1, "alert": true}
                   }
                           ↓
                   ├── TDEngineSink: INSERT INTO rule_result_xxx VALUES(...)
                   └── MySQLLogSink: INSERT INTO rule_execution_log(...)
```

---

## 十五、FAQ

### Q1：为什么不用 Flink SQL + UDF？

Flink SQL + UDF 适合标准的 ETL 和聚合计算。但我们的场景是 **用户上传任意算法**（JAR/Python），算法逻辑千差万别（FFT 频谱分析、机器学习推理、统计检验…），无法用 SQL 表达。DataStream API + ProcessFunction 灵活性更强。

### Q2：Python 算法在 Flink TaskManager 中如何执行？

方案 A（推荐）：TaskManager 节点预装 Python3 + numpy/pandas 等常用库，算法通过子进程（ProcessBuilder）调用。
方案 B：使用 PyFlink，但需要 Flink 集群配置 Python 环境，且与 Java 混合部署有坑。
方案 C：算法容器化，通过 gRPC 调用（最隔离，但复杂度最高）。

### Q3：多设备多测点的并行度怎么设？

经验公式：`parallelism = min(设备数 × 测点数, TaskManager Slot 总数 / 2)`。
3 台设备 × 3 测点 = 9 个 Key → parallelism = 4 足够（Key 会 hash 分布到 4 个 Subtask）。

### Q4：TDEngine Source 轮询频率和窗口大小的关系？

轮询间隔应 ≤ 窗口大小的 1/4。例如窗口 60s → 轮询间隔 ≤ 15s。这样保证窗口触发时有足够数据。对于实时模式（MQTT Source），无需轮询，数据实时推送。

### Q5：如何从当前线程池方案平滑迁移到 Flink？

分三步：
1. 先部署 Flink 集群 + iot-flink-job
2. `FlinkJobManager` 和 `RuleEngineManager` 并存，新增配置项 `rule.engine.type: flink / local`
3. 新建规则默认走 Flink，存量规则逐步迁移后下线 `RuleEngineManager`
