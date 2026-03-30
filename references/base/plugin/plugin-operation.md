# 单据操作插件

## 概述
单据操作插件用于在单据操作（保存、提交、审核、删除等）的事务内进行业务逻辑干预。与UI插件不同，操作插件运行在服务端，有数据库事务保护，用于实现关键业务逻辑。

> **适用边界**
> ✅ 本文档是原生兜底：当 `plugin-base.md`(封装层) 未覆盖你需要的操作事件时使用。
> ❌ 如果封装层 `AbstractOperationServicePlugInExt` 已满足需求，优先读 `references/adv/plugin-base.md`。

- 适用场景：单据操作的权限校验、数据校验、状态转换、级联数据同步
- 执行环境：**服务端事务内**（不同于UI插件）

## 核心基类
- 基类：`kd.bos.entity.plugin.AbstractOperationServicePlugIn`
- 继承关系：`AbstractOperationServicePlugIn implements IOperationServicePlugIn, IOperationService`
- 执行位置：**服务端事务内**（不是客户端）

## 核心事件（时间顺序）

- `onPreparePropertys(PreparePropertysEventArgs e)`：// 加载单据数据包前触发；用于补齐操作所需字段，避免后续取值为空
- `onAddValidators(AddValidatorsEventArgs e)`：// 校验器加载完毕后、执行校验前触发；用于增删校验器
- `beforeExecuteOperationTransaction(BeforeOperationArgs e)`：// 校验通过后、开启事务前触发；用于最后整理数据
- `beginOperationTransaction(BeginOperationTransactionArgs e)`：// 事务开启后、数据库提交前触发；用于事务内同步处理
- `endOperationTransaction(EndOperationTransactionArgs e)`：// 数据写库后、事务提交前触发；用于事务内后置处理
- `rollbackOperation(RollbackOperationArgs e)`：// 事务提交失败回滚后触发；用于无事务资源补偿
- `afterExecuteOperationTransaction(AfterOperationArgs e)`：// 事务提交后触发；用于消息通知、日志等后续处理

## 插件内上下文方法

```java
// 获取操作上下文
BillEntityType billType = this.billEntityType;  // 单据主实体
OperateMeta operateMeta = this.operateMeta;    // 操作信息
OperationResult result = this.operationResult;  // 操作结果

// 获取自定义参数
Map<String, Object> options = this.getOption();
String customParam = (String) options.get("paramKey");

// 添加错误信息
result.addErrorMessage("错误提示文本");

// 添加成功提示
result.addSuccessMessage("操作成功");

// 设置操作状态
result.setSuccess(false);  // 标记操作失败
```

## 示例代码

示例代码统一维护在模板文件中，直接参考：

- [OpPluginTemplate.java](../../../assets/OpPluginTemplate.java)

## 实践建议

1. **必须理解状态转换逻辑**
   - 单据状态有固定转换路径，不是任意转换
   - 同一操作绑定多个插件时避免重复状态转换

2. **onPreparePropertys中不要漏字段**
   - 若操作依赖某字段，必须在此添加
   - 否则系统加载的数据包会缺失此字段

3. **规则校验优先放onAddValidators，事务前事件只做最后整理**
   - 明确的业务规则优先注册校验器，失败会在事务开启前阻断
   - `beforeExecuteOperationTransaction` 更适合最后整理数据、轻量兜底校验与整体取消

4. **数据修改放beginOperationTransaction/endOperationTransaction**
   - 这两个事件都在事务内，异常会自动回滚
   - 数据库操作在此区间是安全的

5. **级联数据同步放afterExecuteOperationTransaction**
   - 此时事务已提交，可放心调用其他服务
   - 但要做好异常处理，不要中断主逻辑

## 常见坑位

### ❌ 把所有校验都堆进beforeExecuteOperationTransaction
- 这样会弱化校验器机制，后续复用和错误定位都更差
- 明确规则优先放 `onAddValidators`，事务前事件只保留少量兜底检查

### ❌ endOperationTransaction后想回滚
- endOperationTransaction时数据已入库
- 无法再回滚，只能在beforeExecuteOperationTransaction做校验

### ❌ 多个操作绑定同一状态转换
```java
// 错误：一级审批和会审都在同意时调用审核通过操作
// 导致单据从"已提交"试图再转为"已审核"->失败

// 正确：应在工作流配置中区分不同路径，操作插件不重复
```

### ❌ 在afterExecuteOperationTransaction里修改单据
- 此时事务已提交，修改会立即生效
- 应在beforeExecuteOperationTransaction或事务内完成修改

### ❌ 忽视onPreparePropertys导致字段缺失
```java
// 如果操作插件要读某字段但没在onPreparePropertys添加
// 系统加载的数据包就不含此字段，导致getValue返回null
```

### ❌ 在操作插件里查询与操作无关的数据
- 操作插件应只关注当前操作的单据数据
- 复杂查询逻辑优先写在服务层

## 预置操作清单

系统预置的可绑定操作（其他操作不支持操作插件）：

| 操作 | 功能 |
|------|------|
| save | 保存单据到数据库 |
| saveandnew | 保存后清空界面进入新增 |
| statusconvert | 切换单据状态 |
| submit | 提交单据（状态→已提交） |
| submitandnew | 提交后新增 |
| unsubmit | 撤销提交（回到暂存） |
| audit | 审核（状态→已审核） |
| unaudit | 反审核（状态→暂存） |
| disable | 禁用 |
| enable | 启用 |
| invalid | 作废 |
| valid | 生效 |
| delete | 删除 |
| donothing | 空操作（用于触发事件流程） |