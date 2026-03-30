# 禁忌清单 (Anti-Patterns)

## 幻觉方法名黑名单

| 错误写法 | 正确替代 | 说明 |
|---|---|---|
| `setReadOnly(...)` | `getView().setEnable(false, "key")` | 苍穹不存在 setReadOnly 方法 |
| `afterCreateControl(...)` | `afterBindData` / `registerListener` | 不存在该事件方法 |
| `IDataModel.setReadOnly(...)` | `getView().setEnable(false, "key")` | 模型层不负责 UI 状态 |
| `this.getView().refresh()` | `this.getView().updateView(key)` | 不存在 refresh()，使用 updateView |
| `model.getEntryCount(...)` | `model.getEntryRowCount(entryKey)` | 方法名和参数都不同 |
| `model.deleteRow(...)` | `model.deleteEntryRow(entryKey, rowIndex)` | 方法名不同 |
| `model.addRow(...)` | `model.createNewEntryRow(entryKey, rowIndex)` | 方法名不同 |
| `destroy(...)` | `destory(...)` | 苍穹方法名就是 destory（少 r），不是 destroy |

## 幻觉类名黑名单

- ❌ 不存在以 `Cosmic` 或 `Cloud` 开头的工具类，除非脚本明确查到。
- ❌ 不存在 `BillHelper`（应为 `BusinessDataServiceHelper`）。
- ❌ 不存在 `FormHelper`（应为 `FormUtils`，位于 `kd.cd.common.form`）。
- ❌ 不存在 `ListHelper`（应为 `BaseDataServiceHelper` 或 `QueryServiceHelper`）。
- ❌ 不存在 `PluginHelper`（应按具体场景使用对应 ServiceHelper）。

## 场景错配黑名单

| 错误做法 | 原因 | 正确做法 |
|---|---|---|
| 在操作插件中调用 `this.getView()` | 操作插件无 UI 上下文 | 使用 `log` 或 `addErrorMessage` |
| 在操作插件中 `this.getModel().setValue(...)` | 操作插件不通过 model 操作 | 直接操作 `DynamicObject` 数据包 |
| 在 UI 插件中做重查询/复杂事务 | UI 插件应保持轻量 | 移至操作插件或服务层 |
| 在 `registerListener` 中调用 `model.getValue(...)` | 此时数据尚未绑定 | 推迟到 `afterBindData` |
| 在 `afterCreateNewData` 中期望触发 `propertyChanged` | 此时赋值不触发 | 在 `afterBindData` 中处理级联 |
| 仅 `implements Listener` 不注册监听 | 监听不会生效 | 在 `registerListener` 中调用 `add*Listener` |
| 对继承型插件 `@Override` 不调 `super.xxx()` | 基类初始化逻辑不执行 | 继承型必须先调 `super`；接口型无需 |
| 使用 `StringUtils` 做字符串判空 | 风格不一致 | 使用 `CharSequenceUtils.isBlank(...)` |
| 使用 `!= null && !isEmpty()` 判集合 | 风格不一致 | 使用 `CollectionUtils.isNotEmpty(...)` |
| 散落调用多个 `OperationServiceHelper` | 缺少错误聚合 | 使用 `OpUtils` 或 `OperateChain` |
| 手拼 `PushArgs`/`DrawArgs` 重复样板 | 有封装可用 | 使用 `BotpUtils` |
| 对基础资料反复 `queryDataSet` | 性能差 | 使用 `BusinessDataServiceHelper.loadFromCache(...)` |
| 直接 `dynamicObject.get("a.b.c")` | 不安全 | 使用 `DynamicObjectUtils` 安全取值 |
| 在插件成员变量中持有 `DataSet`、`InputStream` 等 | 序列化问题 | 使用后立即关闭，不持有引用 |
| 使用非 final 的 static 变量存储状态 | 多实例冲突 | 使用 `PageCache` 或实例变量 |