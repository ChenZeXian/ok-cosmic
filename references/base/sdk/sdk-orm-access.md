# ORM 与查询 (QFilter / KSQL)

金蝶云苍穹提供了强大的 ORM 引擎和基于 KSQL 的查询能力。

**注意：** 仅展示核心查询模式与最佳实践。如需查询 `ORM`、`QFilter` 或 `DBServiceHelper` 的完整方法列表、准确参数签名，**请务必使用 `cosmic_get_class_detail(classname="<类名>")` 工具查询。**

## 1. ORM 查询基础 (ORM / QFilter)

### 核心查询模式
```java
import kd.bos.orm.ORM;
import kd.bos.orm.query.QFilter;
import kd.bos.orm.query.QCP;

ORM orm = ORM.create();

// 1. 查询单条记录 (返回 DynamicObject)
DynamicObject obj = orm.queryOne("entity_name", new QFilter[]{filter});

// 2. 查询集合 (返回 DynamicObjectCollection)
DynamicObjectCollection coll = orm.query("entity_name", fields, new QFilter[]{filter});

// 3. 流式查询 (推荐，返回 DataSet)
DataSet ds = orm.queryDataSet("algo_key", "entity_name", fields, new QFilter[]{filter}, "id desc");
```

### QFilter 常用操作符 (QCP)
- **基本比较**: `equals`, `not_equals`, `large_than`, `less_than`, `large_equals`, `less_equals`
- **集合/范围**: `in`, `not_in`, `between`
- **模糊匹配**: `like` (自动补 `%`), `not_like`

### 复杂条件组合
```java
QFilter f1 = new QFilter("status", QCP.equals, "C");
QFilter f2 = new QFilter("amt", QCP.large_than, 100);

// AND 组合
QFilter combined = f1.and(f2);
// OR 组合
QFilter combinedOr = f1.or(new QFilter("type", QCP.equals, "A"));

// 链式调用
QFilter finalFilter = QFilter.of("status = 'C'").and("amt > 1000");
```

## 2. KSQL 与原生 SQL (DBServiceHelper)

用于执行复杂的、非 ORM 能够表达的 SQL 逻辑。

### 核心方法
- **KSQL 查询**: `DBServiceHelper.executeQuery(algoKey, ksql, params)` -> 返回 `DataSet`
- **KSQL 更新**: `DBServiceHelper.executeUpdate(ksql, params)` (慎用，优先用 `SaveServiceHelper`)

### 底层 DB 路由 (DB / DBRoute)
用于跨库访问或需要使用数据库方言 (`/*dialect*/`) 的场景。
```java
import kd.bos.db.DB;
import kd.bos.db.DBRoute;

DB.execute(DBRoute.of("route_key"), "/*dialect*/update t_table set fstatus='C' where fid=?", params);
```

## 3. 开发建议 (Best Practices)

1. **优先使用 QFilter**：避免手写 SQL 字符串拼接，防止 SQL 注入并确保跨数据库兼容性。
2. **字段按需加载**：严禁在 `query` 中使用 `*` 或加载不必要的字段，以节省内存。
3. **关联查询技巧**：
   - 访问基础资料属性：`new QFilter("customer.number", QCP.like, "C%")`
   - 访问单据体字段：`new QFilter("entryentity.material", QCP.equals, mid)`
4. **性能预警**：避免在 `QFilter` 中使用大量的 `OR` 条件或复杂的子查询，这可能导致索引失效。
5. **资源关闭**：通过 `orm.queryDataSet` 或 `DBServiceHelper.executeQuery` 获取的 `DataSet` **必须**及时关闭。

