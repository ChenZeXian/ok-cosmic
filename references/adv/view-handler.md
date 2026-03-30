# 页面打开与视图管理 (ViewHandler & AutoCloseViewHandler)

## 概述
在单据交互逻辑中，频繁涉及"点击 A 单据打开 B 单据界面"的需求。`ViewHandler` (位于 `kd.cd.common.form.handler`) 采用了链式 API 封装了复杂的 `FormShowParameter` 逻辑，支持一键实现新增、修改、查看、列表界面的弹出或新页签跳转。特别适用于后台任务中需要打开视图进行数据初始化或验证的场景。

> **适用边界**
> ✅ 适用：后台/操作插件中打开表单/列表界面、新页签跳转。
> ❌ 不适用：表单插件中的简单弹窗优先用 `getView().showForm()`，参见 `form-utils.md`。

## 核心类
- **`kd.cd.common.form.handler.ViewHandler`**: 视图操作核心工具接口。
- **`kd.cd.common.form.handler.AutoCloseViewHandler`**: **推荐使用**。自动关闭的视图处理器，实现了 `AutoCloseable`。
- **`kd.bos.form.FormShowParameter`**: 原生表单显示参数。
- **`kd.bos.bill.OperationStatus`**: 页面操作状态枚举。

## 常用 API 方法

### 1. 工厂方法
- `ViewHandler.of(String formId)`: 指定目标单据标识。
- `ViewHandler.of(String formId, Object initOrgId)`: 指定单据及初始化组织ID。
- `AutoCloseViewHandler.of(String formId)`: **推荐**。创建自动关闭的视图处理器。
- `AutoCloseViewHandler.of(String formId, Object initOrgId)`: 创建带组织的自动关闭视图处理器。

### 2. 参数设置
- `addCustomParam(String name, Object value)`: 向目标页面传递自定义业务参数。
- `setOpenOnDesignerPage()`: 设置忽略许可校验。
- `beforeOpen(Consumer<FormShowParameter> c)`: 注入原生参数调整代码。

### 3. 界面动作执行
- `openAddNewView()`: 以新增状态打开。
- `openAddNewViewAndInit(DynamicObject dataEntity)`: 以新增状态打开并初始化数据。
- `openModifyView(Object pkValue)`: 以修改状态打开指定单据。
- `openPeekView(Object pkValue)`: 以只读查看状态打开。
- `openListView()`: 打开目标单据的列表页。
- `openView(Object pkValue, OperationStatus operationStatus)`: 指定状态打开。
- `openViewByShowParameter(FormShowParameter sp)`: 自定义参数打开。
- `closeView()`: 关闭当前视图。

### 4. 调试方法
- `peek()`: 提取界面所有数据快照（调试用）。
- `peekUIMsg()`: 预览 UI 消息队列（调试用）。
- `peekUIMsgFlat()`: 获取平面化的消息列表（调试用）。
- `flatList(String field)`: 扁平列出字段值（调试用）。

## 示例代码

### AutoCloseViewHandler 模板（推荐）
```java
package kd.cd.common.demo;

import kd.cd.common.form.handler.AutoCloseViewHandler;
import kd.bos.form.IFormView;

public class ViewHandlerDemo {
    public void backendOperation() {
        // 使用 try-with-resources 自动关闭视图
        try (AutoCloseViewHandler vh = AutoCloseViewHandler.of("kdcd_test")) {
            vh.beforeOpen(sp -> sp.setAppId("fi"));

            // 打开修改视图
            IFormView view = vh.openModifyView(1462749241941L);

            // 在后台视图中进行数据操作
            // ...do something here...
        } // 自动调用 close()，无需手动关闭
    }
}
```

### 带组织参数的新增视图
```java
public void openWithOrg(Long orgId) {
    try (AutoCloseViewHandler vh = AutoCloseViewHandler.of("my_bill", orgId)) {
        vh.beforeOpen(sp -> sp.setAppId("fi"));
        IFormView view = vh.openAddNewView();

        // 操作视图数据...
    } // 自动关闭
}
```

### 带数据初始化的新增视图
```java
public void openWithInit(DynamicObject initData) {
    try (AutoCloseViewHandler vh = AutoCloseViewHandler.of("my_bill")) {
        // 打开新增视图并初始化数据
        IFormView view = vh.openAddNewViewAndInit(initData);

        // 继续操作...
    } // 自动关闭
}
```

## ViewHandler vs AutoCloseViewHandler

| 场景 | 推荐使用 | 说明 |
|------|---------|------|
| 后台任务打开视图 | `AutoCloseViewHandler` | 配合 try-with-resources 自动关闭 |
| 前台界面跳转 | `ViewHandler` | 由用户手动关闭或系统管理 |
| 异步线程中操作 | `AutoCloseViewHandler` | 确保资源释放 |

## 重要说明

### 组织参数处理
- 传入的 `initOrgId` 对于单据会传入初始化组织参数。
- 对于基础资料会传入使用组织初始参数（仅当使用组织字段无字段名时）。
- 在打开新增视图并且主业务组织字段为空时，还会尝试为主业务组织字段赋值。

### 后台视图特点
- `openPeekView` 相关方法后台默认以查看的方式打开视图，不触发编辑操作的网络互斥。
- 打开的页面默认拥有权限。
- **必须关闭视图**：后台打开的视图必须调用 `closeView()` 或使用 `AutoCloseViewHandler`。

## 实践建议
1. **优先使用 AutoCloseViewHandler**: 后台场景下使用 `AutoCloseViewHandler` 配合 try-with-resources，避免忘记关闭视图导致资源泄露。
2. **链式调用**: 链式调用极大增强了代码可读性，相比原生 `new FormShowParameter` 可减少 50% 以上的代码量。
3. **参数解耦原则**: 传递大型数据对象时，建议先存储在 `PageCache` 中，仅通过 `addCustomParam` 传递其 CacheKey。
4. **状态回调**: 弹出界面返回后，建议在 `closedCallBack` 处理业务逻辑。

## 常见坑位
1. **主键类型错误**: `openModifyView` 传入的 PK 类型（Long/String）必须与物理存储一致，否则页面会报错。
2. **PageId 冲突**: 如果自定义 PageId，确保在并发环境下具有唯一性。
3. **权限校验**: 界面触发的跳转仍会受系统权限体系约束，如果当前用户无权查看目标单据，页面将弹出报错。
4. **忘记关闭视图**: 使用 `ViewHandler` 时忘记调用 `closeView()` 会导致内存泄露和会话资源占用，**推荐使用 `AutoCloseViewHandler`**。