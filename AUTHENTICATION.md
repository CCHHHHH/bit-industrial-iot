# 微服务鉴权模块实现说明

## 📋 架构设计

本项目采用 **网关统一鉴权 + 服务层二次验证** 的两层鉴权架构：

### 1. **第一层：Gateway 网关统一鉴权** (`iot-gateway-service`)
- 所有外部请求的第一道防线
- 使用 WebFlux GlobalFilter 进行 Token 验证
- 将用户信息传递到下游服务

### 2. **第二层：服务层安全组件** (`common-security` + `iot-system-service`)
- 封装通用安全组件（权限注解、AOP 切面）
- 从网关传递的 Header 中获取用户信息
- 支持基于权限注解的细粒度权限控制

---

## 📁 模块划分

### **common-security 模块** - 公共安全组件
位置：`/iot-common/common-security/`

包含内容：
- ✅ `UserContext` - 用户上下文信息类
- ✅ `UserContextHolder` - 用户上下文持有者（ThreadLocal）
- ✅ `RequirePermission` - 权限注解
- ✅ `PermissionAspect` - 权限验证 AOP 切面

依赖：
- JWT (io.jsonwebtoken)
- Spring Web (provided)
- Spring AOP (provided)
- Lombok (provided)

---

### **iot-gateway-service 模块** - 网关层鉴权
位置：`/iot-gateway-service/`

核心组件：
- ✅ `TokenValidationFilter` - Token 验证全局过滤器（GlobalFilter）

功能：
1. 拦截所有进入的请求
2. 验证 Token 有效性
3. 从 Token 中解析用户信息
4. 将用户信息添加到请求头，传递给下游服务
5. 白名单机制（登录接口、Swagger 文档等无需鉴权）

---

### **iot-system-service 模块** - 业务服务层
位置：`/iot-system-service/`

核心组件：
- ✅ `UserContextInterceptor` - 用户上下文拦截器
- ✅ `WebConfig` - Web 配置类（注册拦截器）
- ✅ 示例 Controller 方法（使用 `@RequirePermission` 注解）

功能：
1. 从网关传递的 Header 中获取用户信息
2. 将用户信息存入 ThreadLocal
3. 支持使用 `@RequirePermission` 注解进行权限验证
4. 请求结束后自动清理 ThreadLocal

---

## 🔄 请求流程

```
客户端请求
    ↓
[Gateway 网关]
    ↓ 验证 Token
    ↓ 添加用户信息到 Header (X-User-Id, X-Username)
    ↓
[iot-system-service]
    ↓
[UserContextInterceptor]
    ↓ 从 Header 获取用户信息
    ↓ 存入 UserContextHolder (ThreadLocal)
    ↓
[Controller / Service]
    ↓ 可通过 @RequirePermission 进行权限验证
    ↓ 可通过 UserContextHolder.getContext() 获取当前用户
    ↓
[响应返回]
    ↓
[UserContextInterceptor.afterCompletion]
    ↓ 清理 ThreadLocal
```

---

## 🔐 Token 机制

### Token 生成
- 位置：`common-core/src/main/java/bit/iot/common/utils/TokenUtil.java`
- 算法：JWT (HS256)
- 有效期：1 小时（已修改）
- 密钥：`bit-industrial-iot-secret-key-20260309`（建议改为配置文件读取）

### Token 传递方式（支持三种）
1. **Authorization Header**: `Bearer eyJhbGci...`
2. **Token Header**: `token: eyJhbGci...`
3. **Query Parameter**: `?token=eyJhbGci...`

---

## 🎯 使用示例

### 1. 登录获取 Token
```bash
POST http://localhost:8080/user/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}

# 响应示例：
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": "123",
    "username": "admin",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "roles": ["role1", "role2"],
    "permissions": ["user:add", "user:edit"]
  }
}
```

