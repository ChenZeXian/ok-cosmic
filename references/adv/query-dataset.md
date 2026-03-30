# 高性能查询与计算 (QueryUtils & AlgoUtils)

## 概述
数据查询与处理是苍穹性能优化的第一大战场。`QueryUtils` (位于 `kd.cd.common.util`) 封装了 ORM 与 SQL 查询，极大简化了单条或批量数据的检索逻辑；`AlgoUtils` 针对苍穹特有的 `DataSet` 提供了类 Stream 的内存计算 API，是处理大数据量业务逻辑的首选。

> **适用边界**
> ✅ 适用：单条/批量数据查询、DataSet 内存计算与统计。
> ❌ 不适用：基础资料查询优先用 `BusinessDataServiceHelper.loadFromCache`；实体查询无法表达的场景才考虑裸 SQL。

## 核心类
- **`kd.cd.common.util.QueryUtils`**: **查询核心**。负责从数据库（ORM 或原生 SQL）获取数据。
- **`kd.cd.common.util.AlgoUtils`**: **计算核心**。负责对 `DataSet` 进行过滤、聚合与转换。
- **`kd.bos.algo.DataSet`**: 苍穹高性能数据集对象。

## QueryUtils API 方法

### 1. 单值查询
- `querySingle(String entityId, String field, QFilter... filters)`: 查询一条记录某字段值。
- `querySingleByPk(String entityId, Object pkValue, String field)`: 根据主键获取指定字段数据。
- `queryPkByNumber(String entityId, String number)`: 根据编码查询主键值。

### 2. 集合查询
- `queryAsList(String entityId, String field, QFilter... filters)`: 查询字段值并收集为 List。
- `queryAsSet(String entityId, String field, QFilter... filters)`: 查询字段值并收集为 Set。
- `queryAsMap(String entityId, String keyField, String valueField, QFilter... filters)`: 查询两字段并收集为 Map。
- `queryMatchedPk(String entityId, QFilter... filters)`: 查询满足条件的数据主键数组。
- `queryMatchedPkList(String entityId, QFilter... filters)`: 查询满足条件的数据主键 List。
- `queryMatchedPkSet(String entityId, QFilter... filters)`: 查询满足条件的数据主键 Set。

### 3. 数据集查询
- `queryDataSet(String entityId, String selectFields, QFilter... filters)`: **最常用**。执行实体查询并返回数据集。
- `queryDataSet(String entityId, String selectFields, String orderBys, QFilter... filters)`: 带排序的查询。
- `queryDataSet(String entityId, String selectFields, String orderBys, int top, QFilter... filters)`: 带排序和条数限制。
- `queryDataSet(DBRoute dbRoute, String sql)`: 原生 SQL 查询。
- `queryDataSet(DBRoute dbRoute, String sql, Object[] params)`: 带参数的原生 SQL 查询。
- `queryDataSet(DBRoute dbRoute, SqlBuilder sb)`: 使用 SqlBuilder 的查询。

## AlgoUtils API 方法

### 1. 流处理
- `stream(DataSet dataSet)`: 提供对 DataSet 的 Stream 流支持。

### 2. 过滤与转换
- `filter(DataSet dataSet, Predicate<Row> filter)`: 对数据集执行高效过滤。
- `nullToZero(DataSet dataSet, String... fields)`: 将为 null 的字段值更新为 0。

### 3. 聚合计算
- `sumOf(DataSet dataSet, String field)`: 对数据集进行内存求和（返回 BigDecimal）。
- `listOf(DataSet dataSet, String field)`: 将数据集的一列提取为 List。
- `listOf(DataSet dataSet, Function<Row, T> function)`: 自定义函数提取为 List。
- `setOf(DataSet dataSet, String field)`: 将数据集的一列提取为 Set。
- `setOf(DataSet dataSet, Function<Row, T> function)`: 自定义函数提取为 Set。

### 4. 信息获取
- `fieldsOf(DataSet dataSet)`: 获取 DataSet 中字段数组。
- `sizeOf(DataSet dataSet)`: 获取 DataSet 大小。
- `dump(DataSet dataSet, String... fields)`: 转储 DataSet，按字段分组为 List。

