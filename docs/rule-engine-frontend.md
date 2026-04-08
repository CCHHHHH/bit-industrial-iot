# 规则引擎前端开发文档

> 本文档基于 `iot-rule-service` 后端实现整理，供前端开发人员参考。
> 服务端口：**9004**，所有接口经 API 网关（端口 9000）统一代理，前缀为 `/api/rule`。

---

## 目录

1. [约定与规范](#1-约定与规范)
2. [数据字典](#2-数据字典)
3. [接口文档](#3-接口文档)
   - 3.1 [规则配置](#31-规则配置)
   - 3.2 [算法管理](#32-算法管理)
   - 3.3 [执行日志](#33-执行日志)
4. [TypeScript 类型定义](#4-typescript-类型定义)
5. [页面设计说明](#5-页面设计说明)
6. [业务流程](#6-业务流程)
7. [字段校验规则](#7-字段校验规则)

---

## 1. 约定与规范

### 请求基础信息

| 项目 | 说明 |
|------|------|
| 网关地址 | `http://{host}:9000` |
| 接口前缀 | `/api/rule` |
| Content-Type | `application/json`（文件上传除外） |
| 认证方式 | 请求头携带 `Authorization: Bearer {token}` |

### 统一响应结构

所有接口返回统一包装格式：

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | `number` | `200` 表示成功，其他值表示失败 |
| `msg` | `string` | 错误时为错误描述 |
| `data` | `any` | 成功时的响应数据，失败时为 `null` |

### 分页响应结构

所有分页接口的 `data` 字段为 MyBatis-Plus `Page` 对象：

```json
{
  "code": 200,
  "data": {
    "records": [],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

---

## 2. 数据字典

### 2.1 触发类型 `triggerType`

| 值 | 含义 | 说明 |
|----|------|------|
| `periodic` | 定时触发 | 需配合 `triggerCron` 字段，按 Cron 表达式周期执行 |
| `realtime` | 实时流处理 | 订阅 MQTT 实时数据，持续运行 |

### 2.2 时间窗口类型 `windowType`

| 值 | 含义 | 说明 |
|----|------|------|
| `tumbling` | 滚动窗口 | 固定大小、无重叠，每个事件只属于一个窗口 |
| `sliding` | 滑动窗口 | 固定大小、有重叠，需同时配置 `windowSlide` |
| `session` | 会话窗口 | 按数据间隔动态分割，`windowSize` 表示会话间隔超时时间 |

### 2.3 窗口单位 `windowUnit`

| 值 | 含义 |
|----|------|
| `s` | 秒 |
| `m` | 分钟 |
| `h` | 小时 |
| `d` | 天 |

### 2.4 分组策略 `keyStrategy`

| 值 | 含义 | 说明 |
|----|------|------|
| `device_point` | 按设备+测点分组 | 每个设备的每个测点独立计算窗口（默认） |
| `device` | 按设备分组 | 同一设备的所有测点合并到一个窗口，算法可跨测点计算 |

### 2.5 规则状态 `ruleStatus`

| 值 | 含义 |
|----|------|
| `0` | 已停止 |
| `1` | 运行中 |

### 2.6 算法类型 `algorithmType`

| 值 | 含义 | 说明 |
|----|------|------|
| `jar` | Java JAR 包 | 需同时填写 `algorithmClass`（入口类全限定名） |
| `python` | Python 脚本 | 无需填写 `algorithmClass` |

### 2.7 算法状态 `algorithmStatus`

| 值 | 含义 |
|----|------|
| `0` | 禁用 |
| `1` | 启用 |

### 2.8 执行状态 `execStatus`

| 值 | 含义 |
|----|------|
| `0` | 执行中 |
| `1` | 执行成功 |
| `2` | 执行失败 |

### 2.9 Flink Job 状态 `FlinkJobStatus`

| 值 | 含义 |
|----|------|
| `CREATED` | 已创建，待调度 |
| `RUNNING` | 运行中 |
| `FAILING` | 正在失败 |
| `FAILED` | 已失败 |
| `CANCELLING` | 取消中 |
| `CANCELED` | 已取消 |
| `FINISHED` | 已完成 |
| `RESTARTING` | 重启中 |
| `NOT_FOUND` | 未找到（服务重启后映射丢失） |
| `UNKNOWN` | 查询失败 |

---

## 3. 接口文档

### 3.1 规则配置

#### 获取规则列表

```
GET /api/rule/rule-config/list
```

**请求参数（Query）**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `current` | `number` | 否 | `1` | 当前页码 |
| `size` | `number` | 否 | `10` | 每页条数 |
| `ruleName` | `string` | 否 | — | 规则名称（模糊匹配） |
| `algorithmId` | `string` | 否 | — | 按算法 ID 过滤 |

**响应 `data`**：`Page<RuleConfigListItemDTO>`

```json
{
  "records": [
    {
      "id": "abc123",
      "ruleName": "温度异常检测",
      "ruleDesc": "检测设备温度是否超过阈值",
      "algorithmId": "alg001",
      "algorithmName": "阈值检测算法",
      "algorithmType": "jar",
      "triggerType": "realtime",
      "triggerCron": null,
      "windowType": "tumbling",
      "windowSize": 5,
      "windowUnit": "m",
      "keyStrategy": "device_point",
      "parallelism": 2,
      "ruleStatus": 1,
      "flinkJobId": "a1b2c3d4",
      "createTime": "2026-03-01T10:00:00",
      "updateTime": "2026-03-31T12:00:00"
    }
  ],
  "total": 1,
  "size": 10,
  "current": 1,
  "pages": 1
}
```

---

#### 获取规则详情

```
GET /api/rule/rule-config/{id}
```

**响应 `data`**：`RuleConfigDetailDTO`

```json
{
  "ruleConfig": {
    "id": "abc123",
    "ruleName": "温度异常检测",
    "algorithmId": "alg001",
    "triggerType": "realtime",
    "windowType": "tumbling",
    "windowSize": 5,
    "windowUnit": "m",
    "keyStrategy": "device_point",
    "parallelism": 2,
    "ruleStatus": 0
  },
  "algorithmName": "阈值检测算法",
  "algorithmType": "jar",
  "dataSources": [
    {
      "id": "ds001",
      "ruleId": "abc123",
      "deviceId": "DEV001",
      "deviceName": "1号传感器",
      "pointCodes": "[\"TEMP\",\"HUMID\"]",
      "timeRangeStart": "08:00:00",
      "timeRangeEnd": "20:00:00"
    }
  ],
  "params": [
    {
      "id": "p001",
      "ruleId": "abc123",
      "paramKey": "threshold",
      "paramValue": "80.0",
      "paramDesc": "温度报警阈值（℃）"
    }
  ]
}
```

> **注意**：`pointCodes` 存储为 JSON 数组字符串，前端需 `JSON.parse()` 解析后使用。

---

#### 新增规则

```
POST /api/rule/rule-config
Content-Type: application/json
```

**请求体**：`RuleConfig`（不含 `id`、`ruleStatus`、`flinkJobId`、时间戳字段）

```json
{
  "ruleName": "温度异常检测",
  "ruleDesc": "检测设备温度是否超过阈值",
  "algorithmId": "alg001",
  "triggerType": "realtime",
  "triggerCron": null,
  "windowType": "tumbling",
  "windowSize": 5,
  "windowSlide": null,
  "windowUnit": "m",
  "keyStrategy": "device_point",
  "parallelism": 2
}
```

**响应 `data`**：新增规则的 `id`（`string`）

---

#### 编辑规则

```
PUT /api/rule/rule-config
Content-Type: application/json
```

**请求体**：`RuleConfig`（必须包含 `id`）

> **注意**：运行中的规则须先停止后再编辑。

---

#### 删除规则

```
DELETE /api/rule/rule-config/{id}
```

> 级联删除该规则下的所有数据源、参数、执行日志。运行中的规则不允许删除，须先停止。

---

#### 保存数据源配置

```
POST /api/rule/rule-config/{id}/data-sources
Content-Type: application/json
```

**请求体**：`RuleDataSource[]`（整体替换，先清空再保存）

```json
[
  {
    "deviceId": "DEV001",
    "deviceName": "1号传感器",
    "pointCodes": "[\"TEMP\",\"HUMID\"]",
    "timeRangeStart": "08:00:00",
    "timeRangeEnd": "20:00:00"
  }
]
```

> `pointCodes` 前端收集到字符串数组后需 `JSON.stringify()` 后作为字段值传入。
> `timeRangeStart` / `timeRangeEnd` 可为空（表示不限时段）。

---

#### 保存规则参数

```
POST /api/rule/rule-config/{id}/params
Content-Type: application/json
```

**请求体**：`RuleParam[]`（整体替换）

```json
[
  {
    "paramKey": "threshold",
    "paramValue": "80.0",
    "paramDesc": "温度报警阈值（℃）"
  }
]
```

---

#### 启动规则

```
PUT /api/rule/rule-config/{id}/start
```

后端将校验：
- 规则当前必须是停止状态（`ruleStatus = 0`）
- 规则必须绑定了可用算法（`algorithmStatus = 1`）
- 规则必须配置了至少一个数据源

---

#### 停止规则

```
PUT /api/rule/rule-config/{id}/stop
```

---

#### 手动触发执行一次

```
POST /api/rule/rule-config/{id}/trigger
```

不改变规则的 `ruleStatus`，仅执行一次当前配置。

---

#### 查询 Flink Job 实时状态

```
GET /api/rule/rule-config/{id}/flink-status
```

**响应 `data`**：`string`，枚举值见 [2.9 Flink Job 状态](#29-flink-job-状态-flinkjobstatus)

```json
{
  "code": 200,
  "data": "RUNNING"
}
```

> 此接口反映 Flink 集群中 Job 的实时状态，与数据库中 `ruleStatus` 可能存在短暂不一致，建议在规则详情页轮询（建议间隔 5s）。

---

### 3.2 算法管理

#### 获取算法列表

```
GET /api/rule/rule-algorithm/list
```

**请求参数（Query）**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `current` | `number` | 否 | `1` | 当前页码 |
| `size` | `number` | 否 | `10` | 每页条数 |
| `algorithmName` | `string` | 否 | — | 算法名称（模糊匹配） |
| `algorithmType` | `string` | 否 | — | 算法类型（`jar` / `python`） |

**响应 `data`**：`Page<RuleAlgorithm>`

```json
{
  "records": [
    {
      "id": "alg001",
      "algorithmName": "阈值检测算法",
      "algorithmDesc": "检测数值是否超过指定阈值",
      "algorithmType": "jar",
      "algorithmPath": "./algorithms/threshold-1.0.0.jar",
      "algorithmClass": "com.example.ThresholdAlgorithm",
      "algorithmVersion": "1.0.0",
      "algorithmStatus": 1,
      "fileSize": 20480,
      "createTime": "2026-03-01T10:00:00",
      "updateTime": "2026-03-01T10:00:00"
    }
  ],
  "total": 1,
  "size": 10,
  "current": 1,
  "pages": 1
}
```

---

#### 上传算法文件

```
POST /api/rule/rule-algorithm/upload
Content-Type: multipart/form-data
```

**请求参数（FormData）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | `File` | 是 | 算法文件（`.jar` 或 `.py`） |
| `algorithmName` | `string` | 否 | 算法名称，不传则从文件名推断 |
| `algorithmDesc` | `string` | 否 | 算法描述 |
| `algorithmType` | `string` | 否 | `jar` / `python`，不传则从文件后缀推断 |
| `algorithmClass` | `string` | 否 | JAR 类型必填，Python 类型无需填写 |
| `algorithmVersion` | `string` | 否 | 版本号，默认 `1.0.0` |

**响应 `data`**：`RuleAlgorithm`（含自动生成的 `id` 和 `algorithmPath`）

---

#### 新增算法（仅元数据）

```
POST /api/rule/rule-algorithm
Content-Type: application/json
```

> 适用于文件已通过其他方式部署到服务器共享路径的场景，直接录入元数据。

**请求体**：`RuleAlgorithm`

---

#### 编辑算法

```
PUT /api/rule/rule-algorithm
Content-Type: application/json
```

**请求体**：`RuleAlgorithm`（必须包含 `id`）

> 编辑后，已加载到内存的算法实例会被自动卸载，下次执行时重新加载，实现热替换。

---

#### 删除算法

```
DELETE /api/rule/rule-algorithm/{id}
```

> 同时删除服务器上的算法文件。已被规则绑定的算法建议先解除绑定再删除。

---

#### 启用 / 禁用算法

```
PUT /api/rule/rule-algorithm/{id}/enable
PUT /api/rule/rule-algorithm/{id}/disable
```

> 禁用状态的算法无法被规则启动时调用，但不影响已运行中的规则。

---

### 3.3 执行日志

#### 获取执行日志列表

```
GET /api/rule/rule-execution-log/list
```

**请求参数（Query）**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `current` | `number` | 否 | `1` | 当前页码 |
| `size` | `number` | 否 | `20` | 每页条数 |
| `ruleId` | `string` | 否 | — | 按规则 ID 过滤 |
| `execStatus` | `number` | 否 | — | 按执行状态过滤（`0`/`1`/`2`） |

**响应 `data`**：`Page<RuleExecutionLog>`

```json
{
  "records": [
    {
      "id": "log001",
      "ruleId": "abc123",
      "startTime": "2026-03-31T12:00:00",
      "endTime": "2026-03-31T12:05:00",
      "execStatus": 1,
      "resultData": "{\"avg\":72.5,\"max\":85.3}",
      "errorMsg": null,
      "durationMs": 234
    }
  ],
  "total": 50,
  "size": 20,
  "current": 1,
  "pages": 3
}
```

> `resultData` 为 JSON 字符串，前端展示时需 `JSON.parse()` 后格式化。

---

#### 清空规则执行日志

```
DELETE /api/rule/rule-execution-log/clear/{ruleId}
```

---

## 4. TypeScript 类型定义

```typescript
// ============================================================
// 通用响应
// ============================================================

interface ApiResult<T = any> {
  code: number
  msg: string
  data: T
}

interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// ============================================================
// 枚举
// ============================================================

type TriggerType = 'periodic' | 'realtime'
type WindowType = 'tumbling' | 'sliding' | 'session'
type WindowUnit = 's' | 'm' | 'h' | 'd'
type KeyStrategy = 'device_point' | 'device'
type AlgorithmType = 'jar' | 'python'
type RuleStatus = 0 | 1
type AlgorithmStatus = 0 | 1
type ExecStatus = 0 | 1 | 2
type FlinkJobStatus =
  | 'CREATED' | 'RUNNING' | 'FAILING' | 'FAILED'
  | 'CANCELLING' | 'CANCELED' | 'FINISHED' | 'RESTARTING'
  | 'SUSPENDED' | 'RECONCILING' | 'NOT_FOUND' | 'UNKNOWN'

// ============================================================
// 规则配置
// ============================================================

interface RuleConfig {
  id?: string
  ruleName: string
  ruleDesc?: string
  algorithmId: string
  triggerType: TriggerType
  triggerCron?: string          // triggerType = 'periodic' 时必填
  windowType: WindowType
  windowSize: number
  windowSlide?: number          // windowType = 'sliding' 时必填
  windowUnit: WindowUnit
  keyStrategy: KeyStrategy
  parallelism: number
  ruleStatus?: RuleStatus       // 只读，由后端维护
  flinkJobId?: string           // 只读
  createTime?: string
  updateTime?: string
}

interface RuleConfigListItemDTO extends RuleConfig {
  algorithmName: string
  algorithmType: AlgorithmType
}

interface RuleConfigDetailDTO {
  ruleConfig: RuleConfig
  algorithmName: string
  algorithmType: AlgorithmType
  dataSources: RuleDataSource[]
  params: RuleParam[]
}

// ============================================================
// 数据源
// ============================================================

interface RuleDataSource {
  id?: string
  ruleId?: string
  deviceId: string
  deviceName?: string
  pointCodes?: string           // JSON 字符串，如 '["TEMP","HUMID"]'
  timeRangeStart?: string       // 格式：HH:mm:ss 或 yyyy-MM-dd HH:mm:ss
  timeRangeEnd?: string
  createTime?: string
}

// 前端处理时建议使用带解析的中间类型
interface RuleDataSourceForm {
  deviceId: string
  deviceName?: string
  pointCodes: string[]          // 已解析为数组
  timeRangeStart?: string
  timeRangeEnd?: string
}

// ============================================================
// 规则参数
// ============================================================

interface RuleParam {
  id?: string
  ruleId?: string
  paramKey: string
  paramValue: string
  paramDesc?: string
}

// ============================================================
// 算法
// ============================================================

interface RuleAlgorithm {
  id?: string
  algorithmName: string
  algorithmDesc?: string
  algorithmType: AlgorithmType
  algorithmPath?: string        // 只读，由后端维护
  algorithmClass?: string       // algorithmType = 'jar' 时必填
  algorithmVersion?: string
  algorithmStatus?: AlgorithmStatus
  fileSize?: number             // 只读，单位：字节
  createTime?: string
  updateTime?: string
}

// ============================================================
// 执行日志
// ============================================================

interface RuleExecutionLog {
  id: string
  ruleId: string
  startTime: string
  endTime: string
  execStatus: ExecStatus
  resultData?: string           // JSON 字符串
  errorMsg?: string
  durationMs: number
}
```

---

## 5. 页面设计说明

### 5.1 规则列表页

**核心功能**

- 分页展示规则，支持按 `ruleName`、`algorithmId` 筛选
- 列表显示字段：规则名称、算法名称/类型、触发方式、窗口配置、状态、操作时间
- 状态列用 Badge 区分：运行中（绿色）/ 已停止（灰色）
- 操作按钮：启动 / 停止 / 详情 / 删除
  - 运行中时：只显示「停止」，禁用「启动」「删除」
  - 已停止时：只显示「启动」，禁用「停止」

**交互说明**

- 点击「启动」后，按钮变为 loading 状态，成功后刷新该行状态
- 删除前弹出二次确认弹窗，展示规则名称
- 「详情」跳转到规则详情页

---

### 5.2 规则新增 / 编辑页（三步向导）

建议将规则创建拆为三个步骤：

#### 第一步：基本配置

| 字段 | 组件 | 说明 |
|------|------|------|
| 规则名称 | Input | 必填，不可重复 |
| 规则描述 | Textarea | 选填 |
| 绑定算法 | Select（可搜索） | 下拉选项来自算法列表，仅展示 `algorithmStatus=1` 的算法；选中后展示算法类型 |
| 触发类型 | Radio | `periodic` / `realtime` |
| Cron 表达式 | Input | `triggerType=periodic` 时显示，建议提供 Cron 编辑器 |
| 窗口类型 | Select | `tumbling` / `sliding` / `session` |
| 窗口大小 | InputNumber | 正整数 |
| 窗口步长 | InputNumber | `windowType=sliding` 时显示，必须小于窗口大小 |
| 窗口单位 | Select | `s` / `m` / `h` / `d` |
| 分组策略 | Radio | `device_point` / `device` |
| 并行度 | InputNumber | 正整数，默认 `1`，建议范围 `1~16` |

#### 第二步：数据源配置

- 每行为一个设备配置，支持增减行
- 字段：设备 ID（关联设备选择器）、设备名称（自动回填）、测点编码（多选 Tag）、时间段起止
- `pointCodes` 为空时，后端读取该设备全部测点

#### 第三步：规则参数

- 键值对表格，支持增减行
- 字段：参数键（paramKey）、参数值（paramValue）、描述（paramDesc）
- 算法所需参数由算法开发者提供说明，前端动态录入

**编辑时注意**：运行中的规则不可编辑，编辑页需在页面加载时判断 `ruleStatus`，若为 `1` 则展示只读视图并提示用户先停止。

---

### 5.3 规则详情页

展示规则完整配置（只读），包含：

1. **基本信息**：规则名称、描述、触发类型、窗口配置、算法信息
2. **运行状态卡片**：
   - 数据库状态（`ruleStatus`）
   - Flink Job 实时状态（`/flink-status` 接口，轮询间隔 5s，仅运行中时轮询）
   - Flink Job ID（可复制）
3. **数据源列表**：设备信息 + 测点 + 时间段
4. **规则参数列表**：键值对展示
5. **执行日志标签页**：内嵌执行日志列表（固定 `ruleId` 查询）

---

### 5.4 算法管理页

**列表字段**：算法名称、类型（JAR / Python Badge）、版本、文件大小、状态、创建时间

**操作**：上传 / 编辑 / 启用 / 禁用 / 删除

**上传表单**

| 字段 | 组件 | 说明 |
|------|------|------|
| 算法文件 | Upload（拖拽/点击） | 接受 `.jar` / `.py` |
| 算法名称 | Input | 选填，不填则从文件名推断 |
| 算法描述 | Textarea | 选填 |
| 算法类型 | Select | 选填，不填则从后缀推断，上传后展示识别结果 |
| 入口类 | Input | `algorithmType=jar` 时必填，示例：`com.example.MyAlgorithm` |
| 版本号 | Input | 默认 `1.0.0` |

> 文件大小建议前端限制在 **50MB** 以内，超出时给出友好提示。

---

### 5.5 执行日志页

**筛选条件**：规则名称（转换为 `ruleId`）、执行状态（下拉）、时间范围

**列表字段**：规则名称、开始时间、结束时间、耗时、执行状态、结果数据（折叠展示 JSON）

**状态渲染**

| execStatus | 展示 |
|-----------|------|
| `0` | 蓝色 Badge「执行中」 |
| `1` | 绿色 Badge「成功」 |
| `2` | 红色 Badge「失败」，展示 `errorMsg` |

---

## 6. 业务流程

### 6.1 创建并运行一条规则的完整流程

```
1. 上传/选择算法
   POST /rule-algorithm/upload
   → 获取 algorithmId

2. 创建规则基本配置
   POST /rule-config
   → 获取 ruleId

3. 保存数据源
   POST /rule-config/{ruleId}/data-sources

4. 保存规则参数（可选）
   POST /rule-config/{ruleId}/params

5. 启动规则
   PUT /rule-config/{ruleId}/start

6. 监控运行状态
   GET /rule-config/{ruleId}/flink-status  (轮询)
   GET /rule-execution-log/list?ruleId=xxx (查看日志)
```

### 6.2 规则状态机

```
         启动
   停止 ──────► 运行中
    ▲               │
    │    停止        │
    └───────────────┘
```

- 只有「已停止」状态才能启动
- 只有「运行中」状态才能停止
- 删除操作只允许在「已停止」状态进行

### 6.3 数据源 `pointCodes` 字段处理

后端存储为 JSON 字符串，前端需双向转换：

```typescript
// 读取时：string → string[]
const codes: string[] = dataSource.pointCodes
  ? JSON.parse(dataSource.pointCodes)
  : []

// 提交时：string[] → string
const payload = {
  ...form,
  pointCodes: JSON.stringify(form.pointCodes)
}
```

---

## 7. 字段校验规则

### 规则配置

| 字段 | 规则 |
|------|------|
| `ruleName` | 必填，长度 1~64，不可重复（后端返回错误信息时展示） |
| `algorithmId` | 必填 |
| `triggerType` | 必填 |
| `triggerCron` | `triggerType=periodic` 时必填，建议使用 Cron 验证库 |
| `windowType` | 必填 |
| `windowSize` | 必填，正整数，最小 `1` |
| `windowSlide` | `windowType=sliding` 时必填，正整数，须小于 `windowSize` |
| `windowUnit` | 必填 |
| `keyStrategy` | 必填 |
| `parallelism` | 必填，正整数，范围 `1~32` |

### 算法上传

| 字段 | 规则 |
|------|------|
| `file` | 必填，扩展名必须为 `.jar` 或 `.py` |
| `algorithmClass` | `algorithmType=jar` 时必填，格式为 Java 全限定类名（含点号） |
| `algorithmVersion` | 格式建议 `x.y.z`（如 `1.0.0`） |

### 数据源

| 字段 | 规则 |
|------|------|
| `deviceId` | 必填 |
| `timeRangeStart` / `timeRangeEnd` | 成对出现，格式 `HH:mm:ss`；若填写 `End` 则 `Start` 也必填，且 `Start < End` |

### 规则参数

| 字段 | 规则 |
|------|------|
| `paramKey` | 必填，同一规则下不可重复 |
| `paramValue` | 必填 |
