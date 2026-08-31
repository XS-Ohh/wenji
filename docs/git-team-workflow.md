# 文迹团队 Git 协作说明

本文档用于三名成员在同一 GitHub 仓库中并行开发，目标是保留稳定版本、避免互相覆盖代码，并让每一次修改都可以审查和恢复。

## 1. 当前仓库

- GitHub：`https://github.com/XS-Ohh/wenji`
- 稳定分支：`main`
- 集成分支：`develop`
- 首次提交：`eeba445 feat: 完成文迹项目基础功能`

## 2. 分支结构

```text
main                              稳定、可演示版本
└── develop                       团队集成和联调版本
    ├── improve/backend-stability 成员A：后端稳定性
    ├── improve/frontend-ux       成员B：前端体验
    └── improve/quality-pipeline  成员C：测试和交付质量
```

基本规则：

1. 所有成员从最新的 `develop` 创建自己的工作分支。
2. 成员只向自己的分支执行 `push`。
3. 工作分支通过 Pull Request 合并到 `develop`。
4. `develop` 联调和测试通过后，通过 Pull Request 合并到 `main`。
5. 禁止直接向 `main` 推送，禁止对共享分支执行强制推送。

上传自己的工作分支不会修改 `main`。Git 提交和版本标签会保留历史，因此团队后续迭代不会覆盖最初版本。

## 3. 三名成员的优化任务

### 成员A：后端稳定性

分支：`improve/backend-stability`

- 完善计划状态流转规则。
- 优化收藏和计划节点的事务、幂等性。
- 处理外键冲突并返回明确业务错误。
- 优化计划列表查询。
- 完善 Swagger 和后端测试。

### 成员B：前端体验

分支：`improve/frontend-ux`

- 优化导航、资源卡片、计划页面和响应式布局。
- 完善加载、空状态、失败和重复提交状态。
- 优化表单校验和接口错误提示。
- 抽取复用组件并优化构建体积。
- 验证 1366×768 和 1920×1080 页面。

### 成员C：测试和交付质量

分支：`improve/quality-pipeline`

- 添加 Playwright 端到端测试。
- 配置 GitHub Actions 自动测试。
- 更新 README、API 文档和测试报告。
- 整理演示数据、演示脚本和 AI 辅助记录。
- 检查配置文件和敏感信息。

## 4. 队长的首次设置

当前仓库的 `main` 和 `develop` 已经建立。新仓库才需要执行下面的初始化命令：

```powershell
git init
git branch -M main
git add .
git commit -m "feat: 完成文迹项目基础功能"
git remote add origin https://github.com/XS-Ohh/wenji.git
git push -u origin main
git switch -c develop
git push -u origin develop
```

建议为稳定版本创建标签：

```powershell
git switch main
git pull --ff-only origin main
git tag -a v0.1.0 -m "首个可运行版本"
git push origin v0.1.0
git switch develop
```

标签 `v0.1.0` 会永久指向当时的提交，即使项目继续开发，也可以随时查看或恢复该版本。

## 5. 邀请团队成员

仓库负责人进入 GitHub：

```text
仓库 → Settings → Collaborators → Add people
```

输入成员 GitHub 用户名并邀请。成员接受邀请后才可以向仓库推送分支。

## 6. 成员第一次下载项目

```powershell
git clone https://github.com/XS-Ohh/wenji.git
cd wenji
git switch develop
git pull --ff-only origin develop
```

检查当前状态：

```powershell
git status
git branch
git remote -v
```

正常情况下，当前分支应为 `develop`，工作区应显示 `working tree clean`。

## 7. 创建个人工作分支

成员每次开始新任务前，都要从最新 `develop` 创建分支。

成员A：

```powershell
git switch develop
git pull --ff-only origin develop
git switch -c improve/backend-stability
git push -u origin improve/backend-stability
```

成员B：

```powershell
git switch develop
git pull --ff-only origin develop
git switch -c improve/frontend-ux
git push -u origin improve/frontend-ux
```

成员C：

```powershell
git switch develop
git pull --ff-only origin develop
git switch -c improve/quality-pipeline
git push -u origin improve/quality-pipeline
```

第一次推送需要 `-u` 建立跟踪关系，以后在该分支只需执行 `git push`。

## 8. 日常开发和提交

开始工作时确认分支：

```powershell
git branch --show-current
git status
```

完成一个小目标后提交：

```powershell
git status
git add .
git commit -m "improve: 优化前端响应式布局"
git push
```

推荐提交格式：

```text
feat: 新增功能
fix: 修复缺陷
improve: 优化现有实现
refactor: 重构但不改变行为
test: 添加或修改测试
docs: 更新文档
chore: 工具和配置调整
```

一次提交只处理一个清晰目标。提交前需要检查 `git status`，避免把无关文件一起提交。

## 9. 同步其他成员的修改

其他成员的 Pull Request 合并到 `develop` 后，当前成员应把最新 `develop` 合并进自己的工作分支。

```powershell
git status
git switch develop
git pull --ff-only origin develop
git switch improve/frontend-ux
git merge develop
```

最后一行需要替换成自己的工作分支名称。

没有冲突时继续：

```powershell
git push
```

