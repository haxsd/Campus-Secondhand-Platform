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
├── docs/       设计文档（实施方案 / API 契约 / 数据库设计）
└── deploy/     部署配置（docker-compose / nginx / 建表 SQL）
```

## 开发约定

- 前后端所有接口以 `docs/API接口文档.md` 为唯一契约：先改文档，再改代码。
- 数据库结构见 `docs/数据库设计文档.md`，建表脚本 `deploy/sql/init.sql`。
- 开发流程与里程碑见 `docs/全栈实施方案.md`。

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
