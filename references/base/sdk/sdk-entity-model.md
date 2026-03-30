# 动态领域模型 - 实体模型 (Entity Model)

## 概述
实体模型（Entity Model）是苍穹平台对现实世界业务对象的抽象定义。它类似于 Java 中的“类”定义，规定了数据包（DynamicObject）的结构、字段类型、字段标识及字段间的关系（如主子表）。

## 核心类
- **`kd.bos.entity.MainEntityType`**: 所有实体的基类。
- **`kd.bos.entity.BillEntityType`**: 业务单据实体模型。
- **`kd.bos.entity.BasedataEntityType`**: 基础资料实体模型。
- **`kd.bos.entity.EntryType`**: 单据体（分录）实体模型。
- **`kd.bos.entity.property.FieldProp`**: 简单字段属性基类（如文本、数值）。
- **`kd.bos.entity.property.BasedataProp`**: 基础资料字段属性（复杂属性，指向另一个实体）。
- **`kd.bos.entity.property.EntryProp`**: 分录字段属性（集合属性，包含多行子数据）。

## 常用 API 方法
### 获取属性
- `getProperties()`: 获取实体的所有属性集合。
- `getProperty(String name)`: 根据字段标识获取属性对象。
- `getPrimaryKey()`: 获取主键属性。

### 属性特征访问
- `prop.getName()`: 获取字段标识（Key）。
- `prop.getAlias()`: 获取数据库字段名（Alias）。
- `prop.getDataType()`: 获取字段的物理数据类型。

## 示例代码
### 1. 遍历实体字段元数据
```java
MainEntityType entityType = MetadataServiceHelper.getDataEntityType("my_entity_id");
// 获取所有字段并打印其显示名称
for (IDataEntityProperty prop : entityType.getProperties()) {
    String displayName = prop.getDisplayName().getLocaleValue();
    String key = prop.getName();
    System.out.println(key + " : " + displayName);
}
```

### 2. 动态判断字段类型
```java
IDataEntityProperty prop = entityType.getProperty("customer");
if (prop instanceof BasedataProp) {
    String baseEntityId = ((BasedataProp) prop).getBaseEntityId();
    // 该字段是一个指向 baseEntityId 的基础资料
}
```

## 实践建议
1. **标识驱动**：在代码中访问数据时，务必使用 `prop.getName()`（标识），严禁硬编码 `prop.getAlias()`（数据库物理名），以保证模型的解耦。
2. **元数据缓存**：虽然平台有二级缓存，但在高频循环中建议将 `MainEntityType` 对象提取到循环外。
3. **区分分录**：通过 `prop instanceof EntryProp` 快速识别单据体字段，并递归处理子实体的元数据。

## 常见坑位
1. **同名冲突**：在主表和分录中可能存在标识相同的字段，获取属性时注意 `entityType` 所在的层级。
2. **多语言取值**：`prop.getDisplayName()` 返回的是多语言对象，必须调用 `getLocaleValue()` 才能获取当前语种的字符串。
3. **主键识别**：部分虚实体可能没有物理主键，调用 `getPrimaryKey()` 可能返回 null。
