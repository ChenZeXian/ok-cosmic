# 标准单据列表插件

## 概述
单据列表插件用于控制列表的加载、显示、取数、过滤、行交互等全生命周期。列表支持标准的单据/基础资料列表、左树右表列表等多种布局。

> **适用边界**
> ✅ 本文档是原生兜底：当 `plugin-base.md`(封装层) 未覆盖你需要的列表事件时使用。
> ❌ 如果封装层 `AbstractListPluginExt` 已满足需求，优先读 `references/adv/plugin-base.md`。

- 适用场景：列表过滤定制、列表数据处理、行交互、超链接、单据打开回调

术语说明：文档中的 `F7` 指过滤容器或字段上的基础资料/引用数据选择控件弹窗，不是键盘按键事件。

## 核心基类


- 基类：`kd.bos.list.plugin.AbstractListPlugin`
- 继承关系：`AbstractListPlugin extends AbstractFormPlugin implements ListRowClickListener, IPCListPlugin`

## 额外监听器

- `IRegisterPropertyListener`：过滤/依赖字段事件
- `BeforeF7SelectListener`：F7（基础资料/引用数据选择控件）选择前拦截（过滤容器场景）

## 核心事件

- `filterContainerInit`：// 过滤容器初始化，可添加/修改过滤字段定义
- `beforeCreateListColumns`：// 创建列定义前，可添加/修改列信息
- `beforeCreateListDataProvider`：// 创建取数器前，可自定义取数逻辑
- `setFilter`：// 查询前调整过滤条件
- `filterContainerSearchClick`：// 用户点击查询或修改快捷过滤时触发
- `filterContainerAfterSearchClick`：// 过滤条件解析完毕后联动处理
- `filterContainerBeforeF7Select`：// 过滤容器 F7（基础资料/引用数据选择控件）弹出前拦截
- `filterColumnSetFilter`：// 基础资料过滤字段条件调整
- `baseDataColumnDependFieldSet`：// 设置常用过滤依赖字段
- `beforeItemClick`：// 菜单按钮点击前触发
- `itemClick`：// 菜单按钮点击时触发
- `billListHyperLinkClick`：// 点击超链接单元格时触发
- `beforeShowBill`：// 打开单据维护界面前触发
- `billClosedCallBack`：// 单据维护界面关闭返回时触发
- `listRowClick`：// 列表行点击时触发
- `listRowDoubleClick`：// 列表行双击时触发
- `setCellFieldValue`：// 设置单元格指令时触发
- `setPluginName`：// 界面显示前触发
- `preOpenForm`：// 界面打开前触发
- `loadCustomControlMetas`：// 自定义控件元数据加载时触发
- `setView`：// 视图注入时触发
- `initialize`：// 初始化时触发
- `registerListener`：// 注册监听时触发
- `beforeBindData`：// 绑定前触发
- `afterBindData`：// 绑定后触发

## 插件内上下文方法

### 过滤容器操作
```java
// filterContainerInit 中添加自定义过滤字段
FilterColumn filterColumn = new FilterColumn("datefield");
filterColumn.setDefaultValues("2019-1-30", "2019-1-31");
args.addFilterColumn(filterColumn);

// filterContainerSearchClick 中获取过滤值
String filterValue = args.getFilterValue("datefield");
Map<String, Object> fastFilters = args.getFastFilterValues();

// 动态添加过滤条件
QFilter qfilter = new QFilter("datefield", CompareTypeEnum.EQUAL, "2020-01-01");
args.addFilter("datefield", qfilter);

// 添加快速过滤
args.addFastFilter("textfield", "searchValue");

// 获取过滤后的 QFilter
QFilter filter = args.getQFilter("datefield");

// 获取选中的主组织
Set<String> mainOrgIds = args.getSelectMainOrgIds();
```

