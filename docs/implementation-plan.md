# 文迹第一阶段实施方案

## 1. 最终目录结构

```text
Travel/
├─ backend/
│  ├─ src/main/java/com/wenji/
│  │  ├─ auth/             # 注册、登录、JWT过滤器与认证DTO
│  │  ├─ common/           # 统一响应、分页结构、错误码与全局异常
│  │  ├─ config/           # Security、MyBatis-Plus、OpenAPI配置
│  │  ├─ resource/         # 分类、文化资源及管理员资源管理
│  │  ├─ user/             # 当前用户资料与用户领域模型
│  │  ├─ favorite/         # 收藏（P0后续）
│  │  ├─ plan/             # 计划与路线节点（P0后续）
│  │  ├─ route/            # AI适配器、校验与降级路线（P0后续）
│  │  ├─ checkin/          # 打卡、上传与审核（P0后续）
│  │  ├─ note/             # 研学笔记（P0后续）
│  │  ├─ achievement/      # 积分与成就（P0后续）
│  │  └─ dashboard/        # 个人/管理统计（P0后续）
│  ├─ src/main/resources/
│  │  ├─ application.yml
│  │  ├─ schema.sql
│  │  └─ data.sql
│  ├─ src/test/
│  └─ pom.xml
├─ frontend/
│  ├─ src/{api,assets,components,layouts,router,stores,types,utils,views}/
│  ├─ tests/
│  └─ package.json
├─ docs/
│  ├─ implementation-plan.md
│  ├─ api.md
│  ├─ test-plan.md
│  ├─ test-report.md
│  ├─ ai-usage-log.md
│  └─ demo-script.md
├─ uploads/
├─ .env.example
├─ .gitignore
└─ README.md
```

## 2. 数据库ER关系

- `users` 1:N `favorites`，`culture_resources` 1:N `favorites`；两端组成用户与资源的N:M收藏关系，`(user_id, resource_id)`唯一。
- `culture_categories` 1:N `culture_resources`；资源必须属于一个分类。
- `users` 1:N `study_plans`，`study_plans` 1:N `plan_items`，`culture_resources` 1:N `plan_items`；计划节点连接计划与实际文化资源。
- `users` 1:N `checkins`；`study_plans` 1:N `checkins`为可选关系；`culture_resources` 1:N `checkins`；`audited_by`可选关联管理员用户。
- `users` 1:N `study_notes`；笔记可选关联一个计划和/或一个文化资源。
- `users` N:M `achievements`，由 `user_achievements` 承载且联合唯一，保证成就解锁幂等。
- `users` 1:N `ai_route_logs`；日志可选关联 `study_plans`，用于审计AI与降级调用。
- 所有外键使用 `RESTRICT` 防止误删核心业务记录；计划删除时仅对其节点使用 `CASCADE`。计数冗余字段通过事务内原子更新维护。

## 3. P0任务拆分

| 工作包 | 交付物 | 前置依赖 | 验收重点 |
|---|---|---|---|
| P0-01 基础工程 | 前后端骨架、环境配置、响应/异常、OpenAPI | 无 | 两端可独立启动 |
| P0-02 数据与认证 | 11张表、种子数据、注册/登录/JWT/个人资料 | P0-01 | 401/403、重复用户名、错误密码 |
| P0-03 资源发现 | 分类、资源分页/筛选/详情、后台CRUD | P0-02 | 仅公开已发布资源、管理员权限 |
| P0-04 收藏与上传 | 收藏幂等、列表、图片类型与大小校验 | P0-03 | 唯一约束、计数一致性、路径安全 |
| P0-05 研学计划 | 计划CRUD/状态、节点CRUD/排序/时间 | P0-03 | 数据归属、日期与时间冲突校验 |
| P0-06 AI路线 | 适配器、JSON契约校验、一次重试、规则降级 | P0-05 | 无密钥也可演示、资源不虚构 |
| P0-07 打卡闭环 | 打卡提交、管理员审核、积分事务 | P0-04/05 | 重复审核幂等、仅管理员审核 |
| P0-08 笔记与成就 | 笔记CRUD、6项成就、等级重算 | P0-07 | 归属校验、奖励不重复 |
| P0-09 数据大屏 | 个人趋势/分布/活动及管理概览 | P0-07/08 | 仅当前用户已审核数据、空状态 |
| P0-10 质量交付 | 单测/API/E2E、README、报告、演示脚本 | 全部 | 4条E2E与DoD逐项通过 |

