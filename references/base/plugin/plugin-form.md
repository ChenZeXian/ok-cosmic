# 动态表单插件

## 概述
动态表单是金蝶云苍穹的基础 UI 承载，支持各类表单场景（单据、基础资料、活动等）。动态表单插件用于在表单加载、初始化、交互全生命周期进行业务逻辑干预。

> **适用边界**
> ✅ 本文档是原生兜底：当 `plugin-base.md`(封装层) 未覆盖你需要的表单事件时使用。
> ❌ 如果封装层 `AbstractFormPluginExt` 已满足需求，优先读 `references/adv/plugin-base.md`。

- 适用场景：表单字段联动、控件状态管理、事件拦截、参数验证

术语说明：文档中的 `F7` 指基础资料、引用数据等“选择引用数据”的控件弹窗场景，不是键盘按键事件。

## 核心基类


- 基类：`kd.bos.form.plugin.AbstractFormPlugin`
- 继承关系：`AbstractFormPlugin extends AbstractDataModelPlugin implements IFormPlugin`

## 额外监听器

- `BeforeF7SelectListener`：F7（基础资料/引用数据选择控件）弹出前，调整过滤条件
- `RowClickEventListener`：表格行点击
- `TreeNodeClickListener`：树形节点点击
- `TreeNodeCheckListener`：树形节点勾选
- `TabSelectListener`：标签页切换
- `ItemClickListener`：菜单/按钮点击
- `ClickListener`：控件通用点击
- `ProgresssListener`：进度条变化

## 核心事件

- `setPluginName`：// 显示界面前，准备显示配置时触发
- `preOpenForm`：// 显示界面前，准备参数时触发
- `loadCustomControlMetas`：// 显示界面前，构建参数时触发
- `setView`：// 表单视图模型初始化时调用，传入 IFormView
- `initialize`：// 表单视图初始化后触发
- `registerListener`：// 创建后触发，用于注册控件监听
- `getEntityType`：// 创建数据包前触发
- `createNewData`：// 开始新建数据包时触发
- `afterCreateNewData`：// 新建数据包完毕后触发
- `beforeBindData`：// 刷新前端前触发
- `afterBindData`：// 刷新前端后触发
- `beforeItemClick`：// 菜单按钮点击前触发
- `itemClick`：// 菜单按钮点击时触发
- `beforeDoOperation`：// 执行操作前触发
- `afterDoOperation`：// 执行操作后触发
- `confirmCallBack`：// 确认提示后触发
- `closedCallBack`：// 子界面关闭时触发
- `flexBeforeClosed`：// 弹性域维护界面关闭时触发
- `onGetControl`：// 获取控件编程模型时触发
- `customEvent`：// 自定义控件定制事件触发
- `TimerElapsed`：// 定时触发
- `beforeClosed`：// 界面关闭前触发
- `destory`：// 界面关闭后释放资源时触发
- `pageRelease`：// 界面关闭后释放资源时触发
- `beforeclick`：// 点击前校验事件
- `click`：// 点击后触发操作事件
- `propertyChanged`：// 修改字段值后触发

## 插件内上下文方法

```java
// 获取表单视图模型
IFormView view = this.getView();

// 获取表单数据模型
IDataModel model = this.getModel();

// 获取指定控件
Control control = this.getView().getControl("controlKey");
BasedataEdit basedataEdit = this.getView().getControl("basedatafield");
EntryGrid entryGrid = this.getView().getControl("entryentity");

// 获取字段值
Object value = this.getModel().getValue("fieldKey");

// 设置字段值（自动触发propertyChanged）
this.getModel().setValue("fieldKey", newValue);

// 控件启用/禁用
this.getView().setEnable(false, "controlKey");

// 控件隐显
this.getView().setVisible(false, "controlKey");

// 获取完整数据包
DynamicObject dataEntity = model.getDataEntity();
```

说明：`this.getView().getControl("controlKey")` 的返回类型是 `<T extends kd.bos.form.control.Control>`，所有控件都继承自 `kd.bos.form.control.Control`，实际拿到的是具体控件对象，例如 `BasedataEdit`、`EntryGrid`、`ProgressBar` 等。

## 示例代码

示例代码统一维护在模板文件中，直接参考：

- [FormPluginTemplate.java](../../../assets/FormPluginTemplate.java)

## 实践建议

1. **registerListener中统一注册所有监听**
   - 不要分散在多个事件中注册监听
   - 每个监听最多 add 一次

2. **propertyChanged做联动**
   - 修改后的级联操作放propertyChanged

3. **afterCreateNewData内赋值不触发propertyChanged**
   - 文档明确说明此时不触发propertyChanged
   - 需要联动时在afterBindData里处理

4. **子页面返回统一走closedCallBack**
   - 不要在各处open子页面时写回调
   - 所有子页面返回由closedCallBack统一处理

5. **重逻辑不要放插件**
   - 复杂业务逻辑写服务层
   - UI插件仅做编排与界面控制

## 常见坑位

### ❌ 仅implements接口不注册监听
```java
// 错误：只implements但没注册
public class TreeClickBad extends AbstractFormPlugin implements TreeNodeClickListener {
    @Override
    public void treeNodeClick(TreeNodeClickEvent e) {
        // 永远不会被调用
    }
}

// 正确：implement后在registerListener注册
public class TreeClickGood extends AbstractFormPlugin implements TreeNodeClickListener {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        TreeView tv = this.getView().getControl("treeKey");
        if (tv != null) {
            tv.addTreeNodeClickListener(this);  // 必须注册
        }
    }
}
```

### ❌ 在registerListener里调用model.getValue
- 此时数据尚未绑定，getValue返回null
- 应在afterBindData或propertyChanged里操作

### ❌ 期望afterCreateNewData触发propertyChanged
- 此时赋值不会触发propertyChanged事件
- 级联更新需在afterBindData里显式处理

### ❌ 嵌套树/表格重复注册监听
- 同一控件avoid重复addListener
- 使用标识符检查是否已注册

### ❌ destory拼写错误
- 方法名是destory（少r），不是destroy
- 跟风覆盖时要按文档准确拼写