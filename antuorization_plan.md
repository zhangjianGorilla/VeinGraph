# 用户认证与权限系统设计方案（OAuth2 + 本地注册双模式）

## 目标

为 VeinGraph 引入**双模式认证**：
- **模式 A**：GitHub / Gitee OAuth2 社交一键登录（开源社区友好）
- **模式 B**：传统用户名 + 密码注册/登录（私有化部署友好）
- 两种模式统一汇入 **JWT 会话管理**，文档按用户隔离

## 整体认证流程

```
┌──────────────────────────────────────────────────────┐
│                     前端登录页                         │
│                                                      │
│   ┌─────────────┐        ┌──────────────────────┐    │
│   │ GitHub 登录  │        │  用户名/密码 登录/注册 │    │
│   └──────┬──────┘        └──────────┬───────────┘    │
└──────────┼──────────────────────────┼────────────────┘
           │                          │
           ▼                          ▼
  ┌─────────────────┐      ┌──────────────────┐
  │ /oauth2/authorize│      │ POST /auth/login │
  │ → GitHub/Gitee   │      │ → 校验BCrypt密码  │
  │ → 回调获取用户信息 │      │                  │
  └────────┬────────┘      └────────┬─────────┘
           │                        │
           ▼                        ▼
     ┌──────────────────────────────────┐
     │  统一用户模型 SysUser (MongoDB)    │
     │  首次登录自动创建 / 查找已有用户    │
     └────────────────┬─────────────────┘
                      │
                      ▼
              ┌───────────────┐
              │  签发 JWT 令牌  │
              └───────┬───────┘
                      │
                      ▼
            ┌──────────────────┐
            │ 前端存储 Token     │
            │ Axios 自动注入    │
            │ Authorization头   │
            └──────────────────┘
```

---

## Proposed Changes

### 组件 1：Maven 依赖

#### [MODIFY] [pom.xml](file:///c:/Code/VeinGraph/pom.xml)

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<!-- OAuth2 Client (GitHub/Gitee 社交登录) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
<!-- JJWT (JWT 签发与解析) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

---

### 组件 2：application.yml 配置

#### [MODIFY] [application.yml](file:///c:/Code/VeinGraph/src/main/resources/application.yml)

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID:your-github-client-id}
            client-secret: ${GITHUB_CLIENT_SECRET:your-github-client-secret}
            scope: read:user,user:email
          gitee:
            client-id: ${GITEE_CLIENT_ID:your-gitee-client-id}
            client-secret: ${GITEE_CLIENT_SECRET:your-gitee-client-secret}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/api/login/oauth2/code/{registrationId}"
            scope: user_info
        provider:
          gitee:
            authorization-uri: https://gitee.com/oauth/authorize
            token-uri: https://gitee.com/oauth/token
            user-info-uri: https://gitee.com/api/v5/user
            user-name-attribute: login

veingraph:
  jwt:
    secret: ${JWT_SECRET:veingraph-default-secret-key-please-change-in-production}
    expiration: 86400000  # 24小时（毫秒）
