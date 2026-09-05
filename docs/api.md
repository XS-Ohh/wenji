# 第一阶段API

所有接口前缀为 `/api`，时间戳采用UTC ISO-8601格式。

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": "2026-08-30T06:00:00Z"
}
```

错误码：`40000`参数错误、`40100`未登录、`40101`凭据错误、`40300`无权限、`40400`不存在、`40901`用户名重复、`40900`数据冲突、`50000`内部错误。

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/auth/register` | 公开 | 注册STUDENT并返回JWT |
| POST | `/auth/login` | 公开 | 登录并返回JWT |
| GET | `/users/me` | 登录 | 当前用户资料 |
| PUT | `/users/me` | 登录 | 修改昵称和头像URL |
| GET | `/users/me/summary` | 登录 | 当前积分等级；其余聚合在后续模块接入 |
| GET | `/categories` | 公开 | 可用文化分类 |
| GET | `/resources` | 公开 | 已发布资源分页筛选 |
| GET | `/resources/{id}` | 公开 | 已发布资源详情并原子增加浏览数 |
| GET | `/resources/map` | 公开 | 地图轻量资源列表，支持keyword/categoryId/city筛选 |
| POST | `/resources/{id}/favorite` | 登录 | 收藏资源，重复请求保持幂等 |
| DELETE | `/resources/{id}/favorite` | 登录 | 取消收藏，重复请求保持幂等 |
| GET | `/users/me/favorites` | 登录 | 当前用户收藏列表 |
| POST | `/plans` | 登录 | 创建研学计划 |
| GET | `/plans` | 登录 | 当前用户计划列表 |
| GET | `/plans/{id}` | 登录 | 计划详情与路线节点 |
| PUT | `/plans/{id}` | 登录 | 修改计划基本信息 |
| DELETE | `/plans/{id}` | 登录 | 删除计划 |
| PATCH | `/plans/{id}/status` | 登录 | 修改计划状态 |
| POST | `/plans/{id}/items` | 登录 | 添加路线节点 |
| PUT | `/plans/{id}/items/{itemId}` | 登录 | 修改路线节点 |
| DELETE | `/plans/{id}/items/{itemId}` | 登录 | 删除路线节点 |
| PUT | `/plans/{id}/items/reorder` | 登录 | 提交完整节点ID顺序 |
| POST | `/plans/{id}/ai-route` | 登录 | 生成路线并替换当前节点，失败时规则降级 |
| POST | `/uploads/images` | STUDENT | 上传JPG/PNG/WebP图片，最大5MB |
| GET | `/uploads/images/{filename}` | 公开 | 读取已上传图片 |
| POST | `/checkins` | STUDENT | 提交资源打卡，可选关联计划和图片 |
| GET | `/checkins` | STUDENT | 当前学生打卡列表 |
| POST | `/admin/resources` | ADMIN | 新增文化资源 |
| PUT | `/admin/resources/{id}` | ADMIN | 修改文化资源 |
| DELETE | `/admin/resources/{id}` | ADMIN | 删除未被业务数据引用的资源 |
| PATCH | `/admin/resources/{id}/status` | ADMIN | DRAFT/PUBLISHED/OFFLINE状态变更 |
| GET | `/admin/checkins` | ADMIN | 审核列表，可按status筛选 |
| PATCH | `/admin/checkins/{id}/review` | ADMIN | 通过或驳回打卡 |

分页参数：`page`默认1，`pageSize`默认12且最大100，另支持 `keyword`、`categoryId`、`city`。受保护接口使用 `Authorization: Bearer <token>`。

智能路线请求示例：

```json
{
  "desiredPlaces": 4,
  "dailyStartTime": "09:00:00",
  "dailyEndTime": "17:00:00"
}
```

响应包含 `title`、`summary`、`estimatedBudget`、`days`、`tips`、`fallback` 和已更新的 `plan`。`fallback` 为 `true` 表示当前结果来自后端规则推荐；生成操作会替换计划原有路线节点。

打卡请求示例：

```json
{
  "planId": 1,
  "resourceId": 3,
  "imageUrl": "/api/uploads/images/生成的文件名.png",
  "content": "通过展陈理解了上海自然生态的演变。"
}
```

`planId` 和 `imageUrl` 可省略，但图片与文字至少提供一项；关联计划时，文化资源必须属于该计划节点。同一用户不可重复提交同一文化地点，被驳回的记录可修改后重新提交。

审核请求示例：

```json
{
  "status": "APPROVED",
  "auditComment": "内容真实完整"
}
```

审核状态仅接受 `APPROVED` 或 `REJECTED`，驳回时审核意见必填。重复提交相同审核结果保持幂等，不会重复发放积分。