## 4. 三名成员并行分支计划

采用 `main <- develop <- feature/*`，短分支每日同步 `develop`，PR由另一成员审查。共享契约先合入 `develop`，避免三人同时修改基础DTO和路由。

| 阶段 | 成员A（后端/集成） | 成员B（前端/可视化） | 成员C（AI/质量） |
|---|---|---|---|
| 第1周 | `feature/auth-resources-api` | `feature/web-auth-resources` | `test/auth-resources-contract` |
| 第2周前半 | `feature/plans-api` | `feature/web-plans` | `feature/ai-route` |
| 第2周后半 | `feature/checkin-review` | `feature/web-checkins-notes` | `feature/achievements` |
| 第3周 | `chore/integration-docs` | `feature/dashboard-ui` | `test/e2e-regression` |

冲突控制：A拥有数据库迁移与后端公共配置，B拥有全局样式与前端路由，C拥有AI契约和测试夹具；跨所有权改动先提交小型契约PR。数据库变更按序号追加，禁止多人改写已合并脚本。

## 5. 第一周逐日开发清单

| 日期 | 共同目标 | A | B | C | 当日出口条件 |
|---|---|---|---|---|---|
| Day 1 | 冻结P0范围与契约 | ER/接口/状态机评审 | 低保真流程与路由表 | 测试矩阵和边界清单 | 方案、字段、响应码无冲突 |
| Day 2 | 工程可运行 | Spring骨架、SQL、公共异常 | Vue骨架、请求层、布局 | CI/测试配置、接口夹具 | 两端构建通过、数据库可初始化 |
| Day 3 | 认证闭环 | 注册/登录/JWT/角色 | 登录/注册/登录态 | 认证接口与权限测试 | 401/403/登录成功场景通过 |
| Day 4 | 资源发现 | 分类、列表、详情、管理CRUD | 列表/筛选/详情 | 分页筛选与越权测试 | 学生可浏览，管理员可维护 |
| Day 5 | 收藏与联调 | 收藏、上传与事务计数 | 收藏页、上传交互 | 联调回归和缺陷复测 | 周末演示链路完整 |

每天结束：合并前运行自己模块测试，更新接口文档、缺陷记录和AI使用记录；`develop`保持可启动。

## 6. 技术风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| 本机 `java` 与 Maven JDK不一致 | IDE/命令结果不一致 | README明确Java 17+；以 `mvn -version` 和 `JAVA_HOME`为准 |
| MySQL脚本与测试库方言差异 | CI测试失败 | MySQL做最终验收；单测使用MySQL兼容模式并限制方言特性 |
| JWT密钥弱或泄漏 | 身份可伪造 | 环境变量注入、最短32字符校验、日志不输出密钥 |
| BCrypt演示账号或初始化顺序错误 | 无法登录演示 | 固定已验证哈希，启动集成测试覆盖两类账号 |
| 收藏数/浏览数并发更新丢失 | 展示计数不准 | SQL原子自增减、唯一约束、事务和幂等测试 |
| AI输出非法或虚构资源 | 路线不可用 | 后端白名单校验、最多重试一次、确定性规则降级 |
| 文件上传穿越/超限/伪装类型 | 安全与磁盘风险 | 白名单、5MB限制、服务端生成文件名、固定上传根目录 |
| 计划与审核重复提交 | 重复积分或状态错乱 | 状态条件更新、唯一约束、事务内成就重算 |
| 三人同时修改共享文件 | 合并冲突与接口漂移 | 明确文件所有权、契约先行、小PR、每日同步 |
| 三周范围过大 | P0延期 | 按工作包出口条件推进；P0全部通过前不进入P1 |

## 一致性结论

方案未发现阻塞性冲突。SQL初始化脚本满足规格默认方案；Spring Boot单体、Vue前后端分离与三周范围一致；权限边界以“公开读、登录写、ADMIN管理”为统一规则。规格未定义资源开放状态字段，第一阶段不新增该筛选字段，避免把资源发布状态误当作营业状态。

