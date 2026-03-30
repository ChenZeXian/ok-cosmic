# 实体元数据解析工具 (EntityUtils)

## 概述
元数据（Metadata）驱动是苍穹的核心设计。`EntityUtils` (位于 `kd.cd.common.entity`) 对原生的实体元数据进行了深层封装，提供了获取实体标识、属性解析、下拉列表（Combo）以及分录结构的极简 API。旨在减少元数据读取时的深路径嵌套。

> **适用边界**
> ✅ 适用：获取实体标识/属性解析/下拉枚举/分录结构。
> ❌ 不适用：DynamicObject 数据操作请用 `dynamic-object.md`；弹性域字段解析请用 `flex-prop.md`。

## 核心类
- **`kd.cd.common.entity.EntityUtils`**: **元数据解析核心工具类**。
- **`kd.bos.entity.MainEntityType`**: 苍穹单据主实体元数据。
- **`kd.bos.dataentity.metadata.IDataEntityProperty`**: 实体属性元数据。

## 常用 API 方法

### 1. 实体标识与关键标识
- `getEntityId(String formId)`: **最常用**。根据表单标识获取其关联的数据实体名。
- `getPrimaryKey(String formId)`: 获取实体的 ID 字段 Key（通常为 "id"）。
- `getBillNoKey(String formId)`: 获取实体的单据编号字段 Key（如 "billno"）。
- `getBillStatusKey(String formId)`: 获取实体的单据状态字段 Key（如 "billstatus"）。
- `getPkAndBillnoKey(String formId)`: 获取主键与单据编码字段标识（Pair）。

### 2. 属性解析与检索
- `getProperty(String formId, String field)`: 获取特定属性的元数据定义（单据头&单据体）。
- `getProperty(MainEntityType mainType, String field)`: 从主实体类型获取属性。
- `selectProperties(String formId, Predicate<IDataEntityProperty> predicate)`: 根据字段属性筛选字段。
- `getAllProperties(String formId)`: 获取表单实体中所有字段属性映射。
- `getEntryPropKeys(String formId, String entryKey)`: 获取分录包含的所有子字段 Key。
- `getEntryProperties(String formId, String entryKey)`: 获取分录字段属性映射。
- `getPropKeyWithPrefix(String formId, String field)`: 获取带父级前缀的字段标识（如 entry.subentry.field）。

### 3. 下拉与枚举
- `getComboItemMap(String formId, String comboKey)`: 获取下拉列表的所有枚举值（值 -> 名称）。

### 4. 类型与结构
- `getMainEntityType(String formId)`: 获取主实体类型（兼容PC布局）。
- `getDynamicObjectType(String formId)`: 获取动态对象类型。
- `getDynamicObjectType(String formId, String selectProperties)`: 获取仅含部分字段属性的动态对象类型。
- `getEntryType(String formId, String entryKey)`: 获取分录实体类型。
- `getTableDefine(String formId)`: 获取表单实体表定义。

### 5. 实体信息
- `getChsName(String formId)`: 获取表单实体中文描述名。
- `getAppId(String formId)`: 获取 AppId。
- `getDBRoute(String formId)`: 获取实体数据库路由。
- `getBaseDataQuoteType(String formId, String baseDataField)`: 获取基础资料引用类型。
- `getAliasState(String formId, String field)`: 获取字段数据库字段状态。
- `getLinkEntryRelation(String formId)`: 获取表单实体关联关系映射字段信息。

### 6. 数据校验工具
- `isEmptyPk(Object pkValue)`: **最常用**。安全判定单据主键是否为空（兼容 0L 或空字符串）。
- `isNotEmptyPk(Object pkValue)`: 判定主键非空。
- `checkNotEmptyPk(Object pkValue, String expr)`: 校验主键值非空，为空时抛出异常。

### 7. 扁平提取（调试用）
- `flatListValue(DynamicObject dataEntity, String field)`: 扁平列出主实体中某字段的所有值。
- `peek(Object o)`: 预览实体结构（仅调试）。

## 示例代码

### 动态获取单据显示名
```java
package kd.cd.common.demo;

import kd.cd.common.entity.EntityUtils;

public class MetaDemo {
    public void getInfo(String formId) {
        // 1. 获取实体标识
        String entityId = EntityUtils.getEntityId(formId);

        // 2. 获取主键和编号对应的 Key
        String pkKey = EntityUtils.getPrimaryKey(formId);
        String noKey = EntityUtils.getBillNoKey(formId);

        // 3. 获取下拉列表的中文字符
        Map<String, String> items = EntityUtils.getComboItemMap(formId, "billstatus");

        // 4. 获取实体中文名
        String chsName = EntityUtils.getChsName(formId);

        // 5. 校验主键非空
        if (EntityUtils.isEmptyPk(pkValue)) {
            // 单据尚未保存
        }
    }
}
```

### 获取分录字段信息
```java
public void analyzeEntry(String formId, String entryKey) {
    // 获取分录所有字段标识
    Set<String> fieldKeys = EntityUtils.getEntryPropKeys(formId, entryKey);

    // 获取分录字段属性映射
    Map<String, IDataEntityProperty> properties = EntityUtils.getEntryProperties(formId, entryKey);

    // 获取带前缀的字段标识
    String fullKey = EntityUtils.getPropKeyWithPrefix(formId, "entry.field");
    // 结果: "entry.field" 或 "entity.entry.field"
}
```

## 实践建议
1. **优先使用 getBillNoKey**: 在通用插件中获取单据编号，不要写死 "billno"，以应对客户在设计器中对字段标识的调整。
2. **分录字段前缀**: 拼接分录字段名时，建议使用 `getPropKeyWithPrefix` 自动补全前缀。
3. **缓存重用**: 对于元数据的频繁读取，建议在方法内部缓存 `MainEntityType` 的引用。
4. **主键判空**: 使用 `isEmptyPk` 而非 `== null`，因为苍穹主键默认为 0L。

## 常见坑位
1. **基础资料扩展失效**: 对于动态扩展的字段，如果扩展未发布，`getProperty` 会返回 null。
2. **区分 FormId 与 EntityId**: 表单标识（UI）和实体标识（DB）有时不同（如基础资料扩展），调用时务必核实参数。
3. **主键默认值**: 苍穹主键默认为 0L，使用 `isEmptyPk` 可以精准识别单据是否已入库。
4. **属性对象共享**: `getProperty` 返回的对象是全局缓存共用的，禁止对其进行 set 操作。