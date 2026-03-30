# 缓存服务 (Cache Service)

## 概述
金蝶云苍穹提供了分布式缓存服务，支持跨微服务节点的数据共享。根据数据的生命周期和隔离级别，主要分为**页面缓存**（随表单关闭而销毁）和**应用缓存**（按应用隔离，支持自定义有效期）。

## 核心类
- **`kd.bos.form.IPageCache`**: 页面级缓存接口，仅限表单插件使用。
- **`kd.bos.entity.cache.AppCache`**: 获取应用级缓存的工具类。
- **`kd.bos.entity.cache.IAppCache`**: 应用级缓存接口。

## 常用 API 方法
### 页面缓存 (this.getPageCache())
- `put(String key, String value)`: 存入单条数据。
- `put(Map<String, String> values)`: 批量存入。
- `get(String key)`: 读取数据。

### 应用缓存 (AppCache.get("appId"))
- `put(String key, Object value)`: 存入数据，默认 1 小时过期。
- `put(String key, Object value, int seconds)`: 存入带自定义过期时间的数据。
- `get(String key, Class<T> clazz)`: 类型安全地读取数据。
- `remove(String key)`: 显式移除缓存。

## 示例代码
```java
// 1. 页面缓存示例（在表单插件中）
public class MyFormPlugin extends AbstractFormPlugin {
    public void cacheTempData() {
        this.getPageCache().put("temp_token", "ABC-123");
        String token = this.getPageCache().get("temp_token");
    }
}

// 2. 应用缓存示例（通用场景）
import kd.bos.entity.cache.AppCache;
import kd.bos.entity.cache.IAppCache;

public class CacheDemo {
    public void handleAppCache() {
        IAppCache cache = AppCache.get("my_app_id");
        // 存入缓存，有效期 10 分钟
        cache.put("user_config", configObj, 600);
        
        // 读取缓存
        MyConfig config = cache.get("user_config", MyConfig.class);
    }
}
```

## 实践建议
1. **优先批量操作**：缓存访问虽然快，但高频的网络交互仍有开销，存入多条数据时优先使用批量接口。
2. **应用编码隔离**：调用 `AppCache.get()` 时，务必传入正确的 `appId`，以实现各应用间的数据隔离。
3. **及时释放**：对于不再需要的应用缓存，应主动调用 `remove`，防止缓存堆积。

## 常见坑位
1. **缓存一致性**：由于是分布式缓存，注意在多节点并发更新同一 Key 时可能产生的数据覆盖问题。
2. **序列化要求**：存入 `AppCache` 的对象必须支持序列化（实现 `Serializable` 接口）。
3. **大小限制**：严禁将超大对象（如数万行的 List）放入缓存，这会显著增加网络传输和 Redis 内存压力。