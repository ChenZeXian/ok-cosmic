# 扩展插件基类 (Plugin Base Extensions)

## 概述
扩展插件基类（Ext系列）是基于金蝶苍穹原生插件类的二次封装，通过继承 `kd.cd.common.plugin` 下的基类，开发者可以自动获得日志记录、异常捕获、上下文维护以及常用工具类的快捷访问能力。这是 `kd-cd-cosmic-commons` 框架的基础。

> **适用边界**
> ✅ 适用：所有需要继承 Ext 基类的表单/单据/列表/操作插件。
> ❌ 不适用：BOTP 转换插件和反写插件没有 Ext 封装，直接用原生基类。

## 核心基类

### 单据插件
- **`kd.cd.common.plugin.AbstractBillPlugInExt`**: **单据插件首选**。封装了原生 `AbstractBillPlugIn`，内置 `log` 对象，实现 `IBillPluginExtension` 和 `BeforeF7SelectListener`。

### 表单插件
- **`kd.cd.common.plugin.AbstractFormPluginExt`**: 动态表单插件扩展。

### 列表插件
- **`kd.cd.common.plugin.AbstractListPluginExt`**: 列表插件扩展，强化了选中行（SelectedRows）的处理。

### 操作插件
- **`kd.cd.common.plugin.AbstractOperationServicePlugInExt`**: 操作插件扩展，简化了 `onPreparePropertys` 字段准备。

### 校验器
- **`kd.cd.common.plugin.AbstractValidatorExt`**: 校验器扩展。

## 常用 API 方法

### 通用能力 (所有基类)
- `log.info(String msg, Object... args)`: 统一的 SLF4J 日志记录。

### 操作插件特有 (`AbstractOperationServicePlugInExt`)
- `allFields()`: 一键生成包含所有字段的列表。
- `entryFields(String... entryKeys)`: 一键生成包含分录所有子字段的列表。
- `addErrorMessage(DynamicObject dataEntity, String message)`: 快速回填操作错误。
- `addErrorMessage(DynamicObject dataEntity, String title, String message)`: 带标题的错误消息。
- `addErrorMessage(DynamicObject dataEntity, String title, String errCode, String message)`: 完整错误消息。
- `arrayOfIds(DynamicObject[] array)`: 提取主键数组。
- `setOfIds(DynamicObject[] array)`: 提取主键集合。
- `listOf(DynamicObject[] array, String field)`: 提取字段值列表。
- `setOf(DynamicObject[] array, String field)`: 提取字段值集合。

### IFormPluginExtension 接口方法
- `getValue(String key)`: 获取字段值。
- `getValue(String key, int rowIndex)`: 获取分录字段值。
- `getValue(String key, int rowIndex, int parentRowIndex)`: 获取分录分录字段值。
- `getFlatValues(String key)`: 扁平获取字段所有值。
- `getBaseDataQuoteType(String baseDataField)`: 获取基础资料引用类型。
- `getEntryPropKeys(String entryKey)`: 获取分录字段标识集。
- `getEntryProperties(String entryKey)`: 获取分录字段属性映射。
- `getProperties(Predicate<IDataEntityProperty> predicate)`: 筛选字段属性。
- `getProperty(String field)`: 获取字段属性。
- `getEntryType(String entryKey)`: 获取分录类型。
- `getMainEntityType()`: 获取主实体类型。
- `view()`: 获取当前视图。

### 监听器注册方法
- `addEntryRowClickListeners(String... entryKeys)`: 批量注册分录行点击监听。
- `addTabSelectListeners(String... tabKeys)`: 批量注册页签选择监听。
- `addHyperClickListeners(String... keys)`: 批量注册超链接点击监听。
- `addItemClickListeners(String... keys)`: 批量注册菜单项点击监听。
- `addBeforeF7SelectListeners(String... keys)`: 批量注册 F7 弹出前监听。

### 数据更新方法
- `updateEntryView(String entryKey, DynamicObjectCollection rowData)`: 更新分录视图数据。

### 调试方法
- `peek()`: 预览实体结构（调试用）。

## 示例代码

### 标准单据插件模板
```java
package kd.cd.common.demo;

import kd.cd.common.plugin.AbstractBillPlugInExt;
import kd.bos.form.events.AfterDoOperationEventArgs;

public class MyBillPlugin extends AbstractBillPlugInExt {
    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        if ("audit".equals(e.getOperationKey())) {
            log.info("单据 {} 审核成功", getBillNo());
        }
    }
}
```

### 操作插件模板
```java
public class MyOperationPlugin extends AbstractOperationServicePlugInExt {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        // 加载所有字段
        e.setFieldKeys(allFields());

        // 或仅加载特定分录字段
        e.setFieldKeys(entryFields("entry1", "entry2"));
    }

    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        if (!e.getDataEntities()[0].getDataEntityState().isSuccess()) {
            addErrorMessage(e.getDataEntities()[0], "操作失败，请检查数据");
        }
    }
}
```

