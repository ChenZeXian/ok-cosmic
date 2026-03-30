# 引入引出插件

## 概述
引入引出插件用于扩展 Excel 导入流程，包括批量策略、保存拦截、数据校验与日志记录。

> **适用边界**
> ✅ 本文档是原生兜底：引入引出场景直接使用，无封装层。
> ❌ 普通表单插件的导入事件已在 `FormPluginTemplate` 中省略，因为有独立的 `BatchImportPluginTemplate`。

## 核心基类


- 基类：`kd.bos.form.plugin.impt.BatchImportPlugin`
- 继承关系：`BatchImportPlugin implements Callable<Object>, IImportDataPlugin`

## 核心事件

- `save(List<ImportBillData> rowdatas, ImportLogger logger)`：每批导入数据保存时进入，是最核心的校验与过滤拦截点。
- `getBatchImportSize()`：初始化批量导入策略时调用，用于确定批次大小。
- `isForceBatch()`：初始化导入模式时调用，用于决定是否强制批处理。

## 插件内上下文方法

以下更适合作为导入插件的上下文/工具方法，不建议继续按“事件”理解：

- `getContext()`：获取导入上下文。
- `getLogger()`：获取导入日志对象。
- `getBatchSize()`：读取当前批次大小。
- `refreshHeartbeat()`：刷新任务心跳。
- `call()`：导入任务执行入口包装。

```java
Map<String, Object> billData = data.getData();
logger.log(data.getStartIndex(), "错误信息").fail();
return super.save(rowdatas, logger);
```

## 其他扩展点

- `beforeSave(...)`：保存前批次预校验。
- `resolveExcel()`：Excel 解析扩展。
- `importData()`：导入主流程扩展。
- `getDefaultImportType()` / `getDefaultKeyFields()`：默认导入配置扩展。

## 示例代码

示例代码统一维护在模板文件中，直接参考：

- [BatchImportPluginTemplate.java](../../../assets/BatchImportPluginTemplate.java)

## 实践建议

1. 导入校验优先集中在 `save(...)`。
2. 大数据量场景要明确 `getBatchImportSize()` 和 `isForceBatch()`。
3. 对失败行必须写 `ImportLogger`，方便用户回溯。
4. 优先过滤非法数据后再复用 `super.save(...)`。

## 常见坑位

- 把 `call()`、`refreshHeartbeat()`、`getContext()` 这种上下文能力写成“事件说明”。
- 直接抛异常中断整批导入，导致可导入数据也丢失。
- 不记录失败日志，用户无法定位错误行。
- 批次过大导致内存抖动或请求超时。