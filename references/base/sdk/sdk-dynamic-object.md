# 动态领域模型 - 数据包 (DynamicObject)

## 概述
`DynamicObject` 是苍穹平台的核心数据载体，它是实体模型（EntityType）的运行时实例。它采用多层嵌套的数组结构存储数据，能够灵活地表达主子表、基础资料引用等复杂的业务结构。

## 核心类
- **`kd.bos.dataentity.entity.DynamicObject`**: 单条数据包对象。
- **`kd.bos.dataentity.entity.DynamicObjectCollection`**: 数据包集合（常用于单据体分录）。

## 常用 API 方法
### 获取值 (Getter)
- `get(String key)`: 通用取值，返回 `Object`。
- `getString(String key)` / `getLong(String key)` / `getBigDecimal(String key)`: 类型化取值。
- `getDynamicObject(String key)`: 获取关联的基础资料或 1:1 属性。
- `getDynamicObjectCollection(String key)`: 获取单据体分录集合。

### 设置值 (Setter)
- `set(String key, Object value)`: 设置字段值。

### 其他
- `getDataEntityType()`: 获取该数据包对应的元数据模型。
- `getPkValue()`: 获取该对象的主键值。

## 示例代码
### 1. 存取简单字段与分录
```java
// 获取主表字段
String billNo = bill.getString("billno");
BigDecimal totalAmt = bill.getBigDecimal("totalamount");

// 遍历分录
DynamicObjectCollection entryRows = bill.getDynamicObjectCollection("entryentity");
for (DynamicObject row : entryRows) {
    BigDecimal qty = row.getBigDecimal("qty");
    // 修改分录字段
    row.set("amt", qty.multiply(new BigDecimal("10")));
}
```

### 2. 处理基础资料字段 (重要)
基础资料在 `DynamicObject` 中存有两个 Key：`xxx` (对象) 和 `xxx_id` (内码)。
```java
// 获取客户内码
long customerId = bill.getLong("customer_id");
// 获取客户对象并读取其编码
DynamicObject customer = bill.getDynamicObject("customer");
if (customer != null) {
    String code = customer.getString("number");
}

// 设置基础资料（必须传入 DynamicObject 对象，严禁直接给 customer 键设 ID）
DynamicObject newCust = BusinessDataServiceHelper.loadSingleFromCache(123456L, "bd_customer");
bill.set("customer", newCust); 
```

## 实践建议
1. **优先使用内码**：如果只需要 ID 进行过滤或关联，优先读取 `xxx_id` 键，避免触发不必要的对象加载。
2. **安全取值**：从 `getDynamicObject` 返回的对象可能为 `null`，访问前务必判空。
3. **性能注意**：`DynamicObject` 是纯内存对象，大批量处理时注意及时清理引用，或改用 `DataSet`。

## 常见坑位
1. **Key 拼写错误**：字段标识必须与表单设计器中的“标识”完全一致，大小写敏感。
2. **直接修改 Entity 集合**：对 `DynamicObjectCollection` 执行 `add` 或 `remove` 后，若在插件环境，必须确保 UI 能感知变化。
3. **基础资料设值误区**：执行 `bill.set("customer", 123456L)` 是错误的，这会导致后续代码通过 `getDynamicObject("customer")` 报错。必须先 load 出对象再 set。
