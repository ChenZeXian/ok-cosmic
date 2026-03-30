# 报表取数插件

## 概述
报表取数插件用于接管报表列表的数据查询与列定义，适合复杂查询、自定义数据源、左树右表联动和动态列场景。

> **适用边界**
> ✅ 本文档直接使用：报表取数插件无封装层，直接参考本文档。

## 核心基类


- 基类：`kd.bos.entity.report.AbstractReportListDataPlugin`

## 核心事件

- `query(ReportQueryParam queryParam, Object selectedObj)`：报表查询入口，返回 `DataSet`。
- `getColumns(List<AbstractReportColumn> columns)`：调整显示列定义，控制隐藏、顺序、宽度、冻结等属性。

## 插件内上下文方法

以下方法属于查询过程中的上下文访问能力，不建议当成“自动触发事件”处理：

- `getQueryParam()`：获取当前报表查询参数。
- `getSelectedObj()`：获取左树或左表当前选中对象。
- `setProgress(int)`：异步查询时回传进度。

```java
ReportQueryParam qp = this.getQueryParam();
Object selected = this.getSelectedObj();
this.setProgress(30);
```

## 其他扩展点

- `export(...)`：导出时复用或覆盖查询结果。
- `exportWithSheet(...)`：多 sheet 导出扩展。
- `queryBatchBy(...)`：分批查询扩展。
- `getDynamicColumns(...)`：动态列构造扩展。

## 示例代码

示例代码统一维护在模板文件中，直接参考：

- [ReportListDataPluginTemplate.java](../../../assets/ReportListDataPluginTemplate.java)

## 实践建议

1. `query` 只负责取数和必要聚合，格式化尽量放界面插件。
2. 左树右表场景优先使用 `selectedObj` 限定范围。
3. 大数据量异步查询时使用 `setProgress(int)` 反馈执行进度。
4. 列控制集中放在 `getColumns`，不要在 `query` 混入列逻辑。

## 常见坑位

- 把 `getQueryParam()`、`getSelectedObj()`、`setProgress(int)` 当成事件去写说明或示例。
- 忽略 `selectedObj`，导致左树右表点击节点后仍返回全量数据。
- `query` 返回空对象而不是合法 `DataSet`。
- 在 `query` 里逐行远程调用，导致报表超时。