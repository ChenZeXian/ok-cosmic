# 内存计算框架 (DataSet / Algo)

## 概述
Algo 是苍穹平台的内存计算引擎，遵循 SQL-92 标准，提供类 SQL 的数据处理能力。`DataSet` 是其核心数据结构，采用流式处理（Pipeline），具备严格的内存控制和磁盘交换机制，能有效防止大数据量处理时的 OOM。

## 核心类
- **`kd.bos.algo.DataSet`**: 核心结果集接口，支持转换（Transform）和动作（Action）。
- **`kd.bos.algo.Row`**: 行数据访问器（注意：Row 是虚对象，不可缓存）。
- **`kd.bos.algo.Algo`**: 算法引擎入口。
- **`kd.bos.algo.GroupbyDataSet`**: 分组聚合构造器。
- **`kd.bos.algo.JoinDataSet`**: 数据连接构造器。
- **`kd.bos.algo.HashTable`**: 内存哈希表，用于高性能 HashJoin。

## 常用 API 方法
### 1. 转换 (Transform) - 返回新 DataSet
- `select(String fields)`: 选择或重命名字段。
- `addField(String expression, String alias)`: 增加计算字段。
- `where(String filter)`: SQL 风格过滤。
- `groupBy(String[] fields)`: 分组，后接聚合操作。
- `orderBy(String orderBys)`: 排序。
- `union(DataSet other)`: 合并数据集。
- `copy()`: 复制数据集（支持多次遍历）。

### 2. 动作 (Action) - 消费并关闭 DataSet
- `count()`: 获取总行数。
- `cache(CacheHint hint)`: 缓存到 Redis，支持分页读取。
- `toCollection()`: 转为 `DynamicObjectCollection` (慎用)。

## 示例代码
### 1. 基础转换与聚合
```java
try (DataSet ds = QueryServiceHelper.queryDataSet("key", "entity", "id, qty, price", null, null)) {
    DataSet result = ds.addField("qty * price", "amount")
                       .where("amount > 1000")
                       .groupBy(new String[]{"id"})
                       .sum("amount", "total")
                       .finish();
    while (result.hasNext()) {
        Row row = result.next();
        // 处理结果...
    }
}
```

### 2. 高性能 HashJoin
```java
HashTable detailTable = dsDetail.toHashTable("order_id");
DataSet joined = dsHeader.hashJoin(detailTable, "id", new String[]{"qty", "price"}, true);
```

## 实践建议
1. **优先使用 DataSet**：在处理报表、大批量计算或跨库关联时，应彻底弃用 `DynamicObject`，转为 `DataSet` 编程。
2. **资源释放**：必须使用 `try-with-resources` 或 `AlgoContext` 确保 `DataSet` 及时关闭。
3. **及早过滤**：在 `join` 或 `groupBy` 之前先 `where`，减少中间数据集规模。
4. **大表驱动小表**：在使用普通 `join` 时，尽量将较小的表放在右侧（右侧优先物化并判断规模）。

## 常见坑位
1. **流式单次遍历**：`DataSet` 默认只能遍历一遍，若需二次遍历，必须先调用 `copy()` 或 `cache()`。
2. **跨线程禁止**：`DataSet` 绑定了创建线程的资源，严禁跨线程传递和使用。
3. **字段引用**：在 `join` 后的 `select` 中，若两表有同名列，必须带上表别名，如 `orders.billno`。
4. **Row 缓存**：严禁将 `ds.next()` 返回的 `Row` 对象存入外部 List 中，因为它只是一个指向当前行数据的游标。
