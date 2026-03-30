# 单据转换插件

## 概述
单据转换插件用于在业务流转换（下推、选单）过程中介入源单取数、目标单创建、字段映射与关联关系生成。

> **适用边界**
> ✅ 本文档是原生兜底：BOTP 转换/反写没有 Ext 封装，直接读本文档。
> ❌ 下推/选单的业务调用优先用 `BotpUtils`，参见 `references/adv/botp-convert.md`。

- 适用场景：下推规则扩展、选单逻辑增强、字段映射补充、分单合单策略调整

## 核心基类
- 基类：`kd.bos.entity.botp.plugin.AbstractConvertPlugIn`
- 继承关系：`AbstractConvertPlugIn implements IConvertPlugIn`

## 核心事件

- `initVariable`：// 初始化上下文变量（源单主实体、目标单主实体、转换规则、转换方式）
- `afterBuildQueryParemeter`：// 构建取数参数后，可追加字段与过滤条件
- `beforeBuildRowCondition`：// 编译行过滤条件前，可替换/追加规则条件
- `afterBuildRowCondition`：// 编译行过滤条件后，可补充诊断信息或微调最终条件
- `beforeGetSourceData`：// 取源单数据前，可调整语句与条件
- `afterGetSourceData`：// 取源单数据后，可补充第三方数据或替换源数据
- `beforeBuildGroupMode`：// 分单/合并策略构建前，可调整分单合并依赖字段
- `beforeCreateTarget`：// 创建目标单前（官方说明仅选单场景触发）
- `afterCreateTarget`：// 创建目标单后（官方说明主要下推场景触发）
- `afterFieldMapping`：// 字段映射完成后，补充目标字段赋值
- `afterBizRule`：// 业务规则执行后，做最终字段修正
- `beforeCreateLink`：// 生成关联关系前，可取消/调整关联关系
- `afterCreateLink`：// 生成关联关系后，补充关联携带数据
- `afterBuildDrawFilter`：// 选单条件生成后，追加插件过滤条件
- `afterConvert`：// 转换流程最后事件，做最终修正

## 插件内上下文方法

```java
// 上下文获取
BillEntityType srcMainType = this.getSrcMainType();
BillEntityType tgtMainType = this.getTgtMainType();
ConvertRuleElement rule = this.getRule();
ConvertOpType opType = this.getOpType();  // 下推/选单
```

## 示例代码

示例代码统一维护在模板文件中，直接参考：

- [ConvertPlugInTemplate.java](../../../assets/ConvertPlugInTemplate.java)

## 实践建议

1. 先在 `initVariable` 明确当前上下文（下推/选单）再分支处理。
2. 源单过滤尽量放 `beforeBuildRowCondition` 与 `beforeGetSourceData`，避免后置大规模删数据。
3. 字段补齐优先放 `afterFieldMapping`，规则执行后的兜底修正再放 `afterBizRule`。
4. 选单场景的额外过滤优先放 `afterBuildDrawFilter`，不要把选单过滤和普通取数过滤混写。
5. 关联追溯场景谨慎处理 `beforeCreateLink`，取消后会影响上下游可追踪性。

## 常见坑位

- 误把 `beforeCreateTarget` 当所有场景必触发，导致逻辑漏执行。
- 在 `afterConvert` 做大量数据库操作，影响转换时长。
- 漏掉源单关键字段准备，后续事件中字段取值为 null。
- 选单场景忘记区分 `afterBuildDrawFilter` 与普通取数过滤，导致过滤位置不稳定。
- 关联关系被取消后，后续反写/追踪功能异常。