# 文迹

文迹是面向大学生的文化研学与城市探索系统。当前已实现注册登录/JWT鉴权、个人资料、文化资源与交互地图、收藏、研学计划与路线节点、带规则降级的智能路线生成，以及图片打卡和管理员审核闭环。

## 技术栈

- 前端：Vue 3、TypeScript、Vite、Vue Router、Pinia、Element Plus、Axios、Vitest
- 后端：Java 21、Spring Boot 3.5、Spring Security、JWT、MyBatis-Plus、MySQL 8、Springdoc OpenAPI、JUnit 5
- 架构：Vue单页应用 + Spring Boot单体REST API + MySQL

## 目录

- `backend/`：后端应用、SQL和集成测试
- `frontend/`：前端应用和单元测试
- `docs/`：实施方案、接口、测试、AI使用与演示文档
- `uploads/`：打卡图片存储目录，仅跟踪空目录

完整规划见 [第一阶段实施方案](docs/implementation-plan.md)。

三名成员的分支创建、日常提交、Pull Request、冲突处理和版本恢复流程见
[团队 Git 协作说明](docs/git-team-workflow.md)。

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
- `UPLOAD_DIR`：打卡图片目录；从 `backend/` 启动时默认 `../uploads`
- `VITE_MAP_TILE_URL`：前端地图瓦片模板，默认使用OpenStreetMap开发服务
- `VITE_MAP_ATTRIBUTION`：地图右下角的瓦片来源与版权署名
- `AI_ENABLED`：是否启用模型调用，默认 `false`
- `AI_BASE_URL`：OpenAI兼容接口的基础地址，例如 `https://api.openai.com/v1`
- `AI_API_KEY`、`AI_MODEL`：服务密钥与模型名称
- `AI_TIMEOUT_SECONDS`：连接与读取超时，默认30秒

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
- 文化地图：全部已发布资源的轻量地图数据、聚合标记、筛选联动和详情跳转
- 收藏：收藏、取消收藏和个人收藏列表
- 计划：计划增删改查、状态、路线节点维护与排序
- 智能路线：模型生成、白名单与时间校验、一次重试和规则降级
- 打卡：图片上传、文字记录、关联计划、个人列表和驳回后重新提交
- 管理：资源维护、打卡审核，仅ADMIN可用；通过审核后发放10积分并更新计划进度

统一返回结构和完整路径见 [接口文档](docs/api.md)。

## AI降级设计

AI密钥仅由后端读取。模型输出经过已发布资源白名单、计划日期、每日时间、时间重叠、预算和重复资源校验；JSON解析或校验失败会重试一次。AI关闭、配置不完整或重试失败时，后端按城市、兴趣、收藏量和评分生成3至4个地点的规则路线，并在响应中返回 `fallback: true`。

## 打卡审核设计

仅STUDENT可发起和查看自己的打卡；ADMIN不显示学生打卡入口，只使用审核中心。打卡可关联计划节点，也可直接关联已发布文化资源。图片限制为JPG、PNG或WebP且最大5MB，后端校验文件签名并生成随机文件名。同一用户在同一文化地点只保留一条记录；待审核或已通过时禁止重复提交，被驳回后可修改原记录重新提交。管理员审核使用状态条件更新保证幂等，通过后在同一事务中增加10积分、完成对应计划节点并重算计划进度。

已有本地数据库若不是用最新 `schema.sql` 重建，请先确认无重复打卡数据，再执行一次 [打卡唯一约束迁移](docs/sql/20260831-checkin-unique.sql)。

## 文化地图

公开页面 `/map` 使用Leaflet展示资源经纬度，支持关键词、分类和城市筛选；地图标记按分类着色，密集地点自动聚合，点击左侧资源或地图标记可打开安全构建的资源弹窗并进入详情页。缺少经纬度的数据仍显示在结果列表中，但不会产生错误标记。

默认OpenStreetMap瓦片只适合本地开发和低流量演示，必须保留可见署名，且不可批量预取。正式部署应通过 `VITE_MAP_TILE_URL` 与 `VITE_MAP_ATTRIBUTION` 切换到有服务保障且许可匹配的瓦片供应商。

## 团队分工

- 成员A：后端、数据库、认证、资源、计划与集成
- 成员B：前端、学生端、管理端、可视化与交互测试
- 成员C：AI路线、打卡/成就协作、自动化测试与质量材料

分支与逐日安排见 [第一阶段实施方案](docs/implementation-plan.md)。

## 素材说明

当前骨架未提交来源不明的网络图片，资源无封面时使用分类色块占位。正式图片将在后续上传模块中加入，并在此记录来源、作者与许可。
