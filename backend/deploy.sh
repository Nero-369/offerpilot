#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
ACTION="${1:-all}"
export COMPOSE_PARALLEL_LIMIT=1
case "$ACTION" in
  frontend) docker compose build frontend; docker compose up -d --force-recreate frontend nginx ;;
  backend) docker compose build backend; docker compose up -d --force-recreate backend nginx ;;
  all) docker compose build frontend; docker compose build backend; docker compose up -d --force-recreate backend frontend nginx ;;
  status) docker compose ps ;;
  logs) docker compose logs --tail=200 -f "${2:-backend}" ;;
  *) echo "用法: ./deploy.sh {frontend|backend|all|status|logs [service]}"; exit 1 ;;
esac
