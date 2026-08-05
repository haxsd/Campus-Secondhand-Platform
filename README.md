### Deployment notes

The production deployment requires JDK 17, Node.js, Docker Desktop, and Windows
PowerShell. nginx runs in Docker by default, so the host does not need a local
nginx installation. The JVM and MySQL must both run in `Asia/Shanghai`;
otherwise `CURRENT_TIMESTAMP` and order confirmation deadlines can drift.

从工程根目录开始的完整本机操作步骤见
[`deploy/本机部署手册.md`](deploy/本机部署手册.md)。该手册覆盖 Docker
前置检查、已有 MySQL/Redis 容器复用、RocketMQ 独立编排、后端打包、
nginx 启动和黄金路径演示。

快速路径：

```powershell
Copy-Item deploy/.env.example deploy/.env
# Edit deploy/.env and replace CT_DB_PASSWORD and CT_JWT_SECRET.
.\deploy\build.ps1
.\deploy\start.ps1
```

`deploy/docker-compose.yml` 负责 MySQL、Redis 和 nginx；独立的
`rocketmq/docker-compose.yml` 负责 NameServer、Broker、Proxy 以及两个
初始化服务。RocketMQ topic 初始化和 volume 权限处理会自动完成。
如果本机已经有 `ct-mysql`、`ct-redis` 占用 3306/6379，请按操作手册跳过
Compose 中的 MySQL/Redis，仅复用并检查已有容器。

打开 `http://localhost/`；`/api` 和 `/uploads` 会由 nginx 反代到后端。
停止本项目服务：

```powershell
.\deploy\stop.ps1
```

RocketMQ 5 使用 Proxy 地址 `127.0.0.1:8081` 和 NameServer 端口 `9876`。
超时 topic 为 `campus_trade_order_timeout`，独立编排中的 topic-init 会将其
自动创建为 DELAY topic，不需要手工执行 `mqadmin`。

Demo accounts are seeded by `deploy/sql/initial-data.sql`:

```text
admin001 / Campus@2026
20230001 through 20230012 / Campus@2026
```
# 校园二手交易平台（Campus Secondhand Platform）

校园二手交易全栈项目：信息撮合 + 线下面交，支持用户注册登录、商品发布与人工审核、库存下单、订单状态机、评价信用与纠纷处理。

## 技术栈

- **后端**：Java 17 + Spring Boot 3 + MyBatis + MySQL 8 + Redis 7（Redisson）
- **前端**：Vue 3 + Vite + Pinia + Vue Router + Element Plus + Axios
- **部署**：Docker Compose + Nginx

## 目录结构

```text
├── backend/    Spring Boot 后端
├── frontend/   Vue 3 前端
├── docs/       设计文档（前端设计 / 后端设计 / API 契约 / 数据库设计）
└── deploy/     部署配置（docker-compose / nginx / 建表 SQL）
```

## 开发约定

- 前后端所有接口以 `docs/API接口文档.md` 为唯一契约：先改文档，再改代码。
- 数据库结构见 `docs/数据库设计文档.md`，建表脚本 `deploy/sql/init.sql`。
- 前端设计见 `docs/前端设计文档.md`，后端设计与部署见 `docs/后端设计文档.md`。

## 本地启动（开发期）

```bash
# 1. 启动依赖（MySQL + Redis）
docker run -d --name ct-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=<你的密码> mysql:8
docker run -d --name ct-redis -p 6379:6379 redis:7
# 2. 建表：执行 deploy/sql/init.sql
# 3. 前端
cd frontend && npm install && npm run dev
# 4. 后端（IDEA 直接运行，或）
cd backend && mvn spring-boot:run
```

## 本机生产部署

### 环境要求

- JDK 17（项目固定以 Java 17 编译）
- Node.js 22.18+ 或 24.12+
- Docker Desktop（MySQL、Redis、RocketMQ）
- Windows PowerShell
- nginx（加入 PATH）

### 一键启动

首次部署时复制配置模板并修改本机配置：

```powershell
Copy-Item deploy/.env.example deploy/.env
notepad deploy/.env
```

至少需要修改：

- `CT_DB_PASSWORD`
- `CT_JWT_SECRET`
- `CT_UPLOAD_DIR`

然后执行：

```powershell
.\deploy\build.ps1
.\deploy\start.ps1
```

访问地址：

```text
http://localhost/
```

停止服务：

```powershell
.\deploy\stop.ps1
```

`deploy/start.ps1` 会启动 MySQL、Redis、RocketMQ NameServer、Broker、Proxy、Spring Boot JAR；nginx 使用 `deploy/nginx/campus-trade.conf` 托管 `frontend/dist` 并反向代理后端。

### 测试账号

初始化脚本中的测试账号密码均为：

```text
管理员：admin001 / Campus@2026
普通用户：20230001 至 20230012 / Campus@2026
```

账号来源：`deploy/sql/initial-data.sql`。

### 手动打包部署

```powershell
.\backend\mvnw.cmd -B clean package -DskipTests
Push-Location frontend
npm ci
npm run build
Pop-Location
```

后端生产 profile 通过环境变量启用：

```powershell
$env:CT_PROFILE = "prod"
java -jar backend\target\campus-trade-0.0.1-SNAPSHOT.jar
```

生产配置文件为 `backend/src/main/resources/application-prod.yml`。数据库、Redis、JWT、上传目录和 RocketMQ 均使用 `CT_*` 环境变量，不应把真实密钥写入脚本或仓库。
