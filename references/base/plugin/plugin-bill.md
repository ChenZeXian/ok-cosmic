# 单据界面插件

## 概述
单据界面插件继承动态表单插件能力，并增加单据特有事件，适合处理加载后初始化与单据交互控制。

> **适用边界**
> ✅ 本文档是原生兜底：当 `plugin-base.md`(封装层) 未覆盖你需要的单据事件时使用。
> ❌ 如果封装层 `AbstractBillPlugInExt` 已满足需求，优先读 `references/adv/plugin-base.md`。

- 适用场景：单据加载初始化、字段联动、操作前后 UI 协同

术语说明：文档中的 `F7` 指基础资料、引用数据等选择控件弹窗，不是键盘按键事件。

## 核心基类


- 基类：`kd.bos.bill.AbstractBillPlugIn`
- 继承关系：`AbstractBillPlugIn extends AbstractFormPlugin implements IBillPlugin`

## 额外监听器

- `BeforeF7SelectListener`：F7（基础资料/引用数据选择控件）选择前拦截
- `ItemClickListener`：菜单/按钮点击监听
- `ClickListener`：控件通用点击监听
- `TreeNodeClickListener`：树节点点击监听

## 核心事件

- `afterLoadData(EventObject e)`：// 单据数据加载完成后触发，适合做加载后初始化
- `beforeDoOperation(EventObject e)`：// 操作执行前触发，适合做前置校验/参数整理
- `afterDoOperation(EventObject e)`：// 操作执行后触发，适合做结果提示/刷新协同
- `propertyChanged(EventObject e)`：// 字段值变更后触发，适合做联动赋值

说明：单据插件同时支持动态表单全套事件（生命周期与交互事件与 `plugin-form.md` 一致）。

## 插件内上下文方法

```java
// 视图与模型
IFormView view = this.getView();
IDataModel model = this.getModel();

// 读取单据常用字段
String billNo = (String) model.getValue("billno");
String billStatus = (String) model.getValue("billstatus");

// 控件级操作
Control control = view.getControl("fieldkey");
BasedataEdit basedataEdit = view.getControl("basedatafield");
view.setEnable(false, "fieldkey");
```

说明：`view.getControl("fieldkey")` 的返回类型是 `<T extends kd.bos.form.control.Control>`，所有控件都继承自 `kd.bos.form.control.Control`，实际拿到的是具体控件对象，例如 `BasedataEdit`、`EntryGrid`、`ProgressBar` 等。

## 示例代码

示例代码统一维护在模板文件中，直接参考：

- [BillPlugInTemplate.java](../../../assets/BillPlugInTemplate.java)

## 实践建议

1. 加载后初始化优先放 `afterLoadData`。
2. 事务级校验放操作插件，UI 插件只做交互提示与轻校验。
3. 字段联动优先在 `propertyChanged`，避免分散在多个事件。

## 常见坑位

- 新增单据不触发 `afterLoadData`，初始化逻辑需兼顾 `afterCreateNewData`。
- 在 UI 插件中直接改状态字段替代操作流，容易与业务状态机冲突。