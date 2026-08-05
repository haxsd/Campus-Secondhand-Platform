# 校园二手交易平台

这是一个面向校内场景的校园二手交易平台，采用 Spring Boot 单体后端和
Vue 3 前端，为学生提供商品发布、审核、下单、线下见面交易、评价和纠纷处理
能力。平台只负责校内信息撮合和交易过程管理，**不包含在线支付、退款、物流或
资金托管**；实际交易在线下完成。

## 目录结构

```text
.
├─ backend/
│  ├─ src/main/java/com/campus/trade/
│  │  ├─ auth/             认证、登录态和 JWT
│  │  ├─ product/          商品发布、审核、库存和详情
│  │  ├─ order/            下单、订单状态机和超时关单
│  │  ├─ review/           交易评价和信用分
│  │  ├─ dispute/          纠纷申请、证据和管理员处理
│  │  ├─ file/             本地文件上传和访问
│  │  └─ config/           Redis、JWT、Web 和文件存储等配置
│  ├─ src/main/resources/
│  │  └─ mapper/           MyBatis XML 映射文件
│  └─ docs/                 后端工程学习资料和数据库升级记录
├─ frontend/
│  ├─ src/api/              后端 API 封装，支持真实接口和 Mock 切换
│  ├─ src/mock/             Mock 数据和本地演示接口
│  ├─ src/views/            页面级 Vue 视图
│  ├─ src/stores/           Pinia 状态管理
│  ├─ src/router/           前端路由
│  └─ src/components/       可复用业务组件
├─ docs/
│  ├─ API接口文档.md        HTTP API、请求参数和响应说明
│  ├─ 前端设计文档.md       前端页面、交互和视觉设计
│  ├─ 后端设计文档.md       后端模块、流程和实现设计
│  └─ 数据库设计文档.md     表结构、关系和索引设计
└─ deploy/
   ├─ sql/                  MySQL 初始化数据和测试种子数据
   ├─ nginx/                nginx 静态文件和 API 反向代理配置
   ├─ rocketmq/             RocketMQ 独立 Compose 和 broker.conf
   ├─ docker-compose.yml    MySQL、Redis、nginx 编排
   ├─ build.ps1             构建后端 JAR 和前端 dist
   ├─ start.ps1             启动依赖、RocketMQ、nginx 和后端
   ├─ stop.ps1              停止本项目服务
   ├─ .env.example          本机部署配置模板
   └─ 本机部署手册.md       Windows PowerShell 分步部署说明
```

`backend/`、`frontend/` 是应用源码，`docs/` 是设计和接口资料，`deploy/` 是
本机运行所需的基础设施和脚本。RocketMQ 保持独立 Compose，不合并进
`deploy/docker-compose.yml`。

## 技术栈

### 后端

- Java 17+，Spring Boot 3.5
- MyBatis（不是 MyBatis-Plus）
- MySQL 8、Redis 7
- Caffeine + Redis 二级缓存
- Redisson 分布式锁和并发控制
- JWT 登录认证
- RocketMQ 5.3.2：订单超时延迟消息，定时扫描任务兜底
- Spring Boot Actuator 健康检查

### 前端

- Vue 3
- Vite
- Pinia
- Vue Router
- Element Plus
- Axios

## 核心能力

- JWT + Redis 保存登录态并支持会话失效；
- Caffeine + Redis 二级缓存，结合 Redisson 防止缓存击穿；
- 下单使用 `requestId` 和数据库唯一约束保证幂等，库存通过条件更新防止超卖；
- 订单状态机约束待确认、已确认、已完成、取消和纠纷状态流转；
- RocketMQ 延迟消息驱动订单超时关单，定时扫描任务负责兜底；
- 交易完成后评价并更新信用分；
- 纠纷申请、证据链和管理员处理记录；
- 本地文件上传，使用文件魔数校验内容类型。

## Compose 编排

Compose 文件用 YAML 描述多个容器、网络、卷和健康依赖，可以通过一条命令
创建并启动一组服务。本项目将应用基础设施和 RocketMQ 分为两个独立的
Compose 项目：

```text
campus-trade       MySQL、Redis、nginx
campus-trade-mq    RocketMQ
```

其中：

- `deploy/docker-compose.yml` 管理 MySQL、Redis 和 nginx；
- `deploy/rocketmq/docker-compose.yml` 使用项目名 `campus-trade-mq`，管理
  NameServer、Broker、Proxy、
  `rocketmq-permissions` 和 `topic-init`；
- RocketMQ 会自动初始化 `campus_trade_order_timeout` 延迟 Topic；
- 两个 Compose 项目彼此独立，不会互相识别对方的容器为 orphan；
- 不要使用 `--remove-orphans`，避免误处理其他 Compose 项目的容器。

## 快速开始

以下命令均从工程根目录执行。首次构建会下载 Maven、Node 依赖和 Docker 镜像，
可能需要较长时间。

```powershell
Copy-Item deploy\.env.example deploy\.env
notepad deploy\.env

.\deploy\build.ps1
.\deploy\start.ps1
```

然后访问：

```text
http://localhost
```

停止本项目：

```powershell
.\deploy\stop.ps1
```

如果本机已经存在 `ct-mysql` 或 `ct-redis` 并占用 3306/6379，不要直接启动
Compose 中对应的 MySQL/Redis 服务，应按手册检查并复用已有容器。

完整的前置检查、环境变量、已有容器复用、RocketMQ 验证、数据库初始化、黄金
路径和故障排查，请阅读：

```text
deploy/本机部署手册.md
```

## 测试账号

测试账号由 `deploy/sql/initial-data.sql` 初始化，统一密码为：

```text
Campus@2026
```

账号：

```text
管理员：admin001
普通用户：20230001 - 20230012
```

## 构建与测试

后端使用 Maven Wrapper，不要求预装 Maven：

```powershell
.\backend\mvnw.cmd -B clean package -DskipTests
.\backend\mvnw.cmd -B test -DrunExternalTests=true
```

前端命令：

```powershell
Push-Location frontend
npm ci
npm run lint
npm run build
Pop-Location
```

前端生产构建产物位于 `frontend/dist/`，该目录由用户本机自行构建，不纳入
版本管理。
