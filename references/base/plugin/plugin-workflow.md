# 工作流插件

## 概述
工作流插件用于在流程运行时参与参与人计算、条件判断、流程通知和审批记录格式化。

> **适用边界**
> ✅ 本文档直接使用：工作流插件是接口型（`IWorkflowPlugin`），无封装层，无需调用 super。

- 适用场景：动态审批人、条件分支、流程通知、审批记录定制

## 核心基类

- 基类：`kd.bos.workflow.engine.extitf.IWorkflowPlugin`

## 核心事件

- `calcUserIds(AgentExecution execution)`：参与人计算阶段触发。
- `hasTrueCondition(AgentExecution execution)`：条件分支判断阶段触发。
- `notify(AgentExecution execution)`：流程通知阶段触发。
- `notifyByWithdraw(AgentExecution execution)`：流程撤回阶段触发。
- `formatFlowRecord(IApprovalRecordItem item)`：审批记录展示格式化时触发。

## 插件内上下文方法

以下更适合作为 `AgentExecution` 上的上下文访问能力，而不是插件“事件”：

- `execution.getBusinessKey()`
- `execution.getEntityNumber()`
- `execution.getCurrentFlowElement()`
- `execution.getVariable(...)`
- `execution.setVariable(...)`
- `execution.getCurrentTaskResult()`
- `execution.getStartUserId()`
- `execution.setAssigneeList(...)`

```java
String businessKey = execution.getBusinessKey();
Object amount = execution.getVariable("amount");
execution.setVariable("lastNodeName", "财务审核");
```

## 其他扩展点

以下方法存在于接口默认实现或扩展能力中，但不建议在模板里与核心事件同层表达：

- `filterParticipant(...)`
- `handleTask(...)`
- `afterHandleTask(...)`
- `afterCancelTask(...)`
- `aggregateBills(...)`
- `getJointAuditResult(...)`
- `getExpireTime(...)`
- `getBillPermissions(...)`
- `validatePlugin(...)`
- `resetYZJGroupProperty(...)`

## 示例代码

示例代码统一维护在模板文件中，直接参考：

- [IWorkflowPluginTemplate.java](../../../assets/IWorkflowPluginTemplate.java)

## 实践建议

1. 参与人计算和条件分支尽量独立实现，避免职责混杂。
2. `notify` 与 `notifyByWithdraw` 尽量成对设计，保证状态可恢复。
3. 耗时逻辑不要放在 `notify` 中阻塞流程。
4. 流程变量与单据数据是两套数据，要显式同步。
5. `IWorkflowPlugin` 是接口型插件，不存在“先调用 `super.xxx()`”这一要求。

## 常见坑位

- 把 `execution.getVariable(...)`、`execution.setAssigneeList(...)` 这类上下文访问方法写成插件事件。
- 一个插件同时塞太多流程扩展点，后续维护困难。
- `notify` 修改了单据状态，但 `notifyByWithdraw` 没有补偿恢复。
- 参与人计算完成后没有真正替换审批人列表。