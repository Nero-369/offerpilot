# OfferPilot Git 发布

## 首次部署

```bash
git clone <仓库地址> /root/offerpilot
cd /root/offerpilot/backend
cp .env.example .env
vi .env
chmod +x deploy.sh ../git-deploy.sh
./deploy.sh all
```

`.env` 保存数据库、Redis 和 Qwen 密钥，已被 `.gitignore` 排除，禁止提交。

## 后续发布

本地提交并推送：

```bash
git add .
git commit -m "feat: describe the change"
git push origin main
```

服务器只运行：

```bash
cd /root/offerpilot
./git-deploy.sh
```

脚本通过 Git diff 判断需要构建的服务。Dockerfile 将 `pom.xml` 和
`package-lock.json` 放在源码之前，因此只改 Java/TSX 时会复用 Maven/npm
依赖层；只有依赖清单变化时才重新下载依赖。

## 常用维护命令

```bash
cd /root/offerpilot/backend
./deploy.sh status
./deploy.sh logs backend
./deploy.sh logs frontend
```
