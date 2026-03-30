# 开放平台自定义 API（注解模式）

## 概述
新版本自定义 API 已切换为注解驱动模式，不再使用旧版 `IBillWebApiPlugin` 接口扩展方式。

> **适用边界**
> ✅ 本文档直接使用：OpenAPI 控制器无封装层，注解模式直接参考本文档。

- 推荐模式：`@ApiController` + `@ApiMapping` + `@ApiGetMapping/@ApiPostMapping`
- 适用版本：文档基于 Cosmic V8.0.1（更新时间 2026-01）
- 注解包：`kd.bos.openapi.common.custom.annotation`

## 核心基类
该模式不是“继承插件基类”，而是“声明式控制器”。

### 1) 类级注解
- `@ApiController(value = "应用编码", desc = "描述")`
  - `value` 必填：应用编码（如 `bos`、`open`）
- `@ApiMapping("/前缀")`
  - 可选：类级路由前缀

### 2) 方法级注解
- `@ApiGetMapping("/path")`
  - 适合基础类型参数场景
- `@ApiPostMapping("/path")`
  - 适合复杂对象、Map、集合等

### 3) 参数与返回注解
- `@ApiParam`：参数说明/必填/示例
- `@ApiRequestBody`：POST 且单参数 Model 场景（请求体直解）
- `@ApiResponseBody`：返回值说明（标在 `CustomApiResult<T>` 的 `T` 上）
- `@ApiModel`：Model 标识

## 核心事件

注解模式没有传统插件回调接口，核心触发点是路由命中后的方法执行：

- `@ApiGetMapping`：// GET 路由命中后执行方法，适合基础类型参数查询
- `@ApiPostMapping`：// POST 路由命中后执行方法，适合复杂对象/集合/Map 入参
- `@ApiParam`：// 参数描述与必填约束（可配 `required/example`）
- `@ApiRequestBody`：// POST 单参数模型的请求体映射
- `@ApiResponseBody`：// 返回泛型描述，增强 API 文档可读性

## 常用 API/注解写法

```java
@ApiController(value = "open", desc = "用户API")
@ApiMapping("/user")
public class UserController implements java.io.Serializable {

    @ApiGetMapping("/get")
    public CustomApiResult<String> getUserNameById(
        @ApiParam(value = "用户ID", required = true) Long id
    ) {
        // ...
        return CustomApiResult.success("Tom");
    }

    @ApiPostMapping("/save")
    public CustomApiResult<@ApiResponseBody("true-成功，false-失败") Boolean> saveUser(
        @ApiParam(value = "用户数据", required = true) @jakarta.validation.Valid UserModel user
    ) {
        // ...
        return CustomApiResult.success(Boolean.TRUE);
    }
}
```

## 示例代码

示例代码统一维护在模板文件中，直接参考：

- [OpenApiControllerTemplate.java](../../../assets/OpenApiControllerTemplate.java)

## 请求路径规则

自定义 API 请求地址：

`/v2/{ISV}/{appId}/{api_number}`

其中：
- `{api_number}` = 类级 `@ApiMapping` + 方法级 `@ApiPostMapping/@ApiGetMapping`
- 当开发商标识为 `kingdee` 时，`{ISV}` 在某些场景可不显示（以平台实际网关配置为准）

## 实践建议

1. 类级统一加 `@ApiMapping`，方法只写尾路径，便于维护。
2. 参数必填优先用 `@ApiParam(required = true)` + JSR 校验注解（`@NotNull`、`@Min`）。
3. 复杂入参优先 Model（`@ApiModel`），避免散装 `Map` 带来的类型风险。
4. 返回值统一 `CustomApiResult<T>`，并补 `@ApiResponseBody` 说明。
5. GET 仅用于基础类型参数查询；复杂对象统一 POST。

## 常见坑位

- 继续按旧版 `IBillWebApiPlugin` 写法开发，导致新版本无法按预期发布/治理。
- 同一方法同时使用 `@ApiParam` 与 `@ApiRequestBody` 造成入参解析歧义。
- 忘记 `implements Serializable`（控制器/模型）导致序列化与工具链问题。
- Model 字段未标 `@ApiParam`，文档与参数解析表现不完整。
- 返回非 `CustomApiResult<T>`，导致平台响应规范与错误码治理失效。

## 与旧版模式对照

- 旧版：`IBillWebApiPlugin#doCustomService(...)` 接口回调
- 新版：`@ApiController` 注解声明式控制器

结论：新项目与迁移改造均应优先采用注解模式。