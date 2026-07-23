# backend · Spring Boot 后端

在此目录初始化 Spring Boot 3 工程（Java 17 + Maven + MyBatis）。

模块规划（见 docs/全栈实施方案.md 第 4 节）：

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

接口契约：docs/API接口文档.md；建表：deploy/sql/init.sql。
