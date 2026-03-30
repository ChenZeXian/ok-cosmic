# 分布式事务 (Distributed Transaction / KDTX)

## 概述
KDTX (Kingdee Distributed Transaction) 是苍穹平台提供的分布式事务解决方案，主要用于解决跨微服务、跨数据库操作时的数据一致性问题。它支持“最终一致性”和“TCC”两种核心模式。

## 核心类
- **`kd.bos.kdtx.sdk.api.EventualConsistencyService`**: 最终一致性服务基类。
- **`kd.bos.kdtx.sdk.api.TCCService`**: TCC 模式服务基类。
- **`kd.bos.db.tx.TX`**: 本地数据库事务控制工具。

## 常用 API 方法
### 1. 最终一致性模式 (推荐)
- `EventualConsistencyService.register(Class<? extends EventualConsistencyService> clazz)`: 注册异步执行任务。
- `addProperty(String key, Object value)`: 传递业务参数。

### 2. 本地事务控制
- `TX.required()`: 如果当前没有事务，则开启一个新事务；如果有，则加入。
- `txHandle.commit()`: 提交事务。

## 示例代码
### 实现最终一致性服务
```java
import kd.bos.kdtx.sdk.api.EventualConsistencyService;

// 1. 定义任务逻辑
public class MySyncTask extends EventualConsistencyService {
    @Override
    public void invoke() throws Exception {
        String billNo = (String) this.getProperty("billno");
        // 执行具体的跨系统同步逻辑...
    }
}

// 2. 在主业务逻辑中注册
public void auditBill(DynamicObject bill) {
    try (TXHandle tx = TX.required()) {
        // 修改本地状态
        bill.set("status", "C");
        SaveServiceHelper.update(bill);
        
        // 注册异步同步任务（本地事务提交后才会真正触发执行）
        EventualConsistencyService.register(MySyncTask.class)
            .addProperty("billno", bill.getString("billno"));
            
        tx.commit();
    }
}
```

## 实践建议
1. **优先最终一致性**：绝大多数业务场景（如发邮件、同步第三方接口）建议使用最终一致性模式，以保证主流程的响应速度和高可用。
2. **幂等性要求**：分布式事务任务可能会因网络波动而重试，`invoke()` 方法内部逻辑**必须**实现幂等。
3. **事务范围最小化**：不要在事务块（`TX.required`）中放置耗时的网络 IO 操作。

## 常见坑位
1. **参数序列化**：通过 `addProperty` 传递的对象必须支持序列化（基础类型或 DynamicObject）。
2. **事务嵌套风险**：注意 `TX.required` 与外部事务的交互，避免产生意料之外的回滚。
3. **异常处理**：`invoke()` 方法抛出异常会导致 KDTX 框架自动重试，请确保异常捕获的准确性。
