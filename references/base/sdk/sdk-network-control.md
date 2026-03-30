# 网络控制 (Data Mutex / NetCtrl)

## 概述
网络控制（也称网控或互斥锁）用于解决多用户并发操作同一单据时产生的数据冲突问题。它主要分为**功能互斥**（如审核时禁止编辑）和**数据互斥**（如同一单据禁止两人同时修改）。

## 核心类
- **`kd.bos.mutex.DataMutex`**: 网控核心接口类（支持 AutoCloseable）。
- **`kd.bos.form.operate.MutexHelper`**: 网控操作帮助类。
- **`kd.bos.mutex.MutexLockInfo`**: 定义锁的具体信息（实体、主键、操作标识等）。

## 常用 API 方法
### 1. 申请网控
- `DataMutex.create().require(MutexLockInfo info)`: 申请单条数据的网控锁。
- `batchrequire(List<Map<String, Object>> data)`: 批量申请网控。

### 2. 释放网控
- `release(String objId, String entityKey, String operationKey)`: 释放单条网控。
- `batchRelease(List<Map<String, Object>> data)`: 批量释放。

### 3. 获取锁信息
- `getLockInfo(String objId, String groupId, String entityKey)`: 查询当前数据被谁锁定。

## 示例代码
```java
import kd.bos.mutex.DataMutex;
import kd.bos.mutex.MutexLockInfo;

public void doTaskWithMutex(String billId) {
    // 1. 创建网控对象（使用 try-with-resources 自动管理）
    try (DataMutex mutex = DataMutex.create()) {
        // 2. 定义锁信息
        MutexLockInfo lockInfo = new MutexLockInfo();
        lockInfo.setDataObjId(billId);
        lockInfo.setEntityKey("my_entity_id");
        lockInfo.setOperationKey("submit");
        lockInfo.setGroupId("default_netctrl"); // 默认互斥组
        
        // 3. 申请锁
        if (mutex.require(lockInfo)) {
            try {
                // 执行核心业务逻辑
                process();
            } finally {
                // 4. 显式释放（重要）
                mutex.release(billId, "my_entity_id", "submit");
            }
        } else {
            throw new KDBizException("单据已被其他用户锁定，请稍后重试");
        }
    } catch (Exception e) {
        logger.error("网控异常", e);
    }
}
```

## 实践建议
1. **自动释放**：始终使用 `try-with-resources` 语法创建 `DataMutex` 实例，以确保底层连接被正确关闭。
2. **细粒度控制**：合理利用 `groupId`。如果希望你的逻辑不与系统标准操作冲突，可以自定义 `groupId`。
3. **友好提示**：申请锁失败时，建议调用 `getLockInfo` 并将占用者的信息提示给用户。

## 常见坑位
1. **死锁风险**：严禁在持有锁的过程中执行超长时间的同步等待（如调用外系统 API），这会导致单据被长时间锁定无法操作。
2. **忘记释放**：手动调用的 `require` **必须**有对应的 `release` 调用，否则该单据可能一直处于“锁定中”状态，直到用户 Session 超时。
3. **同用户重入**：默认情况下，同用户多次申请同一把锁是允许的（可重入），但需注意逻辑闭环。
