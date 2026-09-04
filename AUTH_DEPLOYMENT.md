# OfferPilot 登录与 Session 部署

## 统一架构

- PostgreSQL 保存用户资料与 BCrypt 密码哈希。
- Spring Security 负责认证、鉴权、CSRF 与退出登录。
- Spring Session 将登录状态保存到 Redis，默认 12 小时过期。
- 浏览器仅保存 `OFFERPILOT_SESSION` HttpOnly Cookie，不保存 JWT 或密码。
- 后端从 Session 读取用户 ID，并以此隔离对话和长期记忆。

雪花算法不参与登录或 Session。现有 UUID 主键继续保留，避免不必要的数据迁移和 JavaScript `BIGINT` 精度问题。

## 本地部署

```powershell
cd backend
Copy-Item .env.local.example .env
docker compose up -d --build
docker compose ps
curl.exe http://localhost/actuator/health
```

浏览器访问 `http://localhost`。本地使用 HTTP，因此 `SESSION_COOKIE_SECURE=false`。

## 云端部署

首次复制配置并填写真实密码和 Qwen Key：

```bash
cd offerpilot/backend
cp .env.cloud.example .env
docker compose up -d --build
docker compose ps
curl http://127.0.0.1/actuator/health
```

当前仅通过公网 IP 的 HTTP 访问时，暂时使用 `SESSION_COOKIE_SECURE=false`。配置域名和 HTTPS 后必须改为 `true`，再执行：

```bash
docker compose up -d --force-recreate backend nginx
```

## 验证用户隔离

1. 注册用户 A，进行一次顾问问答，然后退出。
2. 注册或登录用户 B；B 不应看到 A 的对话，也不应召回 A 的记忆。
3. 再登录 A，应恢复 A 自己的长期记忆。
4. 在 Redis 中可用 `SCAN 0 MATCH offerpilot:session:*` 确认 Session 存在；退出后对应 Session 应失效。