### 5. 数据集创建
- `newDataSet(DynamicObjectCollection coll)`: 从 DynamicObjectCollection 创建 DataSet。
- `newDataSet(Map<String, DataType> metaMap, List<Object[]> seqRows)`: 根据类型映射创建。
- `newDataSet(String[] fields, DataType[] dataTypes, List<Object[]> seqRows)`: 根据字段定义创建。
- `newDataSet(RowMeta rowMeta, List<Object[]> seqRows)`: 根据 RowMeta 创建。
- `emptyDataSet(Map<String, DataType> metaMap, int initialSize)`: 生成空行 DataSet。
- `appendNullRow(DataSet dataSet, int size)`: 为 DataSet 追加空行。

### 6. 行元数据操作
- `rowAddField(Row row, String field, DataType dataType, Object value)`: Row 对象添加新字段并赋值。
- `rowMetaAddField(RowMeta rowMeta, String field, DataType dataType)`: RowMeta 添加新字段。
- `dumpRowMeta(RowMeta rowMeta)`: RowMeta 转换为类型映射。

### 7. 调试输出
- `print(DataSet dataSet)`: 等距对齐打印 DataSet。
- `print(DataSet dataSet, int top)`: 打印前 N 条。
- `print(DataSet dataSet, int top, boolean withJavaType)`: 带类型打印。

## 示例代码

### DataSet 组合查询与计算
```java
package kd.cd.common.demo;

import kd.cd.common.util.QueryUtils;
import kd.cd.common.util.AlgoUtils;
import kd.bos.algo.DataSet;
import kd.bos.orm.query.QFilter;
import java.math.BigDecimal;

public class QueryDemo {
    public void execute() {
        // 1. 查询已审核单据及其金额
        QFilter filter = new QFilter("status", "=", "C");
        try (DataSet ds = QueryUtils.queryDataSet("my_bill", "id, totalamount", filter.toArray())) {
            // 2. 内存过滤金额 > 1000 的数据
            DataSet filtered = AlgoUtils.filter(ds, row ->
                row.getBigDecimal("totalamount").compareTo(BigDecimal.valueOf(1000)) > 0);

            // 3. 计算汇总
            BigDecimal sum = AlgoUtils.sumOf(filtered, "totalamount");

            // 4. 提取ID列表
            List<Object> ids = AlgoUtils.listOf(filtered, "id");
        } // DataSet 必须确保关闭
    }
}
```

### 快速查询示例
```java
public void quickQuery() {
    // 根据编码查主键
    Object pk = QueryUtils.queryPkByNumber("my_bill", "BILL001");

    // 根据主键查字段
    String name = QueryUtils.querySingleByPk("my_bill", pk, "name");
    QFilter filter = new QFilter("status", "=", "C");

    // 查询字段值集合
    Set<String> names = QueryUtils.queryAsSet("my_bill", "name",
        filter.toArray());

    // 查询字段映射
    Map<Object, String> pkNameMap = QueryUtils.queryAsMap("my_bill", "id", "name",
        filter.toArray());
}
```

### 原生 SQL 查询
```java
public void sqlQuery(DBRoute dbRoute) {
    String sql = "SELECT id, name FROM my_table WHERE status = ?";
    try (DataSet ds = QueryUtils.queryDataSet(dbRoute, sql, new Object[]{"C"})) {
        // 处理结果
        List<Object> ids = AlgoUtils.listOf(ds, "id");
    }
}
```

## 实践建议
1. **优先使用 DataSet**: 对于处理多单据关联、分录数据计算等场景，`DataSet` 性能远超 `List<DynamicObject>`。
2. **延迟加载**: 在 `queryDataSet` 时，仅传入必要的 `selectFields`，严禁使用 `*`。
3. **QFilter 优化**: 尽量在查询阶段完成数据过滤，减少传输到内存中的数据量。
4. **流关闭**: 使用 `try-with-resources` 确保 DataSet 被正确关闭。

## 常见坑位
1. **泄露隐患**: `DataSet` 必须在 `finally` 中或使用 `try-with-resources` 调用 `close()`，否则会造成内存溢出。
2. **空指针问题**: `AlgoUtils` 在执行计算时，如果字段值为空，应先通过 `nullToZero` 转换。
3. **查询频率**: 严禁在插件的生命周期内高频执行查询，应优先使用已加载到界面模型（Model）中的数据。
4. **SQL 注入**: 使用原生 SQL 时，务必使用参数化查询而非字符串拼接。
