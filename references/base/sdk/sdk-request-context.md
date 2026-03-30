# 请求上下文 (Request Context)

## 概述
`RequestContext` 保存了当前请求线程关联的租户、账套、用户信息及登录环境。它是苍穹平台识别“我是谁”、“我在哪”的核心依据，贯穿了从前端请求到后台服务调用的全生命周期。

## 核心类
- **`kd.bos.context.RequestContext`**: 请求上下文的核心工具类。

## 常用 API 方法
- `RequestContext.get()`: 获取当前线程绑定的上下文对象。
- `getCurrUserId()` / `getUid()`: 获取当前登录用户的 ID。
- `getUserName()`: 获取用户姓名。
- `getTenantId()` / `getAccountId()`: 获取租户 ID 和账套 ID。
- `getOrgId()`: 获取当前用户登录时选中的组织 ID。
- `getLocale()` / `getLang()`: 获取当前语种（如 `zh_CN`）。

## 示例代码
### 1. 基础信息获取
```java
import kd.bos.context.RequestContext;

public void bizMethod() {
    RequestContext ctx = RequestContext.get();
    long userId = ctx.getCurrUserId();
    long orgId = ctx.getOrgId();
    String accountId = ctx.getAccountId();
    // 执行基于当前身份的逻辑...
}
```

### 2. 跨线程手动传递 (不推荐，优先用线程池)
```java
// 复制当前上下文
RequestContext copiedCtx = RequestContext.copy(RequestContext.get());

new Thread(() -> {
    try {
        // 在新线程中绑定上下文
        RequestContext.set(copiedCtx);
        // 执行逻辑...
    } finally {
        // 务必清理，防止干扰后续任务
        RequestContext.remove();
    }
}).start();
```

## 实践建议
1. **优先使用平台线程池**：手动使用 `RequestContext.copy` 极易出错，推荐使用 `ThreadPools.executeOnceIncludeRequestContext`，平台会自动处理上下文克隆。
2. **严禁共用实例**：跨线程传递时**必须调用 `copy()`**，严禁直接 `set(originalCtx)`，否则会导致调用链监控数据混乱。
3. **及时清理**：如果手动调用了 `RequestContext.set()`，务必在 `finally` 块中执行 `remove()`。

## 常见坑位
1. **异步线程失效**：在普通异步线程或 MQ 消费者中，直接调用 `RequestContext.get()` 返回的是空对象或默认对象，导致 `userId` 为 0 或数据库操作无权限。
2. **内存泄露**：频繁在非平台托管线程中 `set` 而不 `remove`，会导致 `ThreadLocal` 变量不断堆积。
3. **调用链污染**：若直接复用原线程的 `RequestContext` 实例，多个并发线程会共享同一个 `traceId`，导致日志追踪无法区分。
