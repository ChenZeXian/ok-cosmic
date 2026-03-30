# 动态领域模型 - 公共服务 (ServiceHelpers 全集)

## 概述
苍穹平台提供了一系列 ServiceHelper，作为分布式环境下的基础工具类。它们封装了复杂的跨服务调用，使开发者能够以本地静态调用的方式完成数据存取、元数据获取、业务操作触发、编码生成及微服务调用。

## 核心类与用途
| 分类 | 核心类 | 说明 |
| :--- | :--- | :--- |
| **数据存取** | `BusinessDataServiceHelper` | 结构化对象加载（含缓存、主子表）。 |
| **数据查询** | `QueryServiceHelper` | 轻量级查询、DataSet 获取。 |
| **持久化操作** | `SaveServiceHelper` / `DeleteServiceHelper` | 物理层的数据新增、修改、物理/逻辑删除。 |
| **业务逻辑** | `OperationServiceHelper` | 执行单据生命周期操作（审核、提交等）。 |
| **编码/规则** | `CodeRuleServiceHelper` | 自动生成符合编码规则的单据编号。 |
| **基础资料** | `BaseDataServiceHelper` | 基础资料的分配、自动补值及权限过滤。 |
| **跨微服务** | `DispatchServiceHelper` | 调用其他微服务、二开服务或平台服务。 |
| **系统/环境** | `SystemParamServiceHelper` / `TimeServiceHelper` | 获取系统参数、全局统一时间。 |

## 常用 API 方法

### 1. 编码规则 (CodeRuleServiceHelper)
- `getNumber(String entityId, DynamicObject data)`: 获取单据编号。
- `getBatchCodes(String entityId, int count)`: 批量获取编码。

### 2. 基础资料分配与补值 (BaseDataServiceHelper)
- `assign(String entityId, long[] ids, long[] orgIds)`: 将基础资料分配到目标组织。
- `queryBaseDataFromCache(...)`: 高性能获取基础资料字段值。

### 3. 微服务分发与工厂 (DispatchServiceHelper / ServiceFactory)
- **调用业务微服务**: `DispatchServiceHelper.invokeBizService(cloud, app, svc, method, args)`。
- **调用二开服务**: `DispatchServiceHelper.invokeService(factory, app, svc, method, args)`。
- **自定义工厂模式**:
```java
public class ServiceFactory {
    public static Object getService(String serviceName) {
        String impl = "com.isv.mservice.impl." + serviceName + "Impl";
        return TypesContainer.getOrRegisterSingletonInstance(impl);
    }
}
```

### 4. 数据删除 (DeleteServiceHelper)
- `delete(String entityId, Object[] ids)`: 物理删除。
- `deleteOperate(...)`: 执行删除操作（触发删除插件）。

### 5. 系统参数 (SystemParamServiceHelper)
- `getValue(String paramKey)`: 获取全局参数。
- `getValue(String paramKey, long orgId)`: 获取组织隔离的参数值。

## 示例代码
### 1. 自动生成单据编号
```java
// 创建新对象并自动根据规则填入编号
DynamicObject bill = BusinessDataServiceHelper.newDynamicObject("my_entity");
String billNo = CodeRuleServiceHelper.getNumber("my_entity", bill);
bill.set("billno", billNo);
```

### 2. 调用外部微服务
```java
// 调用“供应链云”下的“库存服务”
Object result = DispatchServiceHelper.invokeBizService(
    "scm", "im", "StockService", "queryInventory", 
    new Object[]{materialId, orgId}
);
```

## 实践建议
1. **优先使用 Helper**：ServiceHelper 系列是苍穹开发的最标准入口，具备良好的事务和权限兼容性。
2. **时间统一性**：获取业务时间务必使用 `TimeServiceHelper.getSystemDate()`，以保证在集群环境下所有节点的时间严格一致。
3. **参数缓存**：系统参数读取较为频繁，`SystemParamServiceHelper` 内部已有良好缓存，无需在业务层二次缓存。
4. **基础资料**：你在处理基础资料（BaseData）相关的业务动作时，请务必先查阅 `BaseDataServiceHelper` 是否已有现成的方法。

## 常见坑位
1. **删除服务误用**：`DeleteServiceHelper.delete` 是物理删除，且不触发任何插件；对于业务单据，建议始终使用 `deleteOperate`。
2. **跨云调用超时**：使用 `DispatchServiceHelper` 调用时，若目标服务负载过高，可能导致当前线程挂死，建议对不稳定接口增加异步处理。
3. **编码规则失效**：调用 `getNumber` 前，单据对象中必须包含规则中定义的“条件字段”（如组织、业务类型），否则生成的编码可能不符合预期。