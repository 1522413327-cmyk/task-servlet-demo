# 极简任务系统 · API 接口手册

> 适用版本：v1 ｜ 最后更新：2026-11-15 ｜ 维护者：MJ 软件工程课程组

本文档面向**接口调用方**（前端、测试、第三方系统），说明每个接口的地址、入参、出参、错误码与调用示例。

接口描述本身由代码注解生成，与实际实现同源，不会有"文档写了但代码没实现"的情况：

| 用途 | 地址 | 说明 |
|---|---|---|
| 交互式文档（推荐） | `http://localhost:8088/swagger-ui/index.html` | 可直接在页面上填参数、发请求 |
| OpenAPI 3 描述文件 | `http://localhost:8088/v3/api-docs` | JSON 格式，可导入 Postman / Apifox / YApi |
| 本文档 | `docs/API.md` | 讲设计取舍、错误码语义、调用示例 |

端口随 profile 变化：dev 为 8088、test 为 8089、prod 为 8080。

---

## 1. 工具选型：为什么是 springdoc 而不是 Knife4j

课程资料里同时提到了 OpenAPI / Knife4j / 代码注解三条路。本项目最终**只用 springdoc-openapi 2.8.9 + Swagger UI**，Knife4j 试过三种接法都不通，记录如下，避免后来者重复踩：

| 接法 | 结果 |
|---|---|
| Knife4j starter 4.5.0 + springdoc 2.8.x（排除传递依赖） | 启动正常，访问 `/v3/api-docs` 抛 `NoSuchMethodError: SpringDocConfigProperties.getGroupConfigs()` —— starter 是照 springdoc 2.3.0 编译的，二进制不兼容 |
| Knife4j starter 4.5.0 + 它自带的 springdoc 2.3.0 | Boot 3.4 启动失败：Spring Framework 6.2 删除了 `ControllerAdviceBean(Object)` 构造器，springdoc 2.3.0 还在调用 |
| 只引 `knife4j-openapi3-ui` 静态资源包 | `/doc.html` 能打开、框架能渲染，但接口列表一直加载不出来（缺少 starter 注入的配置） |

结论：**文档界面不是重点，接口描述才是**。springdoc 一条依赖即可同时产出 OpenAPI JSON 和可交互界面，且与 Boot 3.4 完全兼容。

---

## 2. 基础约定

### 2.1 Base URL

```
http://localhost:8088/api/v1
```

`v1` 是版本号。接口一旦对外发布，形状就不能随便改；将来需要不兼容变更时新开 `/api/v2`，`v1` 继续服务一段时间。

### 2.2 认证

接口与页面**共用同一套会话登录态**（Cookie：`JSESSIONID`），由 `LoginInterceptor` 统一拦截 `/api/**`。

- 未登录调用接口：HTTP **401** + JSON 错误体（不是 302 跳登录页，否则前端拿到的是 HTML）
- 登录入口：`POST /login`（表单字段 `username`、`password`），演示账号 `admin / 123456`
- 跨域预检 `OPTIONS` 请求不会携带 Cookie，拦截器已放行

调用示例（先登录拿 Cookie，再带 Cookie 调接口）：

```bash
# 1) 登录，把会话写入 cookie.txt
curl -c cookie.txt -X POST http://localhost:8088/login \
     -d "username=admin&password=123456"

# 2) 带 Cookie 调接口
curl -b cookie.txt http://localhost:8088/api/v1/tasks
```

### 2.3 请求格式

| 项 | 约定 |
|---|---|
| 方法 | `GET` 查询、`POST` 新增、`PUT` 全量更新、`PATCH` 局部更新、`DELETE` 删除 |
| 请求体 | `Content-Type: application/json`，UTF-8 |
| 日期 | 一律 `yyyy-MM-dd` 字符串，例：`2026-11-30` |
| 时间 | 出参 `createDate` 为 `yyyy-MM-dd HH:mm:ss` |

