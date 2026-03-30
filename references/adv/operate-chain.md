# 业务操作与链式事务 (OpUtils & OperateChain)

## 概述
业务单据的操作（保存、审核、下推等）是苍穹逻辑流转的基石。`OpUtils` (位于 `kd.cd.common.operate`) 提供了一键执行操作并反馈异常的原子 API；`OperateChain` 提供了流式 API 以执行复杂的操作组。本规范旨在确保单据操作的一致性与健壮性。

> **适用边界**
> ✅ 适用：需要连续执行多个操作（如保存→提交→审核）的链式场景。
> ❌ 不适用：单次操作（仅 save / submit / audit）优先用 `OpUtils.executeOperateOrThrow`，更简洁。

## 核心基类/工具类
- **`kd.cd.common.operate.OpUtils`**: **核心操作工具类**。
- **`kd.cd.common.operate.chain.OperateChain`**: 链式操作器。
- **`kd.cd.common.operate.chain.ChainStorage`**: 操作链数据存储。
- **`kd.bos.entity.operate.result.OperationResult`**: 单据操作结果。

## OpUtils API 方法

### 1. 核心操作执行
- `executeOperateOrThrow(String opKey, String entityId, DynamicObject[] dataEntities)`: **最常用**。执行操作，失败则抛出异常回滚事务。
- `executeOperateOrThrow(String opKey, String entityId, Object[] pks)`: 根据主键执行操作。
- `executeOperateOrThrow(String opKey, String entityId, DynamicObject[] dataEntities, OperateOption option)`: 带操作参数执行。
- `executeOperateOrThrow(String opKey, String entityId, Object[] pks, OperateOption option)`: 带操作参数根据主键执行。
- `throwIfFail(OperationResult result)`: 针对手动执行的操作结果进行校验并回滚。

### 2. 回滚删除
- `rollbackAndDelete(String entityId, Object pkValue)`: 如果后续逻辑失败，自动反审核并删除本单据。
- `rollbackAndDelete(IFormView view)`: 基于视图的回滚删除。
- `rollbackAndDelete(String entityId, Object pkValue, boolean requireNewTXForEachStage)`: 可控制事务隔离级别。

### 3. 字段准备（操作插件用）
- `prepareEntryFields(MainEntityType mainType, String... entryKeys)`: 一键生成包含分录所有子字段的列表（推荐）。
- `prepareAllFields(MainEntityType mainType)`: 准备所有加载字段（仅推荐公共插件中使用）。

### 4. 错误消息处理
- `addErrorMessage(AbstractOperationServicePlugIn plugin, DynamicObject dataEntity, String message)`: 在操作插件中手动回填校验错误消息。
- `addErrorMessage(AbstractOperationServicePlugIn plugin, DynamicObject dataEntity, String title, String message)`: 带标题的错误消息。
- `addErrorMessage(AbstractOperationServicePlugIn plugin, DynamicObject dataEntity, String title, String errCode, String message)`: 完整错误消息。
- `addValidatorErrorMsg(AbstractValidator validator, ExtendedDataEntity extDataEntity, String errCode, String message)`: 校验器错误消息。
- `getCompleteFailMsg(OperationResult result)`: 提取全量分录中的所有错误消息。
- `getChsName(String entityId, String opKey)`: 获取操作中文名。

## OperateChain API 方法

### 1. 工厂方法
- `OperateChain.of(String entityId, Object pkValue)`: 根据实体标识和主键创建。
- `OperateChain.of(DynamicObject dataEntity)`: 根据数据包创建。
- `OperateChain.of(IFormView view)`: 根据视图创建。

### 2. 链式操作方法
- `save()`: 保存。
- `save(OperateOption option)`: 带参数保存。
- `submit()`: 提交。
- `submit(OperateOption option)`: 带参数提交。
- `submitNoWF()`: 提交（不触发工作流）。
- `audit()`: 审核。
- `audit(OperateOption option)`: 带参数审核。
- `unSubmit()`: 撤销提交。
- `unAudit()`: 反审核。
- `delete()`: 删除。
- `operate(String opKey)`: 执行自定义操作。
- `operate(String opKey, OperateOption option)`: 带参数执行自定义操作。

### 3. 结果处理
- `isSuccess()`: 判断操作链是否成功。
- `storage()`: 获取操作链数据存储。
- `failThenDelete()`: 失败后删除单据。
- `failThenDeleteAndThrow()`: 失败后删除并抛异常。
- `failThenDeleteAndThrow(String tip)`: 失败后删除并抛异常（带提示）。
- `failThenThrow()`: 失败后抛异常。
- `failThenThrow(String tip)`: 失败后抛异常（带提示）。

### 4. 事务控制
- `requireNewTXForEachStage(boolean require)`: 是否为每个操作阶段开启独立事务。
- `getGlobalOptionControl()`: 获取全局操作参数控制器。

## 示例代码

### 静默审核单据模板
```java
package kd.cd.common.demo;

import kd.cd.common.operate.OpUtils;

public class OpDemo {
    public void auditBill(String formId, Object pkId) {
        // 1. 调用审核操作，失败将自动抛出带完整错误描述的异常
        OpUtils.executeOperateOrThrow("audit", formId, new Object[]{pkId});
    }
}
```

### 链式操作模板
```java
public void chainOperation(DynamicObject dataEntity) {
    // 链式操作：保存 -> 提交 -> 审核 -> 自定义操作
    OperateChain chain = OperateChain.of(dataEntity);
    if (!chain.save().submit().audit().operate("pay").isSuccess()) {
        String errMsg = chain.storage().getErrMsg();
        // 处理失败逻辑
    }
}
```

### 失败后删除
```java
public void operateWithRollback(String entityId, Object pkValue) {
    // 创建单据后，后续操作失败则自动回滚删除
    OperateChain chain = OperateChain.of(entityId, pkValue);
    chain.save().submit().audit();
    chain.failThenDelete();  // 如果前面操作失败，会自动回滚删除
}
```

### 操作插件字段准备
```java
public class MyOperationPlugin extends AbstractOperationServicePlugInExt {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        // 加载分录所有字段(不推荐，只适用公共的操作插件，不要轻易尝试)
        e.setFieldKeys(allFields());
        // 或仅加载特定分录字段
        e.setFieldKeys(entryFields("entry1", "entry2"));
    }
}
```

## 实践建议
1. **优先使用 OrThrow**: 除非你需要手动显示每一个分录的特定报错，否则 `executeOperateOrThrow` 是保持业务逻辑清晰的最佳方式。
2. **多语言注意**: 所有的错误消息回填建议使用多语言属性。
3. **事务范围**: 在操作插件中使用 `OpUtils` 时，其操作会自动被包在当前的数据库事务中。
4. **链式操作**: 使用 `OperateChain` 可以优雅地处理连续操作，支持短路（前一操作失败后不再执行后续操作）。

## 常见坑位
1. **ID 数组错误**: `executeOperateOrThrow` 的 ID 数组必须类型一致（长整型或字符串），混合类型会导致单据找不到。
2. **状态拦截**: 对已审核单据再次执行 `audit` 操作会返回失败，执行前应检查单据状态。
3. **忽略结果检查**: 严禁直接调用 `OperationServiceHelper` 却不通过 `OpUtils.throwIfFail` 检查结果，这会导致逻辑静默失败。
4. **独立事务**: 开启 `requireNewTXForEachStage(true)` 后链路将失去"整体原子性"，需自行处理补偿逻辑。