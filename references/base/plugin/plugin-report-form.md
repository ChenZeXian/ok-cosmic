# 报表界面插件

## 概述
报表界面插件用于控制过滤容器、查询前后处理、行数据加工、导出行为与列展示。

> **适用边界**
> ✅ 本文档直接使用：报表界面插件无封装层，直接参考本文档。

术语说明：文档中的 `F7` 指过滤容器里的基础资料/引用数据选择控件弹窗，不是键盘按键事件。

## 核心基类
- 基类：`kd.bos.report.plugin.AbstractReportFormPlugin`
- 继承关系：`AbstractReportFormPlugin extends AbstractFormPlugin`

## 核心事件

- `initDefaultQueryParam(...)`：设置默认查询参数。
- `filterContainerInit(...)`：初始化过滤容器。
- `filterContainerBeforeF7Select(...)`：过滤容器 F7（基础资料/引用数据选择控件）打开前拦截。
- `filterContainerSearchClick(...)`：点击过滤容器查询按钮时触发。
- `verifyQuery(...)`：查询前校验。
- `beforeQuery(...)`：执行查询前追加或修正查询条件。
- `afterQuery(...)`：查询完成后处理。
- `afterCreateColumn(...)`：列创建完成后调整列显示属性。
- `packageData(...)`：单元格数据打包时格式化前端显示值。
- `processRowData(...)`：行数据返回后、渲染前进行加工。
- `preProcessExportData(...)`：导出数据预处理。
- `setExcelName(...)`：设置导出文件名。
- `setSortAndFilter(...)`：设置列排序和列过滤能力。

## 插件内上下文方法

以下更适合作为界面插件中的上下文能力，不建议当成“自动触发事件”：

- `getView()`：获取报表视图。
- `getModel()`：获取报表数据模型。
- `getQueryParam()`：获取当前查询参数。
- `getControl("reportlistap")`：获取报表控件，返回 `Control` 基类或具体控件对象。

```java
ReportList reportList = this.getControl("reportlistap");
ReportQueryParam qp = this.getQueryParam();
reportList.setSelectedAll(true);
```

说明：`this.getControl("reportlistap")` 的返回类型是 `<T extends kd.bos.form.control.Control>`，所有控件都继承自 `kd.bos.form.control.Control`，实际拿到的是具体控件对象，例如 `ReportList`、`BasedataEdit`、`EntryGrid` 等。

## 其他扩展点

- `getComboItems(...)`
- `colHeadFilterClick(...)`
- `afterSetModelValue(...)`
- `loadOtherEntryFilter(...)`
- `resetColumns(...)`
- `resetDataCount()`
- `setCellStyleRules(...)`
- `setFloatButtomData(...)`
- `setMergeColums(...)`
- `setRowCellStyleEvent(...)`
- `setFlexProperty(...)`
- `setTreeReportList(...)`
- `setOtherEntryFilter(...)`

这些方法更适合归为专项扩展点，而不是和核心查询事件并列。

## 示例代码

示例代码统一维护在模板文件中，直接参考：

- [ReportFormPluginTemplate.java](../../../assets/ReportFormPluginTemplate.java)

## 实践建议

1. 查询合法性校验统一放 `verifyQuery(...)`。
2. 过滤容器初始化与默认参数初始化要配合使用。
3. `processRowData(...)` 避免逐行查库，导出时会放大性能问题。
4. 导出相关逻辑放在 `preProcessExportData(...)` 和 `setExcelName(...)`。

## 常见坑位

- 把 `getView()`、`getQueryParam()` 这类上下文访问能力写成事件说明。
- 在 `packageData(...)` 中实现导出逻辑。
- `verifyQuery(...)` 不校验空条件导致全表扫描。
- 把大量专项扩展点和核心查询事件混在一起，模板难以聚焦。