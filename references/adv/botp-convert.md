# 单据转换与下推 (BotpUtils & PushResult)

## 概述
单据下推（Push）与转换（Convert）是苍穹实现业务流转（如订单下推入库）的核心机制。`BotpUtils` (位于 `kd.cd.common.util`) 对原生的转换服务进行了深度简化，支持一键下推并保存、下推并显示界面、上拉数据以及多层级的正/反向链路追踪。

> **适用边界**
> ✅ 适用：下推/选单/来源追踪/转换结果处理。
> ❌ 不适用：单纯的操作执行（保存/审核）请用 `OpUtils`；转换插件的事件扩展请用 `ConvertPlugInTemplate`。

## 核心类
- **`kd.cd.common.util.BotpUtils`**: **下推核心工具类**。
- **`kd.cd.common.util.PushResult`**: 下推结果封装对象。
- **`kd.bos.entity.botp.runtime.ConvertOperationResult`**: 原生转换结果。

## 常用 API 方法

### 1. 下推操作 (BotpUtils)
- `pushAndSave(String sourceEntityId, String targetEntityId, Object srcPkValue, Map<String, List<Long>> entryMapping, String ruleId, Consumer<PushArgs> consumer)`: **最常用**。一对一后台下推并自动保存。
- `pushNoSave(String sourceEntityId, String targetEntityId, Object srcPkValue, Map<String, List<Long>> entryMapping, String ruleId, Consumer<PushArgs> consumer)`: 下推生成内存对象，不持久化。
- `push(boolean autoSave, PushArgs pushArgs)`: 通用下推方法。
- `pushAndShowTarget(IFormView view, boolean autoSave, PushArgs pushArgs)`: 下推并直接在界面弹出目的单。

### 2. 上拉操作 (BotpUtils)
- `drawToBill(IFormView view, String sourceEntityId, Collection<ListSelectedRow> selectRows, String ruleId, Consumer<DrawArgs> consumer)`: 下拉数据至当前单据。

### 3. 链路追踪 (BotpUtils)
- `findAllTargetBills(String targetEntityId, String entityId, Collection<Long> pkValues)`: 正向查找生成的全量目的单，按当前单据主键分组。
- `findAllTargetBillsFlat(String targetEntityId, String entityId, Collection<Long> pkValues)`: 正向查找，不分组。
- `findAllSourceBills(String sourceEntityId, String entityId, Collection<Long> pkValues)`: 反向查找来源单据，按当前单据主键分组。
- `findAllSourceBillsFlat(String sourceEntityId, String entityId, Collection<Long> pkValues)`: 反向查找，不分组。
- `findDirectTargetBills(String targetEntityId, String entityId, Collection<Long> pkValues)`: 查找直接下游（第一层）。
- `findDirectSourceBills(String sourceEntityId, String entityId, Collection<Long> pkValues)`: 查找直接上游（第一层）。

### 4. 辅助方法
- `loadTargetEntities(ConvertOperationResult result)`: 从下推结果中加载完整的目的单 DynamicObject。
- `getConvertFailMsg(ConvertOperationResult result)`: 获取下推失败信息详情。
- `getDefaultRuleId(String sourceEntityId, String targetEntityId)`: 获取默认转换规则ID（启用状态）。
- `buildSelectedRows(Map<Object, Map<String, List<Long>>> billData)`: 构建下推选择行信息。

## 示例代码

### 后台自动下推模板
```java
package kd.cd.common.demo;

import kd.cd.common.util.BotpUtils;
import kd.cd.common.util.PushResult;

public class PushDemo {
    public void autoPush(String sourceFormId, String targetFormId, Object sourcePk) {
        // 1. 下推并保存
        PushResult result = BotpUtils.pushAndSave(sourceFormId, targetFormId, sourcePk, null, null);
        result.failThenThrow();

        // 2. 获取生成的单据主键
        Object[] targetPks = result.getPks();
    }
}
```

### 链路追踪示例
```java
public void traceBills(String entityId, Collection<Long> pkValues) {
    // 查询所有下游目标单（按当前单据主键分组）
    Map<Long, Map<String, Set<Long>>> targetBills = BotpUtils.findAllTargetBills(entityId, pkValues);

    // 查询特定类型的直接上游源单
    Map<Long, Set<Long>> sourceBills = BotpUtils.findDirectSourceBills("pm_purorderbill", entityId, pkValues);
}
```

## 实践建议
1. **RuleId 获取**: 建议通过 `BotpUtils.getDefaultRuleId` 获取单据间的默认规则，避免硬编码。
2. **分录下推**: 使用 `entryMapping` 参数可精确控制需要下推的分录行。
3. **加载明细**: `loadTargetEntities` 获取的对象包含分录的全量数据。
4. **链路追踪**: 使用 `findDirectXxx` 方法仅查询直接关联，`findAllXxx` 方法递归查询全量关联。

## 常见坑位
1. **规则未分配**: 如果返回空结果，优先检查"转换规则"是否在目标组织中已发布。
2. **分录转换缺失**: 如果 RuleId 中未配置分录，下推后的单据将只有表头。
3. **主键精度丢失**: `Object` 类型的 ID 在跨系统传递时需注意 JS 的数字精度限制（Long -> String）。
4. **参数顺序**: 注意 pushAndSave/pushNoSave 的参数顺序是 `(sourceEntityId, targetEntityId, ...)` 而非旧版的 `(targetId, ruleId, sourceIds, orgId)`。