### 2. 携带 Token 访问接口
```bash
# 方式 1：Authorization Header
GET http://localhost:8080/user/list
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# 方式 2：Token Header
GET http://localhost:8080/user/list
token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# 方式 3：URL 参数
GET http://localhost:8080/user/list?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 3. 使用权限注解
```java
@GetMapping("/admin")
@ApiOperation("管理员接口（需要权限）")
@RequirePermission(value = {"admin:manage"}, requireLogin = true)
public Result<String> adminMethod() {
    // 只有拥有 admin:manage 权限的用户才能访问
    return success("管理员操作成功");
}

@GetMapping("/current")
@ApiOperation("获取当前登录用户信息")
@RequirePermission(requireLogin = true)
public Result<String> getCurrentUserInfo() {
    var context = UserContextHolder.getContext();
    return success("当前用户：" + context.getUsername());
}
```

---

## ⚙️ 配置说明

### Gateway 白名单路径
在 `TokenValidationFilter.isWhitelist()` 方法中配置：
- `/user/login` - 登录接口
- `/error` - 错误页面
- `/swagger/**` - Swagger 文档
- `/v2/api-docs` - Swagger API
- `/v3/api-docs` - OpenAPI
- `/webjars/**` - Swagger 静态资源
- `/doc.html` - Knife4j 文档

如需添加其他免鉴权路径，请在此方法中添加。

---

## 🚨 注意事项

### 1. ThreadLocal 内存泄漏防护
- `UserContextInterceptor` 会在 `afterCompletion` 中自动清理 ThreadLocal
- 手动使用 `UserContextHolder.setContext()` 后必须调用 `clearContext()`

### 2. 密钥安全
- 当前密钥硬编码在代码中
- **生产环境必须改为从配置文件或环境变量读取**
- 建议使用更复杂的密钥字符串

### 3. Token 有效期
- 当前设置为 1 小时
- 可根据业务需求调整（在 `TokenUtil.EXPIRATION_TIME` 中修改）

### 4. 角色和权限传递
- 当前网关层暂未传递角色和权限信息（为了性能考虑）
- 下游服务通过查询数据库获取用户的角色和权限
- 如需在网关层传递，可取消注释 `TokenValidationFilter` 中的相关代码

---

## 📦 依赖管理

### 父 pom.xml
统一管理 JWT 版本：`0.13.0`

### common-security/pom.xml
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <scope>runtime</scope>
</dependency>
```

### iot-gateway-service/pom.xml
```xml
<dependency>
    <groupId>com.bit</groupId>
    <artifactId>common-security</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### iot-system-service/pom.xml
```xml
<dependency>
    <groupId>com.bit</groupId>
    <artifactId>common-security</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>
</dependency>
```

---

## 🎉 完成清单

✅ **common-security 模块**
- [x] UserContext - 用户上下文信息类
- [x] UserContextHolder - 用户上下文持有者
- [x] RequirePermission - 权限注解
- [x] PermissionAspect - 权限验证 AOP 切面

✅ **iot-gateway-service 模块**
- [x] TokenValidationFilter - Token 验证全局过滤器
- [x] 白名单机制
- [x] 用户信息传递到下游服务

✅ **iot-system-service 模块**
- [x] UserContextInterceptor - 用户上下文拦截器
- [x] WebConfig - Web 配置类
- [x] 示例 Controller 方法
- [x] 依赖注入

✅ **公共组件**
- [x] TokenUtil - JWT 工具类（适配 0.13.0 版本）
- [x] MD5Util - 密码加密工具类

---

## 🔧 后续优化建议

1. **Redis 缓存**
   - 将 Token 和用户信息存入 Redis
   - 支持 Token 黑名单机制
   - 实现分布式会话管理

2. **动态权限**
   - 权限数据缓存到 Redis
   - 避免每次请求都查询数据库

3. **OAuth2/OIDC**
   - 集成第三方登录
   - 支持单点登录（SSO）

4. **限流和熔断**
   - 在网关层实现限流
   - 防止恶意攻击

5. **日志审计**
   - 记录所有敏感操作
   - 便于问题追踪和安全审计

---

## 👤 作者
chenhao  
2026/3/9
