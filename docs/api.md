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
| POST | `/admin/resources` | ADMIN | 新增文化资源 |
| PUT | `/admin/resources/{id}` | ADMIN | 修改文化资源 |
| DELETE | `/admin/resources/{id}` | ADMIN | 删除未被业务数据引用的资源 |
| PATCH | `/admin/resources/{id}/status` | ADMIN | DRAFT/PUBLISHED/OFFLINE状态变更 |

分页参数：`page`默认1，`pageSize`默认12且最大100，另支持 `keyword`、`categoryId`、`city`。受保护接口使用 `Authorization: Bearer <token>`。

