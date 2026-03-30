# 请求上下文处理 (RequestContextUtils)

## 概述
在苍穹多线程异步开发中，`RequestContext`（包含用户、租户、多语言等）由于依赖 `ThreadLocal` 无法自动传递。`RequestContextUtils` (位于 `kd.cd.common.concurrent`) 提供了上下文的自动恢复功能，确保异步任务能正确继承主线程的身份与权限。本规范适用于后台任务、集成接口与身份模拟场景。

> **适用边界**
> ✅ 适用：异步线程/后台任务/集成接口中需要恢复用户上下文。
> ❌ 不适用：同步插件事件中上下文自动可用，无需额外处理。

## 核心类
- **`kd.cd.common.concurrent.RequestContextUtils`**: **核心工具类**。
- **`kd.cd.common.concurrent.RequestContextUtils.Guard`**: 上下文守卫接口，支持 try-with-resources。
- **`kd.bos.context.RequestContext`**: 苍穹核心请求上下文接口。

## 常用 API 方法

### 1. 自动恢复模式（推荐）
- `switchUser(long userId)`: **最常用**。切换到指定用户身份，并在 try-with-resources 结束时自动恢复。
- `switchContext(Map<String, Object> params)`: 根据参数切换上下文，并在 try-with-resources 结束时自动恢复。

### 2. Guard 接口
`Guard` 是一个函数式接口，实现了 `AutoCloseable`，配合 try-with-resources 使用，确保离开作用域时原始上下文被 100% 恢复。

## 示例代码

### 自动恢复模式（推荐）
```java
package kd.cd.common.demo;

import kd.cd.common.concurrent.RequestContextUtils;
import kd.bos.context.RequestContext;

public class AsyncDemo {
    public void executeWithUser(long targetUserId) {
        // 使用 try-with-resources 自动恢复
        try (RequestContextUtils.Guard ignored = RequestContextUtils.switchUser(targetUserId)) {
            // 此作用域内以 targetUser 身份执行
            // ...执行业务逻辑...
        }
        // 离开作用域后自动恢复原始上下文，确保线程安全
    }
}
```

### 自定义上下文参数
```java
public void executeWithCustomContext() {
    Map<String, Object> params = new HashMap<>();
    params.put("userId", "12345");
    params.put("userName", "test");

    try (RequestContextUtils.Guard ignored = RequestContextUtils.switchContext(params)) {
        // 以切换后的上下文执行业务逻辑
    }
    // 自动恢复原始上下文
}
```

## 实践建议
1. **强制使用 Guard 模式**: 使用 `switchUser`/`switchContext` 配合 try-with-resources，杜绝忘记恢复导致的"身份污染"。
2. **多租户一致性**: 在多租户环境下，身份切换后所有的 `QFilter` 会自动遵循该租户的隔离逻辑。
3. **配合线程池**: 在高并发环境下，推荐使用此模式自动处理上下文传递。

## 重要说明

### 反射机制风险
该类通过反射机制直接操作 `RequestContext` 的私有字段，存在以下风险：
- **脆性风险**: 深度依赖金蝶 BOS 内部实现，若 BOS 版本升级修改了字段定义（一般不会发生此种情况），此类将失效。

### 支持的上下文属性
通过 `switchContext` 可覆盖的常用属性：
- `userId` - 用户 ID
- `userName` - 用户名
- `userOpenId` - 用户 OpenID
- `tenantId` - 租户 ID
- `locale` - 语言环境

## 常见坑位
1. **忘记恢复**强烈推荐使用 Guard 模式**。
2. **空上下文报错**: 如果在静态初始化块中调用切换方法，由于主线程尚无上下文，可能会报错。
3. **事务关联**: 异步线程默认不继承主线程的数据库事务，如果异步操作涉及写库，需开启独立事务。
4. **Session 失效**: 备份中不包含 Web Session，仅包含后端业务执行所需的元数据标识。