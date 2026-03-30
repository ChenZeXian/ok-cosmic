# 分布式锁 (Distributed Lock)

## 概述
分布式锁用于在分布式环境下对共享资源执行排他性操作。它能确保在多个微服务实例并发执行时，同一时刻只有一个线程能持有特定标识的锁。苍穹平台提供不可重入和可重入两种锁类型。

## 核心类
- **`kd.bos.dlock.DLock`**: 分布式锁的核心管理类。

## 常用 API 方法
- `DLock.create(String lockId, String desc)`: 创建一个不可重入锁。
- `DLock.createReentrant(String lockId, String desc)`: 创建一个可重入锁（推荐，防止同一线程死锁）。
- `lock()`: 阻塞式加锁，直到成功为止。
- `tryLock()`: 尝试加锁，不等待，立即返回成功或失败。
- `tryLock(long timeout, TimeUnit unit)`: 在指定时间内尝试加锁。
- `unlock()`: 释放锁。**必须在 finally 块中调用**。

## 示例代码
```java
import kd.bos.dlock.DLock;
import java.util.concurrent.TimeUnit;

public class LockDemo {
    public void executeWithLock() {
        // 1. 创建锁标识（建议包含模块和业务主键）
        DLock lock = DLock.createReentrant("my_module/order/12345", "订单12345并发锁");
        
        // 2. 加锁
        try {
            if (lock.tryLock(5, TimeUnit.SECONDS)) {
                try {
                    // 执行排他性业务逻辑
                    processBusiness();
                } finally {
                    // 3. 释放锁
                    lock.unlock();
                }
            } else {
                throw new KDBizException("获取锁超时，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

## 实践建议
1. **优先使用可重入锁**：通过 `createReentrant` 创建的锁可以防止同一线程在嵌套调用中产生的死锁问题。
2. **Key 命名规范**：锁标识应具备唯一性且易于识别，建议采用 `模块/业务类型/主键ID` 的格式。
3. **超时控制**：尽量使用 `tryLock` 并设置合理的超时时间，避免因长时间阻塞导致微服务线程池枯竭。

## 常见坑位
1. **忘记释放锁**：如果不在 `finally` 中执行 `unlock()`，一旦业务代码抛出异常，锁将无法及时释放，直到节点心跳超时。
2. **死锁风险**：不可重入锁在同一个线程内重复调用 `lock()` 会导致永久阻塞。
3. **宕机延迟释放**：若持有锁的节点崩溃，锁会自动保留直到服务注册中心将其移除（约 5 分钟），期间其他节点可能无法获取该锁。
