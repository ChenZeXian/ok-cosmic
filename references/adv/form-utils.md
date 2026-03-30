# 界面视图增强工具 (FormUtils)

## 概述
界面交互逻辑是插件开发最耗时的地方。`FormUtils` (位于 `kd.cd.common.form`) 提供了一系列针对 `IFormView` 的快捷操作，能够极大简化控件控制、视图判定、消息处理等代码。

> **适用边界**
> ✅ 适用：表单控件启用/禁用/隐藏、UI 消息、视图判定。
> ❌ 不适用：后台操作（无 UI 上下文）请用操作插件；后台打开表单/列表请用 `view-handler.md`。

## 核心类
- **`kd.cd.common.form.FormUtils`**: **界面工具核心类**。

## API 方法全览

### 1. 视图状态与属性
- `getBillFormId(IFormView view)`: 获取当前表单标识（原标识，非实体标识）。
- `getBillStatus(IFormView view)`: 获取单据审核状态。
- `getParentViewNoPlugin(IFormView view)`: 获取无插件的父页面视图。
- `getViewByPageId(String pageId)`: 通过 PageId 获取视图对象。
- `getPluginInstance(String pageId, Class<T> clazz)`: 获取某个视图对象的某个插件实例。
- `getAllPluginInstance(String pageId)`: 获取视图所有插件实例（调试用）。

### 2. 列表控件
- `getBillList(IFormView view)`: 获取列表控件（BillList）。
- `getReportList(IFormView view)`: 获取报表列表控件（ReportList）。
- `getListGrid(IFormView view, Class<T> clazz)`: 获取列表网格控件（泛型）。
- `getListButtonKeys(String formId, boolean includeDropDown)`: 获取列表所有按钮标识与其对应操作映射。

### 3. 控件筛选与检索
- `getAllControls(IFormView view)`: 获取当前界面所有控件 Map。
- `getAllControls(String formId)`: 获取表单中所有控件对象。
- `getSubControls(Container container, boolean includeContainer)`: 获取容器内的子控件。
- `selectControls(IFormView view, Predicate<Control> filter)`: 筛选表单控件。
- `selectSubControls(Container container, Predicate<Control> filter)`: 筛选容器子控件。

### 4. 表单标识获取
- `getF7ListFormId(String billFormId)`: 获取列表F7标识。
- `getListFormId(String billFormId)`: 获取列表标识。
- `getMasterFormId(String extFormId)`: 根据拓展表单标识获取原表单标识。

### 5. 元数据读取（数据库访问）
- `readListMetadata(String billFormId)`: 查询列表元数据。
- `readEntityMeta(String formId)`: 查询实体元数据。
- `readEntityMetaAsMap(String formId, String... fieldKeys)`: 查询实体元数据字段映射。
- `readFormMeta(String formId)`: 查询表单元数据。
- `readFormMetaAsMap(String formId, String... fieldKeys)`: 查询表单元数据字段映射。

### 6. UI 消息处理
- `peekUIMsg(IFormView view)`: 预览视图前端提示信息（调试用）。
- `getFlatUIMessages(IFormView view, boolean ignoreSuccessMsg)`: 提取视图前端提示信息。

## 示例代码

### 获取视图信息
```java
public void analyzeView(IFormView view) {
    // 获取表单标识
    String formId = FormUtils.getBillFormId(view);

    // 获取单据状态
    String status = FormUtils.getBillStatus(view);

    // 获取父页面视图
    IFormView parentView = FormUtils.getParentViewNoPlugin(view);
}
```

### 控件筛选
```java
public void filterControls(IFormView view) {
    // 获取所有控件
    Map<String, Control> allControls = FormUtils.getAllControls(view);

    // 筛选特定类型控件
    Map<String, Control> entryGrids = FormUtils.selectControls(view, EntryGrid.class::isInstance);

    // 获取列表按钮映射
    Map<String, String> buttonOps = FormUtils.getListButtonKeys(formId, true);
}
```

### 列表操作
```java
public void listOperation(IFormView listView) {
    // 获取列表控件
    BillList billList = FormUtils.getBillList(listView);

    // 通过PageId获取视图
    IFormView targetView = FormUtils.getViewByPageId(pageId);

    // 获取插件实例
    MyPlugin plugin = FormUtils.getPluginInstance(pageId, MyPlugin.class);
}
```

### UI 消息提取
```java
public void checkMessages(IFormView view) {
    // 获取所有非成功消息
    List<String> messages = FormUtils.getFlatUIMessages(view, true);

    // 预览所有UI消息（调试用）
    Map<String, ?> uiMsg = FormUtils.peekUIMsg(view);
}
```

## 实践建议
1. **控件筛选**: 使用 `selectControls` 配合方法引用可以快速筛选特定类型控件，如 `EntryGrid.class::isInstance`。
2. **父视图获取**: 在弹出界面中需要访问父页面时，使用 `getParentViewNoPlugin` 获取。
3. **按钮映射**: `getListButtonKeys` 可用于动态判断按钮对应的操作，便于条件控制。
4. **元数据读取**: `readXxxMeta` 系列方法会访问数据库，应避免高频调用。

## 常见坑位
1. **视图类型判断**: `getBillStatus` 仅适用于 `IBillView` 类型视图，对动态表单会抛异常。
2. **PageId 时效**: `getViewByPageId` 获取的视图对象仅在当前会话有效。
3. **控件 Key 区分**: 控件的 Key 与字段标识可能不同，需通过设计器确认。
4. **元数据缓存**: `readXxxMeta` 方法每次访问数据库，建议缓存结果。

## 相关工具类
- **`kd.cd.core.util.SystemPropertyUtils`**: 系统配置工具
  - `isProdEnv()`: 判定是否为生产环境
  - `get(String key, String def)`: 获取配置项
  - `getBoolean(String key, boolean def)`: 获取布尔配置项