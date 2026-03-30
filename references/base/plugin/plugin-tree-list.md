# 左树右表单据列表插件

## 概述
左树右表插件在标准列表上增加了“分组树 + 右侧列表”的协同能力，适用于组织树、分类树、业务分组树筛选场景。

> **适用边界**
> ✅ 本文档是原生兜底：左树右表有两个模板（TreeListPluginTemplate/StandardTreeListPluginTemplate），事件签名参考本文档。
> ❌ 封装层 Ext 基类的通用部分优先读 `references/adv/plugin-base.md`。

## 核心基类
- 基类：`kd.bos.list.plugin.AbstractTreeListPlugin`
- 继承关系：`AbstractTreeListPlugin extends AbstractListPlugin implements ITreeListPlugin, SearchEnterListener`

## 核心事件

- `initializeTree(...)`：树模型初始化时触发。
- `initTreeToolbar(...)`：树工具栏初始化后触发。
- `treeNodeClick(...)`：节点点击后触发。
- `buildTreeListFilter(...)`：基于当前节点构建右侧列表过滤条件。
- `refreshNode(...)`：树节点刷新时触发。
- `treeToolbarClick(...)`：树工具栏按钮点击时触发。
- `search(...)`：树搜索回车时触发。
- `beforeShowBill(...)`：打开右侧详情前触发。

## 插件内上下文方法

以下属于树列表插件的上下文访问能力，不建议继续按“事件”理解：

- `getTreeListView()`：获取树列表视图模型。
- `getTreeModel()`：获取树模型。
- `getView()` / `getModel()`：访问页面视图和数据模型。

```java
ITreeListView treeListView = this.getTreeListView();
ITreeModel treeModel = this.getTreeModel();
Object currentNodeId = treeListView.getCurrentNodeId();
```

## 其他扩展点

- `setTreeListView(...)`：树列表视图模型注入阶段，偏框架上下文准备，不建议与业务事件并列。
- `nodeClickFilter()`：返回点击节点时附加到右表的过滤条件，更适合作为过滤扩展点理解。
- `setCustomerParam()`：树列表自定义参数构建扩展。
- `expendTreeNode(...)`：有的文档或版本中会出现，但官方说明更推荐统一在 `refreshNode(...)` 处理中做懒加载。

## 示例代码

示例代码统一维护在模板文件中，直接参考：

- [TreeListPluginTemplate.java](../../../assets/TreeListPluginTemplate.java)
- [StandardTreeListPluginTemplate.java](../../../assets/StandardTreeListPluginTemplate.java)

## 实践建议

1. 树节点驱动右表过滤优先放在 `buildTreeListFilter(...)`。
2. 需要树模型时通过 `getTreeModel()` 访问，不要把它当成事件。
3. 懒加载场景优先统一放在 `refreshNode(...)` 处理。
4. 打开详情页前如需透传来源信息，可放在 `beforeShowBill(...)`。

## 常见坑位

- 把 `getTreeListView()`、`getTreeModel()` 这类上下文方法写进事件总览。
- 在 `treeNodeClick(...)` 里直接写大量查询逻辑，导致点击卡顿。
- 节点懒加载和刷新逻辑分散在多个事件里，后续难维护。
- 只在 `initializeTree(...)` 加节点，没有同步处理 `refreshNode(...)`。