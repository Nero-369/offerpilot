#!/usr/bin/env sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
cd "$ROOT"
BRANCH="${DEPLOY_BRANCH:-main}"
BEFORE="$(git rev-parse HEAD)"

echo "[1/4] 拉取 origin/$BRANCH"
git pull --ff-only origin "$BRANCH"
AFTER="$(git rev-parse HEAD)"

if [ "$BEFORE" = "$AFTER" ]; then
  echo "代码没有变化，无需重新构建。"
  cd backend && ./deploy.sh status
  exit 0
fi

CHANGED="$(git diff --name-only "$BEFORE" "$AFTER")"
FRONTEND=false
BACKEND=false
echo "$CHANGED" | grep -q '^frontend/' && FRONTEND=true || true
echo "$CHANGED" | grep -Eq '^backend/|^git-deploy\.sh$' && BACKEND=true || true

echo "[2/4] 变更文件"
echo "$CHANGED"
cd backend
chmod +x deploy.sh

echo "[3/4] Docker 增量构建"
if [ "$FRONTEND" = true ] && [ "$BACKEND" = true ]; then
  ./deploy.sh all
elif [ "$FRONTEND" = true ]; then
  ./deploy.sh frontend
elif [ "$BACKEND" = true ]; then
  ./deploy.sh backend
else
  echo "仅文档发生变化，不重启服务。"
fi

echo "[4/4] 服务状态"
./deploy.sh status
