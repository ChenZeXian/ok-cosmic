# 动态对象处理全览 (DynamicObject & DynamicObjectUtils)

## 概述
`DynamicObject` 是苍穹数据的核心载体（内存中的数据包）。`DynamicObjectUtils` (位于 `kd.cd.common.util`) 提供了对这些对象及其集合的高效、安全操作。由于原生 API 容易引发 `NullPointerException` 或类型转换异常，推荐一律使用工具类进行数据存取。

> **适用边界**
> ✅ 适用：DynamicObject 安全取值/批量提取/集合操作。
> ❌ 不适用：元数据结构解析请用 `entity-metadata.md`；查询构建请用 `query-dataset.md`。

## 核心类
- **`kd.bos.dataentity.entity.DynamicObject`**: 基础数据载体（单对象）。
- **`kd.bos.dataentity.entity.DynamicObjectCollection`**: 动态对象集合（通常用于表示单据的分录）。
- **`kd.cd.common.util.DynamicObjectUtils`**: **核心工具类**。

## 常用 API 方法

### 1. 安全取值与设置 (单对象)
- `safeGetValue(DynamicObject dyn, String propKey)`: **最常用**。安全获取字段值，字段不存在时返回 null。
- `safeSetValue(DynamicObject dyn, String propKey, Object value)`: 安全设置值，字段不存在时不抛异常。
- `nullSafeGet(DynamicObject dyn, String field)`: 空安全获取字段值。
- `getPkValue(DynamicObject dyn)`: 获取主键。
- `containsKey(DynamicObject dyn, String key)`: 判断是否包含某字段属性。
- `containsKey(DynamicObjectCollection coll, String key)`: 判断集合是否包含某字段。

### 2. 扁平提取 (深层路径)
- `flatSetOf(DynamicObject dyn, String expr)`: **深路径提取**。扁平列出动态对象中某字段的所有值，收集为 Set。示例：`"entry.subentry.field"`。
- `flatListOf(DynamicObject dyn, String expr)`: 同上，返回 List。

### 3. 批量属性提取 (集合/分录)
- `arrayOfIds(DynamicObject[] array)`: 快速提取数组内所有对象的主键 ID。
- `arrayOfIds(Collection<DynamicObject> coll)`: 快速提取集合内所有对象的主键 ID。
- `setOfIds(DynamicObject[] array)`: 提取主键 ID，返回 Set。
- `setOfIds(Collection<DynamicObject> coll)`: 提取主键 ID，返回 Set。
- `setOf(Collection<DynamicObject> coll, String field)`: 提取并去重某一属性。
- `listOf(Collection<DynamicObject> coll, String field)`: 提取属性，返回列表。
- `sumOf(Collection<DynamicObject> coll, String decimalField)`: 对数值字段执行内存汇总。

### 4. 数据集转换与序列化
- `toDataSet(DynamicObjectCollection coll)`: 集合转数据集（DataSet）以执行高性能计算。
- `toDataSet(DynamicObjectCollection coll, String... selectFields)`: 选择字段转换。
- `fromDataSet(DataSet ds)`: 将计算结果转换回对象集合。
- `serialize(DynamicObject... dynArr)`: 序列化动态对象。
- `deSerialize(String serialized, DynamicObjectType dt)`: 反序列化动态对象。

### 5. 对象创建与克隆
- `clone(DynamicObject dyn)`: 深度克隆数据对象，默认清除主键值。
- `newDynamicObject(String formId)`: 根据表单标识生成新的动态对象。
- `newDynamicObject(DynamicObjectType dt)`: 根据类型生成新的动态对象。

### 6. 属性信息
- `getPropKeys(DynamicObject dyn)`: 获取属性标识集。
- `getPropKeys(DynamicObjectCollection coll)`: 获取集合属性标识集。
- `dump(DynamicObject dyn)`: 转储 DynamicObject 为 Map。

### 7. 状态判断
- `isNewCreate(DynamicObject dyn)`: 判断数据包是否为新增（不来源于数据库）。

## 示例代码

### 安全链式取值与分录统计
```java
package kd.cd.common.demo;

import kd.cd.common.util.DynamicObjectUtils;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import java.math.BigDecimal;

public class DataDemo {
    public void process(DynamicObject bill) {
        // 1. 安全获取基础资料名称，无视空指针风险
        String orgName = DynamicObjectUtils.nullSafeGet(bill, "org.name");

        // 2. 统计分录中所有行特定金额之和
        DynamicObjectCollection entry = bill.getDynamicObjectCollection("billentry");
        BigDecimal totalAmount = DynamicObjectUtils.sumOf(entry, "amount");

        // 3. 提取分录中所有物料ID
        Set<Object> materialIds = DynamicObjectUtils.setOf(entry, "material.id");

        // 4. 深路径扁平提取
        Set<Object> allSubValues = DynamicObjectUtils.flatSetOf(bill, "entry.subentry.field");
    }
}
```

### 判断单据是否新增
```java
public void checkBillStatus(DynamicObject bill) {
    if (DynamicObjectUtils.isNewCreate(bill)) {
        // 新增单据，尚未保存到数据库
    } else {
        // 已存在的单据
    }
}
```

## 实践建议
1. **优先使用 Path 取值**: 在处理多级关联（如单据->物料->规格型号）时，使用 `flatSetOf("material.model")` 而不是逐层 `.get(...)`。
2. **批量查询原则**: 严禁在分录循环中查询数据库。应先提取分录中所有 ID（使用 `setOfIds`），执行一次批量查询后，再进行内存匹配。
3. **内存字段管理**: 动态添加的字段在使用 `toDataSet` 前需确保已注册在元数据中。
4. **序列化场景**: `serialize`/`deSerialize` 适用于跨进程传递或缓存存储。

## 常见坑位
1. **类型强转错误**: 从 `safeGetValue` 获取的值必须根据元数据定义的字段类型进行强转（如 `Long`, `BigDecimal`, `Date`），严禁凭经验判断。
2. **脏数据未提交**: 手动修改 `DynamicObject` 的值后，如果不希望触发操作插件的变更逻辑，需通过 `clearDirty` 控制。
3. **集合引用失效**: 对 `DynamicObjectCollection` 执行 `clear()` 后，之前获取的引用将变为空，操作前需确认状态。
4. **克隆后主键**: `clone` 方法默认清除主键值，如需保留主键请手动设置。