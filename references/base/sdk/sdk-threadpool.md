# 线程池 (Unified Thread Pool)

## 概述
苍穹平台提供了统一的线程池管理机制，用于替代原生的 JDK 线程池。使用平台线程池可以确保线程生命周期的安全管理、自动清理线程变量，并支持将 `RequestContext` (请求上下文) 自动传递到异步线程中。

## 核心类
- **`kd.bos.threads.ThreadPools`**: 获取和创建线程池的工厂类。
- **`kd.bos.threads.ThreadPool`**: 平台封装的线程池接口。

## 常用 API 方法
### 快速执行
- `executeOnceIncludeRequestContext(String name, Runnable runnable)`: 携带当前上下文执行一次异步任务。

### 创建线程池
- `newCachedThreadPool(String name, int coreSize, int maxSize)`: 创建可缓存线程池。
- `newFixedThreadPool(String name, int size)`: 创建固定大小线程池。

### 提交任务
- `execute(Runnable task)`: 提交任务。
- `executeIncludeRequestContext(Runnable task)`: 提交任务并携带上下文。
- `submit(Callable<T> task)`: 提交带返回值的任务。

## 示例代码
```java
import kd.bos.threads.ThreadPool;
import kd.bos.threads.ThreadPools;

public class ThreadDemo {
    // 1. 建议将线程池定义为静态常量（全局共享）
    private static final ThreadPool pool = ThreadPools.newFixedThreadPool("MyBizPool", 5);

    public void runAsyncTask() {
        // 2. 提交携带上下文的异步任务
        pool.executeIncludeRequestContext(() -> {
            // 在此异步线程中可以正常调用 RequestContext.get()
            doComplexBusiness();
        });
    }
    
    public void runOnce() {
        // 3. 简单场景：单次快速异步
        ThreadPools.executeOnceIncludeRequestContext("SingleTask", () -> {
            log.info("异步执行中...");
        });
    }
}
```

## 实践建议
1. **优先携带上下文**：在业务逻辑中，务必使用 `xxxIncludeRequestContext` 系列方法，否则异步线程中无法获取用户信息、账套信息及进行数据库操作。
2. **全局化维护**：线程池应作为类的静态成员变量，严禁在方法内部频繁创建和关闭线程池。
3. **命名规范**：创建线程池时必须指定有业务语义的 `name`，以便在 `monitor` 监控中定位问题。

## 常见坑位
1. **直接 new Thread**：严禁在代码中直接使用 `new Thread().start()`，这会导致上下文丢失且线程不可控。
2. **拒绝策略**：平台线程池对阻塞队列做了优化，默认不会丢失任务，但在高负载下可能会阻塞提交线程，需注意响应时间。
3. **内存泄露**：虽然平台会自动清理线程变量，但如果在异步线程中使用 `ThreadLocal` 存储了大对象且未手动移除，仍存在泄露风险。
