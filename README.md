# 文迹

文迹是面向大学生的文化研学与城市探索系统。当前第一阶段已实现项目骨架、MySQL初始化、统一API响应与异常处理、注册登录/JWT鉴权、个人资料，以及文化分类和文化资源的公开查询与管理员维护。

## 技术栈

- 前端：Vue 3、TypeScript、Vite、Vue Router、Pinia、Element Plus、Axios、Vitest
- 后端：Java 21、Spring Boot 3.5、Spring Security、JWT、MyBatis-Plus、MySQL 8、Springdoc OpenAPI、JUnit 5
- 架构：Vue单页应用 + Spring Boot单体REST API + MySQL

## 目录

- `backend/`：后端应用、SQL和集成测试
- `frontend/`：前端应用和单元测试
- `docs/`：实施方案、接口、测试、AI使用与演示文档
- `uploads/`：后续图片上传目录，仅跟踪空目录

完整规划见 [第一阶段实施方案](docs/implementation-plan.md)。

## 环境要求

- Java 17或更高LTS版本，推荐Java 21
- Maven 3.6.3+
- Node.js 20.19+或22.12+，当前环境Node 22.17.0可完成构建
- npm 10+
- MySQL 8.0+

当前Windows环境的 `java -version` 可能仍指向Java 8，但 `JAVA_HOME`、`javac` 与 Maven 使用Java 21。请以 `mvn -version` 为准。PowerShell若禁止执行 `npm.ps1`，使用 `npm.cmd`。

## 数据库初始化

以下命令会重建文迹业务表，仅用于本地开发数据库：

```powershell
mysql -u root -p --execute="source backend/src/main/resources/schema.sql"
mysql -u root -p --execute="source backend/src/main/resources/data.sql"
```

也可先手工创建 `wenji` 数据库，再设置 `DB_INIT_MODE=always` 让后端启动时加载SQL。生产环境不要使用自动重建模式。

## 环境变量

复制 `.env.example` 中的变量到本机运行环境，至少修改：

- `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`
- `JWT_SECRET`：至少32个UTF-8字节，生产环境必须随机生成
- `JWT_EXPIRATION_SECONDS`：默认7200
- `AI_*`：当前阶段仅保留配置契约，默认关闭

不要提交 `.env` 或真实API密钥。

## 启动后端

```powershell
cd backend
mvn.cmd spring-boot:run
```

后端默认地址为 `http://localhost:8080`，Swagger UI 为 `http://localhost:8080/swagger-ui.html`。

## 启动前端

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

前端默认地址为 `http://localhost:5173`，开发代理会将 `/api` 转发到后端。

## 演示账号

- 管理员：`admin / Admin123!`
- 学生：`student / Student123!`

密码在 `data.sql` 中使用 BCrypt 哈希存储。账号仅用于本地演示，生产环境不可使用这些默认凭据。

## 测试

```powershell
cd backend
mvn.cmd test

cd ../frontend
npm.cmd test
npm.cmd run build
```

后端测试使用隔离的H2 MySQL兼容模式，不读取演示数据。MySQL 8脚本仍应在联调前通过本机MySQL执行一次。

## 当前接口

- 认证：注册、登录、当前用户资料、资料修改、摘要
- 资源：分类、已发布资源分页/关键词/分类/城市筛选、详情
- 管理：资源新增、修改、删除和状态变更，仅ADMIN可用

统一返回结构和完整路径见 [接口文档](docs/api.md)。

## AI降级设计

AI路线属于后续P0工作包。契约要求由后端持有密钥、AI输出经过资源白名单与时间校验；关闭AI或调用失败时，后端按城市、兴趣、收藏量和评分生成确定性模板路线。

## 团队分工

- 成员A：后端、数据库、认证、资源、计划与集成
- 成员B：前端、学生端、管理端、可视化与交互测试
- 成员C：AI路线、打卡/成就协作、自动化测试与质量材料

分支与逐日安排见 [第一阶段实施方案](docs/implementation-plan.md)。

## 素材说明

当前骨架未提交来源不明的网络图片，资源无封面时使用分类色块占位。正式图片将在后续上传模块中加入，并在此记录来源、作者与许可。
