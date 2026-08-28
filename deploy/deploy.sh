#!/usr/bin/env bash
# 本地构建并发布到生产服务器
# 用法: ./deploy/deploy.sh [all|backend|h5|admin]
set -euo pipefail

SERVER=root@YOUR_SERVER_IP
REMOTE_DIR=/opt/recycle
TARGET=${1:-all}
ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)

deploy_backend() {
  echo ">>> 构建后端 jar"
  (cd "$ROOT_DIR/backend" && mvn -q clean package -DskipTests)
  echo ">>> 上传后端与编排文件"
  scp "$ROOT_DIR/backend/target/backend.jar" "$SERVER:$REMOTE_DIR/backend/target/backend.jar"
  scp "$ROOT_DIR/backend/Dockerfile" "$SERVER:$REMOTE_DIR/backend/Dockerfile"
  scp "$ROOT_DIR/deploy/docker-compose.prod.yml" "$SERVER:$REMOTE_DIR/deploy/"
  echo ">>> 远端重建 backend 容器"
  ssh "$SERVER" "cd $REMOTE_DIR/deploy && docker compose -f docker-compose.prod.yml up -d --build backend"
  echo ">>> 健康检查"
  for i in $(seq 1 30); do
    if ssh "$SERVER" "curl -sf http://127.0.0.1:8080/actuator/health" | grep -q UP; then
      echo "backend healthy"
      return 0
    fi
    sleep 5
  done
  echo "backend 健康检查超时,请查日志: ssh $SERVER 'docker logs --tail 200 recycle-backend'"
  exit 1
}

deploy_h5() {
  echo ">>> 构建 H5"
  (cd "$ROOT_DIR/app-uni" && pnpm install --frozen-lockfile && pnpm build:h5)
  echo ">>> 发布 H5 静态资源"
  rsync -az --delete "$ROOT_DIR/app-uni/dist/build/h5/" "$SERVER:/var/www/h5/"
}

deploy_admin() {
  echo ">>> 构建管理后台"
  (cd "$ROOT_DIR/admin-web" && pnpm install --frozen-lockfile && pnpm build)
  echo ">>> 发布管理后台静态资源"
  rsync -az --delete "$ROOT_DIR/admin-web/dist/" "$SERVER:/var/www/admin/"
}

case "$TARGET" in
  backend) deploy_backend ;;
  h5) deploy_h5 ;;
  admin) deploy_admin ;;
  all) deploy_backend; deploy_h5; deploy_admin ;;
  *) echo "用法: $0 [all|backend|h5|admin]"; exit 1 ;;
esac
echo ">>> 发布完成"
