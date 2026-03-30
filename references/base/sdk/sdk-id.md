# 分布式 ID (Distributed ID)

## 概述
分布式 ID 服务用于在多微服务节点环境下产生全局唯一的 ID 值。它主要用作实体的主键，或其他需要唯一标识的业务场景。该服务具备高性能、趋势有序和高可用的特性。

## 核心类
- **`kd.bos.id.ID`**: ID 生成的核心工具类。

## 常用 API 方法
- `ID.genLongId()`: 获取单个 `long` 类型 ID（对应 DB 字段 `bigint(19)`）。
- `ID.genLongIds(int count)`: 一次性获取指定数量的 `long` 类型 ID 数组。
- `ID.genStringId()`: 获取单个 `String` 类型 ID（对应 DB 字段 `varchar(12)`）。
- `ID.genStringIds(int count)`: 一次性获取指定数量的 `String` 类型 ID 数组。

## 示例代码
```java
import kd.bos.id.ID;

public class IdDemo {
    public void generateId() {
        // 生成主键 ID
        long billId = ID.genLongId();
        
        // 批量生成主键
        long[] batchIds = ID.genLongIds(10);
        
        // 生成字符串类型的唯一标识
        String traceId = ID.genStringId();
    }
}
```

## 实践建议
1. **主键必选**：凡是苍穹平台的业务实体主键，务必使用 `ID.genLongId()` 生成，不要使用外部随机数或自增 ID。
2. **批量获取**：在需要大批量创建单据或分录的循环中，建议先调用 `genLongIds` 一次性拿到 ID 数组，以减少内部锁竞争。
3. **类型匹配**：`long` 类型 ID 是对 `String` 类型进行 Base39 编码后的结果，两者可以互转且保持趋势有序。

## 常见坑位
1. **历史数据转换**：旧系统初始化数据或第三方系统导入的 ID 可能不是通过此服务生成的，强行互转可能导致数据错误。
2. **种子耗尽风险**：如果微服务节点频繁异常重启且系统时间跳动过大，可能导致 WorkerId 种子用完（上限 8192），此时需要清理 Zookeeper 上的种子节点。
3. **依赖 DLock**：ID 服务启动时依赖分布式锁（DLock）获取种子，若 Redis/Zookeeper 异常导致锁不可用，ID 服务将无法启动。