### 2.4 统一响应结构

无论成功还是失败，响应体结构完全一致：

```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "timestamp": "2026-11-15 21:30:00"
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | int | 业务码，**0 表示成功**；非 0 见第 3 节错误码表 |
| `message` | string | 可直接展示给用户的提示；成功时固定 `ok` |
| `data` | object / array / null | 业务数据；失败时为 `null`（字段保留，不省略） |
| `timestamp` | string | 服务端响应时间，排查"前端说没收到"时用于对齐时间线 |

**判断成功的唯一标准是 `code == 0`**，不要用 `data != null` 或 HTTP 200 判断：`DELETE` 成功时 `data` 就是 `null`。

### 2.5 分页约定

| 参数 | 位置 | 默认 | 说明 |
|---|---|---|---|
| `pageNo` | query | 1 | 页码，从 1 开始；小于 1 按 1 处理；超过总页数自动回到最后一页 |
| `pageSize` | query | 随 profile | 每页条数；dev=5 / test=3 / prod=10；超过上限（100）时回落为默认值 |

分页出参：

```json
{
  "list": [],
  "pageNo": 1,
  "pageSize": 5,
  "totalCount": 30,
  "totalPages": 6
}
```

---

## 3. 错误码表

错误码唯一来源：`cn.edu.mju.band.common.ErrorCode`。**前 3 位与 HTTP 状态码对应**，后 2 位是该类错误的序号。

| code | HTTP | message | 触发场景 | 调用方应如何处理 |
|---|---|---|---|---|
| `0` | 200 / 201 | `ok` | 成功 | 取 `data` |
| `40001` | 400 | 请求参数不合法 | 字段缺失、超长、枚举越界、类型转换失败、请求体不是合法 JSON、Content-Type 不对 | 按 `message` 提示用户改输入，重试无效 |
| `40002` | 400 | 日期格式错误 | 日期过了正则但不是真实日期，如 `2026-02-30` | 同上，检查日期 |
| `40101` | 401 | 未登录或会话已过期 | 未登录 / 会话超时 | 跳转登录页，重新登录后再试 |
| `40401` | 404 | 任务不存在 | 按 id 查询/更新/删除时目标不存在 | 提示"记录已被删除"并刷新列表 |
| `50001` | 500 | 系统繁忙，请稍后重试 | 未预期的服务端异常（堆栈只进日志，不返回客户端） | 可重试一次；持续失败联系维护者 |
| `50002` | 503 | 数据访问失败 | 数据库不可用、连接池耗尽、SQL 执行失败 | 稍后重试；持续失败说明数据库故障 |
| `50003` | 500 | 数据写入失败 | 写操作影响行数为 0（记录在检查后被并发删除等） | 重新查询最新状态再操作 |

> **没有 40501**：请求方法不支持（如对 `/api/v1/tasks` 发 PUT）时，异常在请求匹配到 Controller 之前就已抛出，
> `@RestControllerAdvice` 拿不到 handler 类型，无法接管，返回的是 Spring MVC 默认的 405 响应体。
> 这类框架层错误不纳入本项目的错误码体系 —— 与其再造一个永远不会生效的错误码，不如在文档里说清楚边界。

错误响应示例：

```json
{
  "code": 40401,
  "message": "id=999 的任务不存在",
  "data": null,
  "timestamp": "2026-11-15 21:30:00"
}
```

> `message` 是给人看的、可能变化；`code` 是给程序判断的、不会变。分支判断请永远用 `code`。

---

## 4. 字段字典

### 4.1 状态 `status`

| 值 | 含义 | 界面文案 |
|---|---|---|
| `0` | 待办 | 待办 |
| `1` | 进行中 | 进行中 |
| `2` | 已完成 | 已完成 |

### 4.2 优先级 `priority`

| 值 | 含义 | 界面文案 |
|---|---|---|
| `1` | 低 | 低 |
| `2` | 中 | 中 |
| `3` | 高 | 高 |

### 4.3 任务对象 `TaskVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | int | 任务 ID，数据库自增 |
| `title` | string | 标题，1~50 字符 |
| `status` | int | 状态码，见 4.1 |
| `statusText` | string | 状态中文名，**只读**，由服务端翻译，客户端不要反推 |
| `priority` | int | 优先级码，见 4.2 |
| `priorityText` | string | 优先级中文名，只读 |
| `dueDate` | string | 截止日期 `yyyy-MM-dd`；未设置时为**空字符串**（不是 null） |
| `createDate` | string | 创建时间 `yyyy-MM-dd HH:mm:ss`，只读，由数据库生成 |