### 使用扩展方法
```java
public class DemoPlugin extends AbstractBillPlugInExt {
    public void demo() {
        // 获取字段值（支持深路径）
        String orgName = getValue("org.name");

        // 扁平获取分录所有物料ID
        Set<Object> materialIds = getFlatValues("entry.material.id");

        // 获取分录字段信息
        Set<String> entryFields = getEntryPropKeys("entry");

        // 获取基础资料引用类型
        String baseType = getBaseDataQuoteType("material");

        // 调试预览
        Object data = peek();
    }
}
```

## 方法签名快照（生成 @Override 时直接对照）

### AbstractFormPluginExt / AbstractBillPlugInExt 生命周期
```java
public void initialize()
public void registerListener(EventObject e)
public void preOpenForm(PreOpenFormEventArgs e)
public void createNewData(BizDataEventArgs e)
public void afterCreateNewData(EventObject e)
public void loadData(LoadDataEventArgs e)
public void afterLoadData(EventObject e)
public void beforeBindData(EventObject e)
public void afterBindData(EventObject e)
public void afterCopyData(EventObject e)
public void propertyChanged(PropertyChangedArgs e)
public void beforeDoOperation(BeforeDoOperationEventArgs args)
public void afterDoOperation(AfterDoOperationEventArgs args)
public void beforeItemClick(BeforeItemClickEvent evt)
public void itemClick(ItemClickEvent evt)
public void beforeClick(BeforeClickEvent evt)
public void click(EventObject evt)
public void beforeF7Select(BeforeF7SelectEvent e)
public void confirmCallBack(MessageBoxClosedEvent evt)
public void closedCallBack(ClosedCallBackEvent e)
public void clientCallBack(ClientCallBackEvent e)
public void beforeClosed(BeforeClosedEvent e)
public void customEvent(CustomEventArgs e)
public void TimerElapsed(TimerElapsedArgs e)
public void afterAddRow(AfterAddRowEventArgs e)
public void afterDeleteRow(AfterDeleteRowEventArgs e)
public void afterDeleteEntry(AfterDeleteEntryEventArgs e)
public void destory()   // 注意：不是 destroy
```

### AbstractListPluginExt 特有
```java
public void setFilter(SetFilterEventArgs e)
public void afterCreateNewData(EventObject e)
public void beforeDoSelectRow(BeforeDoSelectRowEventArgs e)
```

### AbstractOperationServicePlugInExt 生命周期
```java
public void onPreparePropertys(PreparePropertysEventArgs e)
public void onAddValidators(AddValidatorsEventArgs e)
public void beforeExecuteOperationTransaction(BeforeOperationArgs e)
public void beginOperationTransaction(BeginOperationTransactionArgs e)
public void endOperationTransaction(EndOperationTransactionArgs e)
public void afterExecuteOperationTransaction(AfterOperationArgs e)
public void onReturnOperation(ReturnOperationArgs e)
```

### AbstractValidatorExt
```java
public void validate(ExtendedDataEntity[] dataEntities, ValidateContext validateContext)
```

## 实践建议
1. **继承型插件强制使用 super**: 对继承型插件基类（如 `AbstractFormPluginExt`、`AbstractListPlugin`、`AbstractOperationServicePlugIn` 等），重写生命周期方法时务必先调用 `super.xxx()`，确保基类中的上下文初始化逻辑正常运行；接口型插件（如 `IWorkflowPlugin`）不适用此规则。
2. **包名校验**: 导入时必须认准 `kd.cd.common.plugin`，避免误导至原生的 `kd.bos` 类。
3. **轻量化插件**: 插件层只保留贴近触发点的编排与控制逻辑；复杂计算、可复用业务规则与跨模块数据处理应剥离至服务层（Service）。
4. **使用扩展方法**: 优先使用基类提供的扩展方法（如 `getValue`, `getFlatValues` 等），减少重复代码。

## 常见坑位
1. **序列化问题**: 插件类会被序列化存储在 Session 中，严禁在插件成员变量中持有不可序列化的对象（如 `DataSet`, `InputStream`）。
2. **空指针异常**: 在 `initialize` 阶段 `this.getView()` 可能尚未完全准备好，UI 操作建议推迟到 `registerListener` 之后。
3. **多实例冲突**: 同一个表单打开多个页签时，静态变量（static）会共享，严禁在插件中使用非 final 的静态变量存储状态。
4. **忘记调用 super**: 重写生命周期方法时忘记调用 `super.xxx()`，可能导致基类的初始化逻辑不执行。