建议每天开始开发前同步一次，在创建 Pull Request 前再同步一次。

## 10. 解决代码冲突

当两名成员修改同一文件的相同位置时，`git merge develop` 可能报告冲突。

先查看冲突：

```powershell
git status
```

在 IDEA 中打开冲突文件，选择：

```text
Git → Resolve Conflicts
```

IDEA 通常会显示：

- 左侧：当前工作分支代码。
- 右侧：准备合并的 `develop` 代码。
- 中间：最终保留结果。

解决后执行：

```powershell
git add .
git commit -m "merge: 合并develop并解决冲突"
git push
```

冲突处理原则：先理解双方代码的业务目的，不要直接选择“全部接受左侧”或“全部接受右侧”。涉及不熟悉的模块时，应联系对应成员共同确认。

## 11. 创建 Pull Request

成员将工作分支上传后，进入 GitHub：

```text
Pull requests → New pull request
```

例如成员B应选择：

```text
base: develop
compare: improve/frontend-ux
```

Pull Request 描述至少包括：

```markdown
## 改动内容
- 优化资源卡片响应式布局
- 完善加载和错误状态

## 验证
- npm.cmd test
- npm.cmd run build

## 注意事项
- 未修改后端接口
```

合并前检查：

1. 修改范围是否符合任务。
2. 是否误提交 `.env`、构建产物或个人配置。
3. 后端或前端测试是否通过。
4. 是否存在未解决的冲突。
5. 页面或接口是否经过人工验证。

确认后将工作分支合并到 `develop`，不要直接合并到 `main`。

## 12. 发布稳定版本

三名成员的工作都合并到 `develop` 后，在 `develop` 运行：

```powershell
cd backend
mvn.cmd test

cd ../frontend
npm.cmd test
npm.cmd run build
```

联调和测试通过后，在 GitHub 创建：

```text
develop → main
```

合并后创建版本标签：

```powershell
git switch main
git pull --ff-only origin main
git tag -a v0.2.0 -m "完成第一轮团队优化"
git push origin v0.2.0
git switch develop
git merge main
git push
```

此时：

```text
v0.1.0 = 最初可运行版本
v0.2.0 = 第一轮团队优化版本
```

## 13. GitHub 分支保护

仓库负责人进入：

```text
Settings → Rules → Rulesets
```

为 `main` 配置：

- 必须通过 Pull Request 合并。
- 禁止强制推送。
- 禁止删除分支。
- 至少一名成员审核。
- 配置 CI 后要求自动测试通过。

建议 `develop` 也禁止强制推送，并要求通过 Pull Request 合并。

## 14. 错误修改和版本恢复

### 尚未提交的文件修改

先在 IDEA 中查看差异。确认确实不要后，可以恢复单个文件：

```powershell
git restore 路径/文件名
```

此操作会丢弃该文件尚未提交的修改，执行前必须确认。

### 已提交但尚未推送

不要直接删除历史。可以增加一个反向提交：

```powershell
git revert 提交ID
```

### 已合并的错误 Pull Request

优先在 GitHub Pull Request 页面点击 `Revert`，生成一个新的撤销 Pull Request。不要对共享分支使用 `reset --hard` 或强制推送。

### 查看历史版本

```powershell
git log --oneline --graph --decorate --all
git tag
git show v0.1.0
```

## 15. 严禁操作

团队成员不要执行：

```powershell
git push --force
git push origin main
git reset --hard
```

除非团队负责人明确确认，不要：

- 删除 `main` 或 `develop`。
- 修改已发布版本标签。
- 把真实密码、Token、AI 密钥提交到仓库。
- 将本地数据库文件、`node_modules`、`target` 或 `dist` 提交到仓库。

## 16. 常见问题

### GitHub 连接被重置

```powershell
git config --global http.version HTTP/1.1
git push
```

### 当前分支没有上游分支

```powershell
git push -u origin 当前分支名称
```

### push 被拒绝，提示 non-fast-forward

不要强制推送，先同步目标分支：

```powershell
git fetch origin
git switch develop
git pull --ff-only origin develop
git switch 自己的工作分支
git merge develop
git push
```

### 出现 LF/CRLF 提示

这是 Windows 换行符提示，不代表提交失败。团队不要频繁修改 Git 换行配置，避免产生整文件差异。

## 17. 每日检查清单

开始开发：

```text
[ ] 当前位于自己的工作分支
[ ] 已同步最新 develop
[ ] 工作目录没有来源不明的修改
```

结束开发：

```text
[ ] 已运行相关测试
[ ] 已检查 git status
[ ] 提交信息清晰
[ ] 已 push 自己的工作分支
[ ] 需要集成时已创建 Pull Request
```

## 18. 常用命令速查

```powershell
# 查看当前分支
git branch --show-current

# 查看文件状态
git status

# 查看提交历史
git log --oneline --graph --decorate --all

# 同步 develop
git switch develop
git pull --ff-only origin develop

# 创建工作分支
git switch -c improve/task-name

# 首次上传工作分支
git push -u origin improve/task-name

# 提交修改
git add .
git commit -m "improve: 描述本次优化"
git push

# 将最新 develop 合并到工作分支
git switch improve/task-name
git merge develop
```
