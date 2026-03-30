# 基础工具类 (DataEntity Utils)

## 概述
苍穹平台在 `kd.bos.dataentity.utils` 包下提供了一系列高性能、Null 安全的工具类，用于处理字符串、日期、集合及 JSON 序列化。在业务开发中，应优先使用这些工具类而非原生 JDK 或第三方库。

## 核心类
- **`kd.bos.dataentity.utils.StringUtils`**: 字符串处理工具。
- **`kd.bos.dataentity.utils.DateUtils`**: 日期计算与格式化工具。
- **`kd.bos.dataentity.utils.CollectionUtils`**: 集合操作工具。
- **`kd.bos.dataentity.serialization.SerializationUtils`**: JSON 序列化工具。

## 常用 API 方法
### 1. 字符串 (StringUtils)
- `isBlank(String str)` / `isNotBlank(String str)`: Null 安全的空白检查。
- `equals(String str1, String str2)`: 避免 NPE 的相等比较。
- `join(Iterable<?> iterable, String separator)`: 字符串拼接。

### 2. 日期 (DateUtils)
- `format(Date date, String pattern)`: 日期格式化。
- `parseDate(String str, String pattern)`: 日期解析。
- `addDays(Date date, int amount)`: 日期加减计算。

### 3. 集合 (CollectionUtils)
- `isEmpty(Collection<?> coll)`: Null 安全的判空。
- `partition(List<T> list, int size)`: 将大列表拆分为固定大小的子列表（分批处理利器）。

### 4. JSON 序列化 (SerializationUtils)
- `toJsonString(Object obj)`: 对象转 JSON。
- `fromJsonString(String json, Class<T> clazz)`: JSON 转对象。

## 示例代码
```java
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.dataentity.utils.CollectionUtils;
import java.util.List;

public class UtilsDemo {
    public void processData(String input, List<Long> ids) {
        // 1. 字符串检查
        if (StringUtils.isBlank(input)) { return; }
        
        // 2. 集合分批处理（每 100 条一批）
        if (CollectionUtils.isNotEmpty(ids)) {
            List<List<Long>> batches = CollectionUtils.partition(ids, 100);
            for (List<Long> batch : batches) {
                doBatchUpdate(batch);
            }
        }
    }
}
```

## 实践建议
1. **Null 安全第一**：凡是涉及外部输入（如界面字段、API 入参）的变量，建议一律通过 `StringUtils.isNotBlank` 或 `CollectionUtils.isEmpty` 进行预检。
2. **格式统一**：日期格式化建议统一使用 `DateUtils.format`，以保证在不同时区环境下的表现一致。
3. **避免重复造轮子**：在手写复杂的字符串截取或集合合并逻辑前，先查阅上述工具类的 API。

## 常见坑位
1. **混用包名**：注意 `StringUtils` 存在于多个包中（如 Apache Commons, Spring），苍穹开发务必认准 `kd.bos.dataentity.utils`。
2. **JSON 性能**：`SerializationUtils` 内部基于 FastJson，对于极高性能要求的场景，注意大对象的序列化开销。
3. **DateUtils 偏移**：日期加减时，注意 `amount` 参数的正负值含义。