`statusText` / `priorityText` 存在的意义：中文映射只维护一份（`TaskFormatter`）。若让前端各自 if-else，改文案时就会漏改。

---

## 5. 接口详情

### 5.1 分页查询任务

```
GET /api/v1/tasks
```

**查询参数**

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `title` | string | 否 | — | 标题模糊匹配，空白视为不限 |
| `status` | int | 否 | — | 状态精确过滤，0/1/2 |
| `pageNo` | int | 否 | 1 | 页码 |
| `pageSize` | int | 否 | 随 profile | 每页条数 1~100 |

**请求示例**

```bash
curl -b cookie.txt "http://localhost:8088/api/v1/tasks?title=报告&status=0&pageNo=1&pageSize=5"
```

**响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "list": [
      {
        "id": 30,
        "title": "提交最终版代码与设计报告",
        "status": 0,
        "statusText": "待办",
        "priority": 3,
        "priorityText": "高",
        "dueDate": "2026-11-30",
        "createDate": "2026-11-13 15:30:00"
      }
    ],
    "pageNo": 1,
    "pageSize": 5,
    "totalCount": 1,
    "totalPages": 1
  },
  "timestamp": "2026-11-15 21:30:00"
}
```

**可能的错误**：`40001`（`pageNo=abc` 等类型错误）、`40101`（未登录）。

---

### 5.2 按 ID 查询任务

```
GET /api/v1/tasks/{id}
```

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | int | 是 | 任务 ID |

**响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 7,
    "title": "为业务层编写 Mockito 单元测试",
    "status": 2,
    "statusText": "已完成",
    "priority": 3,
    "priorityText": "高",
    "dueDate": "2026-11-02",
    "createDate": "2026-10-25 15:20:00"
  },
  "timestamp": "2026-11-15 21:30:00"
}
```

**可能的错误**：`40401`（不存在）、`40001`（id 不是数字）。

---

### 5.3 新增任务

```
POST /api/v1/tasks
```

**请求体**

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| `title` | string | 是 | 1~50 字符，首尾空格会被去除 | 任务标题 |
| `priority` | int | 是 | 1 / 2 / 3 | 优先级 |
| `status` | int | 是 | 0 / 1 / 2 | 状态 |
| `dueDate` | string | 否 | `yyyy-MM-dd` 或空 | 截止日期 |

> 请求体里**不要**传 `id` 和 `createDate`：前者由数据库生成，后者由数据库 `DEFAULT CURRENT_TIMESTAMP` 生成。传了也会被忽略（入参对象里没有这两个字段），这是白名单式设计。

**请求示例**

```bash
curl -b cookie.txt -X POST http://localhost:8088/api/v1/tasks \
     -H "Content-Type: application/json" \
     -d '{"title":"录制三分钟演示视频","priority":3,"status":0,"dueDate":"2026-11-28"}'
```

