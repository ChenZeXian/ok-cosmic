# 单据反写插件

## 概述
单据反写插件用于下游单据在保存/审核等操作时，把计算结果反写回上游单据，并参与关闭行、超额检查、保存回滚等完整流程。

> **适用边界**
> ✅ 本文档是原生兜底：反写插件没有 Ext 封装，直接读本文档。
> ❌ 下推/选单的业务调用优先用 `BotpUtils`，参见 `references/adv/botp-convert.md`。

- 适用场景：数量金额反写、上游行关闭控制、超额校验定制、反写补偿

## 核心基类
- 基类：`kd.bos.entity.botp.plugin.AbstractWriteBackPlugIn`
- 继承关系：`AbstractWriteBackPlugIn implements IWriteBackPlugIn`

## 核心事件

- `preparePropertys`：// 读取下游目标单前，准备所需目标字段
- `beforeTrack`：// 构建关联记录前，可取消本关联主实体反写
- `beforeCreateArticulationRow`：// 构建单行关联记录前，可取消该行反写
- `beforeExecWriteBackRule`：// 执行反写规则前，可禁用当前规则
- `afterCalcWriteValue`：// 反写值计算后，修正分配量
- `beforeReadSourceBill`：// 读取源单前，准备源单字段
- `afterReadSourceBill`：// 读取源单后，补充第三方数据
- `afterCommitAmount`：// 反写写入源单行后，做连锁更新
- `beforeExcessCheck`：// 超额检查前，可取消检查
- `afterExcessCheck`：// 超额检查后，决定提示/中断
- `beforeCloseRow`：// 关闭上游行前（可跳过关闭条件检查）
- `afterCloseRow`：// 上游行关闭状态写入后
- `beforeSaveTrans`：// 开启保存事务前，准备第三方数据
- `beforeSaveSourceBill`：// 源单保存前
- `afterSaveSourceBill`：// 源单保存后
- `rollbackSave`：// 保存失败回滚补偿
- `finishWriteBack`：// 反写结束释放资源（如网控）

## 插件内上下文方法

```java
// 上下文
BillEntityType targetSubMainType = this.getTargetSubMainType();
String opType = this.getOpType();  // Draft/Save/Audit/UnAudit/Delete/...
LinkSetItemElement currLinkSetItem = this.getCurrLinkSetItem();
```

- `setContext(...)`：框架设置当前上下文的初始化入口，更适合作为上下文准备能力理解，而不是业务事件。

## 示例代码

示例代码统一维护在模板文件中，直接参考：

- [WriteBackPlugInTemplate.java](../../../assets/WriteBackPlugInTemplate.java)

## 实践建议

1. `preparePropertys` 与 `beforeReadSourceBill` 必须明确字段准备，避免后续空值。
2. 超额场景优先在 `afterExcessCheck` 做统一提示策略。
3. 涉及第三方系统写入，优先用 `beforeSaveTrans` + `rollbackSave` 做补偿闭环。
4. 资源申请（网控、缓存句柄）必须在 `finishWriteBack` 释放。

## 常见坑位

- 忽略 `rollbackSave`，保存失败后外部系统数据不一致。
- 在 `beforeExcessCheck` 一律取消检查，导致业务失控。
- `beforeTrack`/`beforeCreateArticulationRow` 误取消后反写缺失。
- `finishWriteBack` 未释放资源引发后续并发问题。