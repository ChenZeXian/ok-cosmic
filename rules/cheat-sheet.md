# 高频 API 速查表

以下 API 签名已确认准确，AI 可直接使用无需脚本验证。

## QFilter 构造与组合 `[查询]`

```java
// 基本构造
new QFilter("field", QCP.equals, value)
new QFilter("field", QCP.not_equals, value)
new QFilter("field", QCP.large_than, value)
new QFilter("field", QCP.less_than, value)
new QFilter("field", QCP.large_equals, value)
new QFilter("field", QCP.less_equals, value)
new QFilter("field", QCP.in, new Object[]{v1, v2, v3})
new QFilter("field", QCP.not_in, new Object[]{v1, v2, v3})
new QFilter("field", QCP.like, "%keyword%")
new QFilter("field", QCP.is_null, null)
new QFilter("field", QCP.is_not_null, null)

// 组合
filter1.and(filter2)
filter1.or(filter2)
```

## Model 读写 `[表单] [单据]`

```java
// 读取字段值
Object val = getModel().getValue("fieldKey");                          // 表头字段
Object val = getModel().getValue("fieldKey", rowIndex);                // 分录字段
Object val = getModel().getValue("fieldKey", rowIndex, parentRowIndex);// 子分录字段

// 设置字段值（自动触发 propertyChanged）
getModel().setValue("fieldKey", value);                                // 表头
getModel().setValue("fieldKey", value, rowIndex);                      // 分录

// 分录操作
int rowCount = getModel().getEntryRowCount("entryKey");               // 分录行数
int newRow = getModel().createNewEntryRow("entryKey", rowIndex);      // 新增分录行
getModel().deleteEntryRow("entryKey", rowIndex);                      // 删除分录行
DynamicObject dataEntity = getModel().getDataEntity();                // 获取完整数据包
DynamicObjectCollection entry = dataEntity.getDynamicObjectCollection("entryKey"); // 分录集合
```

## View 控制 `[表单] [单据] [列表]`

```java
// 启用/禁用
getView().setEnable(boolean, "key1", "key2"...);       // 支持多 key

// 显示/隐藏
getView().setVisible(boolean, "key1", "key2"...);      // 支持多 key

// 通知
getView().showSuccessNotification(String msg);
getView().showErrorNotification(String msg);
getView().showTipNotification(String msg);

// 操作
getView().invokeOperation(String opKey);               // 调用操作
getView().updateView(String key);                      // 刷新控件
getView().updateView();                                // 刷新整个视图
getView().showForm(FormShowParameter/ListShowParameter); // 打开页面

// PageCache（跨事件传值）
getView().getPageCache().put(String key, String value);
String val = getView().getPageCache().get(String key);

// 获取控件
Control ctrl = getView().getControl("controlKey");
```

## 封装工具类速查 `[通用]`

```java
// 字符串判空 — kd.cd.core.util.CharSequenceUtils
CharSequenceUtils.isBlank(str)
CharSequenceUtils.isNotBlank(str)
CharSequenceUtils.equals(a, b)

// 集合判空 — kd.cd.core.util.CollectionUtils
CollectionUtils.isEmpty(collection)
CollectionUtils.isNotEmpty(collection)

// DynamicObject 取值 — kd.cd.common.util.DynamicObjectUtils
DynamicObjectUtils.safeGet(dynamicObject, "field")
DynamicObjectUtils.setOf(dynamicObjects, "field")
DynamicObjectUtils.listOf(dynamicObjects, "field")

// 查询 — kd.cd.common.util.QueryUtils
QueryUtils.querySingle(entityId, field, QFilter...)          // 查一条一字段
QueryUtils.queryAsList(entityId, field, QFilter...)          // 查一列转 List
QueryUtils.queryAsSet(entityId, field, QFilter...)           // 查一列转 Set
QueryUtils.queryAsMap(entityId, keyField, valueField, QFilter...) // 双列转 Map
QueryUtils.queryDataSet(entityId, selectFields, QFilter...)  // 查 DataSet
QueryUtils.queryMatchedPkSet(entityId, QFilter...)           // 查匹配主键

// 操作 — kd.cd.common.operate.OpUtils
OpUtils.executeOperateOrThrow(opKey, entityId, new Object[]{pk})
OpUtils.executeOperateOrThrow(opKey, entityId, dataEntities)
OpUtils.throwIfFail(operationResult)
OpUtils.addErrorMessage(plugin, dataEntity, message)
```

## 弹窗与回调 `[表单] [单据]`

```java
// 打开 F7 列表弹窗
ListShowParameter lsp = ShowParameterUtils.getF7List(formId, multiSelect);
getView().showForm(lsp);

// 打开普通表单弹窗
FormShowParameter fsp = ShowParameterUtils.getForm(formId, OperationStatus.ADDNEW, ShowType.Modal, "500px", "300px");
fsp.setCustomParam("key", value);
fsp.setCloseCallBack(new CloseCallBack(this, "actionId"));
getView().showForm(fsp);

// 确认框
ConfirmCallBackListener listener = new ConfirmCallBackListener("callbackId", this);
getView().showConfirm("确认信息？", MessageBoxOptions.YesNo, listener);
```

## 操作链 `[操作]`

```java
// 链式操作
OperateChain.of(dataEntity).save().submit().audit().failThenThrow();
OperateChain.of(entityId, pkValue).save().submit().failThenDeleteAndThrow();
```