**响应**：HTTP **201 Created**，响应头 `Location: /api/v1/tasks/31`

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 31,
    "title": "录制三分钟演示视频",
    "status": 0,
    "statusText": "待办",
    "priority": 3,
    "priorityText": "高",
    "dueDate": "2026-11-28",
    "createDate": "2026-11-15 21:30:00"
  },
  "timestamp": "2026-11-15 21:30:00"
}
```

**可能的错误**

| code | 场景 | message 示例 |
|---|---|---|
| `40001` | 标题为空 | `title: 任务标题不能为空` |
| `40001` | 优先级越界 | `priority: 优先级只能是 1(低)、2(中)、3(高)` |
| `40002` | 日期不存在 | `截止日期不是真实存在的日期: 2026-02-30` |
| `50003` | 写库失败 | `数据写入失败` |

---

### 5.4 全量更新任务

```
PUT /api/v1/tasks/{id}
```

**语义**：全量更新。除 `dueDate` 外所有字段必填；`dueDate` 传 `null` 或空串表示**清空**截止日期。

> 只改状态请用 5.5 的 PATCH 接口。用 PUT 改状态时若漏传 `dueDate`，日期会被清空 —— 这是"全量替换"的正常语义，不是 bug。

**请求体**：同 5.3。

**响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 7,
    "title": "为业务层编写 Mockito 单元测试（已补充分支用例）",
    "status": 1,
    "statusText": "进行中",
    "priority": 2,
    "priorityText": "中",
    "dueDate": "2026-11-20",
    "createDate": "2026-10-25 15:20:00"
  },
  "timestamp": "2026-11-15 21:30:00"
}
```

**可能的错误**：`40001`、`40002`、`40401`、`50003`。

---

### 5.5 更新任务状态

```
PATCH /api/v1/tasks/{id}/status
```

只修改 `status` 一个字段，用于看板拖拽、待办勾选这类场景。相比 PUT，它不会误改其他字段，也不需要传全量数据。

**请求体**

| 字段 | 类型 | 必填 | 约束 |
|---|---|---|---|
| `status` | int | 是 | 0 / 1 / 2 |

```bash
curl -b cookie.txt -X PATCH http://localhost:8088/api/v1/tasks/7/status \
     -H "Content-Type: application/json" -d '{"status":2}'
```

**响应**：`code=0`，`data` 为更新后的任务对象（`statusText` 同步变为"已完成"）。

**可能的错误**：`40001`（status 越界）、`40401`（不存在）。

---

### 5.6 删除任务

```
DELETE /api/v1/tasks/{id}
```

**语义**：物理删除。成功时 `data` 为 `null`。

```bash
curl -b cookie.txt -X DELETE http://localhost:8088/api/v1/tasks/31
```

```json
{
  "code": 0,
  "message": "ok",
  "data": null,
  "timestamp": "2026-11-15 21:30:00"
}
```

**可能的错误**：`40401`（不存在，包括重复删除）。

---

## 6. 调试技巧

**① 用 Swagger UI 直接发请求**
打开 `http://localhost:8088/swagger-ui/index.html`，先在同一浏览器登录 `/login`，再回到文档页点 `Try it out` —— 同源请求会自动带上 `JSESSIONID`。

**② 未登录时接口返回什么**

```bash
curl -i http://localhost:8088/api/v1/tasks
```

```
HTTP/1.1 401
Content-Type: application/json;charset=UTF-8
Content-Length: 115

{"code":40101,"message":"未登录或会话已过期，请先登录","data":null,"timestamp":"2026-11-15 21:30:00"}
```

**③ 数据库挂掉时接口返回什么**

`GET` 详情类接口返回 `503 / 50002`；列表接口按业务层既有策略**降级为空列表**（见 `TaskBuss#search` 的 try-catch，页面场景是为了不白屏，接口场景是否该降级已列入代码评审议题）。

---

## 7. 变更记录

| 版本 | 日期 | 变更 |
|---|---|---|
| v1 | 2026-11-15 | 首次发布：6 个接口（列表 / 详情 / 新增 / 全量更新 / 状态流转 / 删除），统一响应体与 8 个错误码 |
