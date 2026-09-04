# OfferPilot 双记忆模式

OfferPilot 使用同一套问答接口，通过环境变量选择记忆后端：

- `lightweight`（默认）：PostgreSQL 保存对话，Redis 缓存最近 12 条消息。适合云端小内存服务器。
- `tencentdb`：在保留本地 PostgreSQL 副本的同时，将消息双写到 TencentDB Agent Memory，并在回答前调用 Memory Core 召回。Memory Core 不可用时自动回退到 lightweight。

## 云端轻量模式

在 `backend/.env` 中设置：

```env
MEMORY_MODE=lightweight
```

随后按现有 CI/CD 部署。Flyway 会自动执行 `V5__conversation_memory.sql`。

## Windows 本地复杂模式

1. 按 TencentDB Agent Memory 官方文档在 Docker Desktop/WSL2 中启动完整套件。
2. 确认 Memory Core 可从 Windows 访问，例如 `http://localhost:8420`。
3. OfferPilot 后端运行在 Docker 中，因此在 `backend/.env` 中填写：

```env
MEMORY_MODE=tencentdb
MEMORY_BASE_URL=http://host.docker.internal:8420
MEMORY_TEAM_ID=offerpilot
MEMORY_AGENT_ID=offer-advisor
```

4. 启动 OfferPilot：

```powershell
cd backend
docker compose up -d --build
docker compose ps
```

如果 OfferPilot 后端不在 Docker 中，而是直接用 Maven 启动，把地址改成：

```env
MEMORY_BASE_URL=http://localhost:8420
```

## 验证

连续问两次：

1. `记住我更看重技术成长，期望城市是杭州。`
2. `根据我刚才的偏好分析这个 Offer。`

数据库检查：

```sql
SELECT conversation_id, role, left(content, 80), created_at
FROM chat_messages
ORDER BY created_at DESC
LIMIT 10;
```

浏览器会在 `localStorage` 中保存 `offerpilot.conversationId`。删除该值会开始新的会话。

