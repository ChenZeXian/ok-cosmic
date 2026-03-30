# 弹性域自动解析工具 (FlexPropUtils)

## 概述
弹性域是苍穹平台提供的高度扩展字段机制。物理上它们以扁平字段（如 `flex_field1`）存储。`FlexPropUtils` (位于 `kd.cd.common.util`) 封装了复杂的映射关系，支持将弹性域 ID 批量解析为结构化的 `Map` 键值对，大幅减少代码量并提高可读性。

> **适用边界**
> ✅ 适用：弹性域 ID 解析为结构化键值对。
> ❌ 不适用：普通实体字段读取请用 `entity-metadata.md`；基础资料引用请用 `BaseDataServiceHelper`。

## 核心类
- **`kd.cd.common.util.FlexPropUtils`**: **弹性域处理核心工具类**。
- **`kd.cd.common.util.FlexType`**: 弹性域类型枚举。

## FlexType 枚举
弹性域类型决定了字段映射关系的来源：
- **`AUX`**: 辅助属性
- **其他类型**: 根据业务需求扩展

## 常用 API 方法

### 1. 弹性域自动解析
- `parseSingle(DynamicObject flexEntity, FlexType flexType)`: 解析单个弹性域实体，返回值映射。
- `parse(Collection<DynamicObject> flexEntities, FlexType flexType)`: 批量解析对象集合，返回按主键分组的值映射。
- `parseByFlexIds(Collection<Object> flexIds, FlexType flexType)`: 直接根据弹性域的主键 ID 进行批量解析，返回按主键分组的值映射。

### 2. 查询方法
- `queryJsonByFlexIds(Collection<Object> flexIds, FlexType flexType)`: 批量查询并返回弹性域主键映射其 JSON 字符串。

### 3. 配置加载
- `loadFieldRelationAsMap(FlexType flexType)`: 加载弹性域标识与物理字段名的映射关系。

## 示例代码

### 解析单据中的辅助属性弹性域
```java
package kd.cd.common.demo;

import kd.cd.common.util.FlexPropUtils;
import kd.cd.common.util.FlexType;
import kd.bos.dataentity.entity.DynamicObject;
import java.util.Map;
import java.util.Collection;

public class FlexDemo {
    public void demo(DynamicObject bill) {
        // 1. 获取弹性域实体
        DynamicObject flexEntity = bill.getDynamicObject("flex_field");

        // 2. 解析辅助属性 (AUX) 类型的弹性域
        Map<String, Object> values = FlexPropUtils.parseSingle(flexEntity, FlexType.AUX);

        // 3. 根据字段 Key 获取解析后的业务值
        Object color = values.get("flex_color");
        Object size = values.get("flex_size");
    }
}
```

### 批量解析弹性域
```java
public void batchDemo(Collection<Object> flexIds) {
    // 根据弹性域主键批量解析
    Map<Object, Map<String, Object>> result = FlexPropUtils.parseByFlexIds(flexIds, FlexType.AUX);

    // 遍历结果
    for (Map.Entry<Object, Map<String, Object>> entry : result.entrySet()) {
        Object flexId = entry.getKey();
        Map<String, Object> fieldValues = entry.getValue();

        // 获取具体字段值
        Object color = fieldValues.get("flex_color");
    }
}
```

### 从分录中提取弹性域值
```java
public void extractFromEntry(DynamicObject bill) {
    DynamicObjectCollection entries = bill.getDynamicObjectCollection("entry");

    // 收集所有弹性域ID
    Set<Object> flexIds = DynamicObjectUtils.setOf(entries, "flex_field.id");

    // 批量解析
    Map<Object, Map<String, Object>> flexValues = FlexPropUtils.parseByFlexIds(flexIds, FlexType.AUX);

    // 匹配分录数据
    for (DynamicObject entry : entries) {
        Object flexId = entry.get("flex_field.id");
        Map<String, Object> values = flexValues.get(flexId);
        if (values != null) {
            // 使用解析后的值
        }
    }
}
```

## 实践建议
1. **优先使用解析工具**: 严禁直接通过 `get("flex_field1")` 这种方式硬编码逻辑。使用 `FlexPropUtils` 后代码具有更好的业务可读性。
2. **区分 FlexType**: 目前常见的弹性域类型包括辅助属性（AUX）、物料特性等，调用前必须确认所属类型。
3. **批量处理性能**: 在分录处理逻辑中，建议收集所有弹性域 ID 后统一调用 `parseByFlexIds` 一次性完成转换。
4. **空值判断**: 如果弹性域 ID 为空，解析结果通常会返回一个空的 Map。

## 常见坑位
1. **缓存更新延迟**: 如果在设计器中新发布了弹性域，后台工具类可能需要 10 分钟缓存 TTL 刷新后才能读取到新字段，生产环境可能需要刷新元数据缓存。
2. **空值判断**: 如果弹性域 ID 为空，解析结果通常会返回一个空的 Map，需要进行空判断。
3. **字段重复**: 不同类型的弹性域可能包含相同标识的字段，调用时必须确认 `FlexType`。
4. **返回类型变化**: `parseSingle` 方法实际是从 `parse` 返回的 Map 中提取对应主键的值，注意返回类型是 `Map<String, Object>`。