### 列定义操作
```java
// beforeCreateListColumns 中修改列定义
ComboListColumn comboColumn = new ComboListColumn();
comboColumn.setListFieldKey("combofield");

MergeListColumn mergeColumn = new MergeListColumn();
mergeColumn.setKey("mergecolumn");
mergeColumn.getItems().add(comboColumn);

beforecreatelistcolumnsargs.addListColumn(mergeColumn);
```

### 行点击操作
```java
// listRowClick 中获取行数据
@Override
public void listRowClick(RowClickEventArgs e) {
    DynamicObject dataEntity = e.getDataEntity();
    String billNo = (String) dataEntity.get("billno");
    System.out.println("点击行：" + billNo);
}

// listRowDoubleClick 中处理双击
@Override
public void listRowDoubleClick(RowClickEventArgs e) {
    DynamicObject dataEntity = e.getDataEntity();
    String billId = (String) dataEntity.get("id");
    // 打开单据或其他操作
}
```

## 示例代码

示例代码统一维护在模板文件中，直接参考：

- [ListPluginTemplate.java](../../../assets/ListPluginTemplate.java)

## 实践建议

1. **过滤定制优先用 filterContainerInit + filterContainerAfterSearchClick**
   - 在 init 中定义字段
   - 在 afterSearchClick 中处理联动

2. **避免在逐行处理链路中查库**
   - 大数据量场景性能问题
   - 应在 beforeCreateListDataProvider 或 setFilter 中提前准备数据

3. **基础资料引用字段不要在 beforeCreateListDataProvider 里修改**
   - 会破坏内置缓存
   - 只适合修改非基础资料字段

4. **行点击与菜单项点击分离**
   - 行点击处理用 listRowClick
   - 菜单按钮用 itemClick
   - 避免混淆

5. **单据打开回调用 billClosedCallBack**
   - 不要在打开前后分散写逻辑
   - 统一由 billClosedCallBack 处理返回

## 常见坑位

### ❌ 在 beforeCreateListDataProvider 修改基础资料引用属性
```java
// 错误：破坏缓存
@Override
public void beforeCreateListDataProvider(BeforeCreateListDataProviderArgs args) {
    // 不要修改基础资料字段（如 orgname、deptname）
    args.getQueryStatement().getSelectItems().add("org.name");  // 错误！
}

// 正确：只修改非基础资料字段
args.getQueryStatement().getSelectItems().add("customfield");
```

### ❌ beforeItemClick 与 itemClick 逻辑混用
```java
// 错误：重复执行
@Override
public void beforeItemClick(ItemClickEvent e) {
    // 在此修改单据状态  
}

@Override
public void itemClick(ItemClickEvent e) {
    // 在此又修改了同样的状态  → 重复！
}

// 正确：职责分离
beforeItemClick → 检查权限、校验
itemClick → 执行操作
```

### ❌ 逐行处理链路中循环查库
```java
// 错误：性能灾难
for (每一行) {
    queryDatabase();  // ❌ 10000 行数据 = 10000 次查询
}

// 正确：提前准备数据
@Override
public void beforeCreateListDataProvider(...) {
    // 一次查询获取所有需要的数据
    Map<String, Object> dataCache = queryAllDataOnce();
    // 后续逐行逻辑直接从缓存取值
}
```

### ❌ filterContainerSearchClick 中添加过滤后忘记调用 super
```java
// 错误
@Override
public void filterContainerSearchClick(FilterContainerSearchClickArgs args) {
    args.addFilter(...);
    // 忘记调用 super.filterContainerSearchClick(args);
}

// 正确
@Override
public void filterContainerSearchClick(FilterContainerSearchClickArgs args) {
    args.addFilter(...);
    super.filterContainerSearchClick(args);  // 必须调用
}
```

### ❌ listRowClick 中修改列表数据
- 行点击时不要直接修改数据源
- 应该打开编辑界面或触发操作，而非在列表里直接改

### ❌ 超链接点击不处理异常
```java
@Override
public void billListHyperLinkClick(HyperLinkClickEvent e) {
    try {
        // 跳转或打开
    } catch (Exception ex) {
        // 必须捕获，否则列表可能崩溃
    }
}
```