```

> [!IMPORTANT]
> GitHub 和 Gitee 的 `client-id` / `client-secret` 需要用户到对应平台的 OAuth Apps 中自行申请，填入 `.env` 文件。

---

### 组件 3：用户数据模型

#### [NEW] `com.veingraph.model.SysUser`

| 字段 | 类型 | 说明 |
|------|------|------|
| [id](file:///c:/Code/VeinGraph/src/main/java/com/veingraph/rag/EsHybridSearchService.java#81-154) | String | MongoDB 主键 |
| `username` | String | 本地注册的用户名 (unique) |
| `password` | String | BCrypt 加密密码（OAuth2 用户此字段为空） |
| `nickname` | String | 显示昵称 |
| `avatar` | String | 头像 URL（OAuth2 登录时自动获取） |
| `role` | String | `ROLE_USER` / `ROLE_ADMIN` |
| `provider` | String | 认证来源：`local` / `github` / `gitee` |
| `providerId` | String | 第三方平台用户 ID |
| `createdAt` | LocalDateTime | 注册时间 |

#### [NEW] `com.veingraph.repository.mongo.SysUserRepository`

---

### 组件 4：JWT 工具类

#### [NEW] `com.veingraph.security.JwtTokenProvider`

- `generateToken(userId, username, role)` → 签发 JWT
- `validateToken(token)` → 校验签名+过期
- `getUserIdFromToken(token)` → 提取用户 ID

---

### 组件 5：Spring Security 核心配置

#### [NEW] `com.veingraph.security.SecurityConfig`

关键配置：
- `/auth/**` 和 `/oauth2/**` 放行（登录注册 + OAuth2 回调）
- `/actuator/**` 和 Swagger 文档放行
- SSE 流式接口 `/chat/stream` 放行（前端通过 URL 参数传 token）
- 其余接口全部 authenticated
- 注册 `JwtAuthenticationFilter` 在 `UsernamePasswordAuthenticationFilter` 之前
- 配置 `OAuth2LoginSuccessHandler` 处理社交登录成功后的 JWT 签发

#### [NEW] `com.veingraph.security.JwtAuthenticationFilter`

拦截请求 → 提取 Bearer Token → 验证 → 注入 SecurityContext

#### [NEW] `com.veingraph.security.OAuth2LoginSuccessHandler`

OAuth2 社交登录成功后的回调处理器：
1. 从 OAuth2User 中提取用户信息（头像、昵称、平台 ID）
2. 查找 MongoDB 中是否已有该 `provider + providerId` 的用户
3. 没有则自动创建 `SysUser`（provider=github/gitee）
4. 签发 JWT
5. **重定向到前端页面并通过 URL 参数传递 Token**：`http://localhost:5173/oauth/callback?token=xxx`

---

### 组件 6：认证控制器

#### [NEW] `com.veingraph.controller.AuthController`

| 接口 | 方法 | 说明 |
|------|------|------|
| `POST /auth/register` | 注册 | username + password + nickname → BCrypt 加密存库 |
| `POST /auth/login` | 登录 | 校验密码 → 签发 JWT |
| `GET /auth/me` | 个人信息 | 从 Token 解析当前用户 |

---

### 组件 7：文档权限隔离

#### [MODIFY] [DocumentMeta.java](file:///c:/Code/VeinGraph/src/main/java/com/veingraph/model/DocumentMeta.java)

新增 `@Indexed private String userId` 字段。

#### [MODIFY] [DocumentController.java](file:///c:/Code/VeinGraph/src/main/java/com/veingraph/controller/DocumentController.java)

- 上传时自动注入 `currentUserId`
- 列表查询改为 `findByUserId(currentUserId)`

#### [MODIFY] [GraphController.java](file:///c:/Code/VeinGraph/src/main/java/com/veingraph/controller/GraphController.java)

- 图谱查询时验证文档归属权

#### [NEW] `com.veingraph.security.SecurityUtils`

```java
public static String getCurrentUserId() {
    // 从 SecurityContextHolder 提取当前 JWT 中的 userId
}
```

---

### 组件 8：前端适配

#### [NEW] `web/src/views/Login.vue`

暗黑科幻风的登录页面，包含：
- 用户名/密码表单（本地注册/登录）
- GitHub 登录按钮（跳转 OAuth2 授权）
- Gitee 登录按钮（跳转 OAuth2 授权）

#### [NEW] `web/src/views/OAuthCallback.vue`

OAuth2 回调落地页：从 URL 参数中提取 Token → 存入 localStorage → 跳转主页

#### [MODIFY] [web/src/App.vue](file:///c:/Code/VeinGraph/web/src/App.vue)

- 增加 vue-router 路由守卫
- 未认证用户强制跳转 `/login`

#### [MODIFY] Axios 配置

- 请求拦截器：自动注入 `Authorization: Bearer <token>`
- 响应拦截器：401 自动跳转登录页

---

## .env.example 更新

```env
# OAuth2 社交登录（可选，不配置则不启用对应的社交登录按钮）
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret
GITEE_CLIENT_ID=your-gitee-client-id
GITEE_CLIENT_SECRET=your-gitee-client-secret

# JWT 密钥（生产环境必须修改）
JWT_SECRET=your-random-secret-key
```

---

## Verification Plan

### 编译验证
```bash
mvn compile -q
```

### 本地注册/登录测试
1. `POST /auth/register` → 注册成功
2. `POST /auth/login` → 获取 JWT
3. 不带 Token 访问 `/documents` → 401
4. 带 Token 访问 → 仅返回当前用户数据

### OAuth2 登录测试
1. 点击 GitHub 登录按钮 → 跳转 GitHub 授权页
2. 授权后回调 → 自动创建用户 → 签发 JWT → 跳转主页
3. 验证用户头像和昵称正确显示
