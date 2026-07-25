# backend · Spring Boot 后端

校园二手交易平台后端，使用 Java 17、Spring Boot 3、MyBatis、MySQL 8 和 Redis（Redisson）。

## 当前进度

- 已完成 Spring Boot 与 Maven 工程初始化。
- 已完成统一响应、业务异常、全局异常处理和 Long 转字符串。
- 已连接 MySQL、Redis。
- 已实现公开分类接口 `GET /api/categories`。

## 模块结构

模块规划（见 `docs/后端设计文档.md` 第 2 节）：

```text
src/main/java/com/campus/trade/
├── common/    统一返回、异常、UserContext、常量、工具
├── config/    MyBatis / Redis / 拦截器 / Web 配置
├── auth/      登录注册、JwtProvider、LoginInterceptor
├── user/      用户 + 信用摘要
├── category/  类目
├── product/   商品、图片、审核、库存
├── history/   浏览记录
├── order/     订单、快照、状态机、超时扫描
├── review/    评价 + 信用重算
├── dispute/   纠纷
└── file/      FileStorageService（本地实现，预留 OSS）
```

每个业务模块内部按照 `controller / service / mapper / entity / dto / vo` 分层，
公共响应和异常放在 `common`，框架配置统一放在 `config`。

## 本地运行

默认开发配置与当前 Docker 环境一致：

```text
MySQL: localhost:3306/campus_trade
Redis: localhost:6379（无密码）
后端端口: 8080
```

数据库密码等配置可以通过环境变量覆盖，常用变量如下：

```text
CT_DB_HOST
CT_DB_PORT
CT_DB_NAME
CT_DB_USERNAME
CT_DB_PASSWORD
CT_REDIS_HOST
CT_REDIS_PORT
CT_REDIS_PASSWORD
CT_SERVER_PORT
```

在 IDEA 中使用项目 JDK 17、Maven 运行 `CampusTradeApplication`，或者在已配置 Maven 的终端执行：

```bash
# 运行单元测试
mvn test

# 验证本机 MySQL 和 Redis 真实连接
mvn -DrunExternalTests=true test

# 启动后端
mvn spring-boot:run
```

启动后可访问：

```text
GET http://localhost:8080/api/categories
GET http://localhost:8080/api/actuator/health
```

接口契约：`docs/API接口文档.md`；建表：`deploy/sql/init.sql`。
